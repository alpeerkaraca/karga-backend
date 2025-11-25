package com.alpeerkaraca.tripservice.service;

import com.alpeerkaraca.common.exception.ConflictException;
import com.alpeerkaraca.common.exception.ResourceNotFoundException;
import com.alpeerkaraca.tripservice.dto.TripMessage;
import com.alpeerkaraca.tripservice.model.Trip;
import com.alpeerkaraca.tripservice.model.TripStatus;
import com.alpeerkaraca.tripservice.repository.TripRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.awt.geom.Point2D;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TripManagementService {
    private static final String TOPIC_TRIP_EVENTS = "trip_events";
    // TAXI FARE PRICES
    private static final double DISTANCE_FEE_PER_KM = 36.30;
    private static final double TIME_FEE_PER_MIN = 7.56;
    private static final double OPENING_FEE = 54.50;
    private static final BigDecimal MINIMUM_FEE = BigDecimal.valueOf(175);
    // Constants
    private static final String TRIP_NOT_FOUND = "Trip not found: ";
    private static final double EARTH_RADIUS_KM = 6371.0;
    private final TripRepository tripsRepository;
    private final KafkaTemplate<String, TripMessage> kafkaTemplate;

    public List<Trip> getAvailableTrips() {
        return tripsRepository.findAvailableTrips();
    }

    @Transactional
    public Trip acceptTrip(UUID tripId, UUID driverId) {
        Trip trip = tripsRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(TRIP_NOT_FOUND + tripId));

        if (trip.getTripStatus() != TripStatus.REQUESTED) {
            throw new ConflictException("Trip has already been accepted or completed.");
        }

        trip.setDriverId(driverId);
        trip.setTripStatus(TripStatus.ACCEPTED);
        trip.setStartedAt(Timestamp.from(Instant.now()));
        Trip savedTrip = tripsRepository.save(trip);

        TripMessage kafkaMessage = TripMessage.builder()
                .eventType("TRIP_ACCEPTED")
                .tripId(tripId)
                .driverId(driverId)
                .passengerId(savedTrip.getPassengerId())
                .timestamp(Timestamp.valueOf(LocalDateTime.now()))
                .fare(new BigDecimal(0))
                .build();
        kafkaTemplate.send(TOPIC_TRIP_EVENTS, kafkaMessage);

        return savedTrip;
    }

    public void startTrip(UUID tripId) {
        Trip trip = tripsRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException(TRIP_NOT_FOUND + tripId));
        if (trip.getTripStatus() != TripStatus.ACCEPTED) {
            throw new ConflictException("Trip can not be started. Trip status must be ACCEPTED.");
        } else {
            trip.setTripStatus(TripStatus.IN_PROGRESS);
            trip.setStartedAt(Timestamp.from(Instant.now()));
            tripsRepository.save(trip);
            TripMessage kafkaMessage = TripMessage.builder()
                    .eventType("TRIP_STARTED")
                    .tripId(tripId)
                    .driverId(trip.getDriverId())
                    .passengerId(trip.getPassengerId())
                    .timestamp(Timestamp.valueOf(LocalDateTime.now()))
                    .fare(new BigDecimal(0))
                    .build();
            kafkaTemplate.send(TOPIC_TRIP_EVENTS, kafkaMessage);
        }
    }

    public void completeTrip(UUID tripId) {
        Trip trip = tripsRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException(TRIP_NOT_FOUND + tripId));
        if (trip.getTripStatus() != TripStatus.IN_PROGRESS) {
            throw new ConflictException("Trip can not be completed. Trip status must be started.");
        } else {
            trip.setTripStatus(TripStatus.COMPLETED);
            trip.setEndedAt(Timestamp.from(Instant.now()));
            BigDecimal fare = calculateFare(trip);

            trip.setFare(fare);
            tripsRepository.save(trip);
            TripMessage kafkaMessage = TripMessage.builder()
                    .eventType("TRIP_COMPLETED")
                    .tripId(tripId)
                    .driverId(trip.getDriverId())
                    .passengerId(trip.getPassengerId())
                    .timestamp(Timestamp.valueOf(LocalDateTime.now()))
                    .fare(fare)
                    .build();
            kafkaTemplate.send(TOPIC_TRIP_EVENTS, kafkaMessage);
        }
    }

    public void cancelTrip(UUID tripId) {
        Trip trip = tripsRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException(TRIP_NOT_FOUND + tripId));
        if (trip.getTripStatus() == TripStatus.COMPLETED) {
            throw new ConflictException("Trip has already been completed.");
        } else if (trip.getTripStatus() == TripStatus.CANCELLED) {
            throw new ConflictException("Trip has already been cancelled.");
        } else {
            trip.setTripStatus(TripStatus.CANCELLED);
            trip.setEndedAt(Timestamp.from(Instant.now()));
            tripsRepository.save(trip);
            TripMessage kafkaMessage = TripMessage.builder()
                    .eventType("TRIP_CANCELLED")
                    .tripId(tripId)
                    .driverId(trip.getDriverId())
                    .passengerId(trip.getPassengerId())
                    .timestamp(Timestamp.valueOf(LocalDateTime.now()))
                    .fare(new BigDecimal(0))
                    .build();
            kafkaTemplate.send(TOPIC_TRIP_EVENTS, kafkaMessage);
        }
    }

    private BigDecimal calculateFare(Trip trip) {
        if (trip.getTripStatus() != TripStatus.COMPLETED) {
            return BigDecimal.ZERO;
        }
        long timeDifference = TimeUnit.MILLISECONDS.toMinutes(trip.getEndedAt().getTime() - trip.getStartedAt().getTime());
        long elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(timeDifference);
        double distanceInKm = calculateHaversineDistance(
                trip.getStartLatitude(),
                trip.getStartLongitude(),
                trip.getEndLatitude(),
                trip.getEndLongitude()
        );
        BigDecimal fare = BigDecimal.valueOf(OPENING_FEE)
                .add(BigDecimal.valueOf(DISTANCE_FEE_PER_KM * distanceInKm))
                .add(BigDecimal.valueOf(TIME_FEE_PER_MIN * elapsedMinutes));
        if (fare.compareTo(MINIMUM_FEE) < 0) {
            return MINIMUM_FEE;
        }
        return fare;
    }

    private double calculateHaversineDistance(double startLat, double startLong, double endLat, double endLong) {
        double dLat = Math.toRadians(endLat - startLat);
        double dLon = Math.toRadians(endLong - startLong);

        double startLatitudeRad = Math.toRadians(startLat);
        double endLatitudeRad = Math.toRadians(endLat);

        double squareOfHalfChordLength =
                Math.pow(Math.sin(dLat / 2), 2) +
                        Math.pow(Math.sin(dLon / 2), 2) *
                                Math.cos(startLatitudeRad) * Math.cos(endLatitudeRad);

        double angularDistanceInRadians = 2 * Math.asin(Math.sqrt(squareOfHalfChordLength));

        return EARTH_RADIUS_KM * angularDistanceInRadians;

    }
}
