package com.alpeerkaraca.tripservice.service;

import com.alpeerkaraca.common.exception.ConflictException;
import com.alpeerkaraca.common.exception.ResourceNotFoundException;
import com.alpeerkaraca.tripservice.dto.TripMessage;
import com.alpeerkaraca.tripservice.factory.PricingStrategyFactory;
import com.alpeerkaraca.tripservice.model.PricingType;
import com.alpeerkaraca.tripservice.model.Trip;
import com.alpeerkaraca.tripservice.model.TripStatus;
import com.alpeerkaraca.tripservice.repository.TripRepository;
import com.alpeerkaraca.tripservice.strategy.PricingStrategy;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service managing the entire lifecycle of a trip (Accept, Start, Complete, Cancel).
 * <p>
 * This service handles state transitions, persists them to the database,
 * performs fare calculations based on distance/time, and broadcasts lifecycle events via Kafka.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class TripManagementService {
    private static final String TOPIC_TRIP_EVENTS = "trip_events";
    private static final String TRIP_NOT_FOUND = "Trip not found: ";

    private final TripRepository tripsRepository;
    private final KafkaTemplate<String, TripMessage> kafkaTemplate;

    private final PricingStrategyFactory pricingStrategyFactory;

    /**
     * Retrieves a list of trips that are currently available for drivers to accept.
     *
     * @return List of available {@link Trip} entities.
     */
    public List<Trip> getAvailableTrips() {
        return tripsRepository.findAvailableTrips();
    }

    /**
     * Accepts a requested trip by a driver.
     * <p>
     * Locks the trip record (via {@code findByIdForUpdate}) to prevent concurrent accepts.
     * Updates status to {@link TripStatus#ACCEPTED} and publishes 'TRIP_ACCEPTED' event.
     * </p>
     *
     * @param tripId   UUID of the trip.
     * @param driverId UUID of the driver accepting the trip.
     * @return The updated {@link Trip} entity.
     * @throws ResourceNotFoundException If trip is not found.
     * @throws ConflictException         If trip is not in REQUESTED state.
     */
    @Transactional
    public Trip acceptTrip(UUID tripId, UUID driverId) {
        Trip trip = tripsRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(TRIP_NOT_FOUND + tripId));

        if (trip.getTripStatus() != TripStatus.REQUESTED) {
            throw new ConflictException("Trip has already been accepted or completed.");
        }

        trip.setDriverId(driverId);
        trip.setTripStatus(TripStatus.ACCEPTED);
        trip.setStartedAt(Timestamp.from(Instant.now())); // Usually marks acceptance time here
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

    /**
     * Starts the trip when the driver picks up the passenger.
     * Updates status to {@link TripStatus#IN_PROGRESS}.
     *
     * @param tripId UUID of the trip.
     */
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
                    .currentLongitude(trip.getStartLongitude())
                    .currentLatitude(trip.getStartLatitude())
                    .build();
            kafkaTemplate.send(TOPIC_TRIP_EVENTS, kafkaMessage);
        }
    }

    /**
     * Completes the trip, calculates the final fare, and publishes the completion event.
     *
     * @param tripId UUID of the trip.
     */
    public void completeTrip(UUID tripId) {
        Trip trip = tripsRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException(TRIP_NOT_FOUND + tripId));
        if (trip.getTripStatus() != TripStatus.IN_PROGRESS) {
            throw new ConflictException("Trip can not be completed. Trip status must be started.");
        } else {
            trip.setTripStatus(TripStatus.COMPLETED);
            trip.setEndedAt(Timestamp.from(Instant.now()));

            PricingType pricingType = PricingType.STANDARD; // This should be dynamic based on trip or passenger in the future.
            PricingStrategy pricingStrategy = pricingStrategyFactory.getStrategy(pricingType);
            BigDecimal fare = pricingStrategy.calculate(trip);

            trip.setFare(fare);
            tripsRepository.save(trip);

            TripMessage kafkaMessage = TripMessage.builder()
                    .eventType("TRIP_COMPLETED")
                    .tripId(tripId)
                    .driverId(trip.getDriverId())
                    .passengerId(trip.getPassengerId())
                    .timestamp(Timestamp.valueOf(LocalDateTime.now()))
                    .fare(fare)
                    .currentLongitude(trip.getEndLongitude())
                    .currentLatitude(trip.getEndLatitude())
                    .build();
            kafkaTemplate.send(TOPIC_TRIP_EVENTS, kafkaMessage);
        }
    }

    /**
     * Cancels the trip if it hasn't been completed or canceled yet.
     *
     * @param tripId UUID of the trip.
     */
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
                    .currentLongitude(trip.getStartLongitude())
                    .currentLatitude(trip.getStartLatitude())
                    .build();
            kafkaTemplate.send(TOPIC_TRIP_EVENTS, kafkaMessage);
        }
    }


}