package com.alpeerkaraca.tripservice.service;

import com.alpeerkaraca.common.event.TripMessage;
import com.alpeerkaraca.common.exception.ConflictException;
import com.alpeerkaraca.common.exception.ResourceNotFoundException;
import com.alpeerkaraca.common.exception.SerializationException;
import com.alpeerkaraca.common.model.TripEventTypes;
import com.alpeerkaraca.tripservice.factory.PricingStrategyFactory;
import com.alpeerkaraca.tripservice.model.PricingType;
import com.alpeerkaraca.tripservice.model.Trip;
import com.alpeerkaraca.tripservice.model.TripOutbox;
import com.alpeerkaraca.tripservice.model.TripStatus;
import com.alpeerkaraca.tripservice.repository.TripOutboxRepository;
import com.alpeerkaraca.tripservice.repository.TripRepository;
import com.alpeerkaraca.tripservice.strategy.PricingStrategy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
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
@Slf4j
public class TripManagementService {
    private static final String TRIP_NOT_FOUND = "Trip not found: ";

    private final TripRepository tripsRepository;
    private final TripOutboxRepository tripOutboxRepository;
    private final ObjectMapper objectMapper;
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
        trip.setStartedAt(Instant.now());
        Trip savedTrip = tripsRepository.save(trip);

        readyEvent(tripId, TripEventTypes.TRIP_ACCEPTED, savedTrip, BigDecimal.ZERO);
        return savedTrip;
    }

    /**
     * Starts the trip when the driver picks up the passenger.
     * Updates status to {@link TripStatus#IN_PROGRESS}.
     *
     * @param tripId UUID of the trip.
     */
    @Transactional
    public void startTrip(UUID tripId) {
        Trip trip = tripsRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException(TRIP_NOT_FOUND + tripId));
        if (trip.getTripStatus() != TripStatus.ACCEPTED) {
            throw new ConflictException("Trip can not be started. Trip status must be ACCEPTED.");
        } else {
            trip.setTripStatus(TripStatus.IN_PROGRESS);
            trip.setStartedAt(Instant.now());
            tripsRepository.save(trip);

            readyEvent(tripId, TripEventTypes.TRIP_STARTED, trip, BigDecimal.ZERO);
        }
    }

    /**
     * Completes the trip, calculates the final fare, and publishes the completion event.
     *
     * @param tripId UUID of the trip.
     */
    @Transactional
    public void completeTrip(UUID tripId) {
        Trip trip = tripsRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException(TRIP_NOT_FOUND + tripId));
        if (trip.getTripStatus() != TripStatus.IN_PROGRESS) {
            throw new ConflictException("Trip can not be completed. Trip status must be started.");
        } else {
            trip.setTripStatus(TripStatus.COMPLETED);
            trip.setEndedAt(Instant.now());

            PricingType pricingType = PricingType.STANDARD; // This should be dynamic based on trip or passenger in the future.
            PricingStrategy pricingStrategy = pricingStrategyFactory.getStrategy(pricingType);
            BigDecimal fare = pricingStrategy.calculate(trip);

            trip.setFare(fare);
            tripsRepository.save(trip);
            readyEvent(tripId, TripEventTypes.TRIP_COMPLETED, trip, fare);
        }
    }

    /**
     * Cancels the trip if it hasn't been completed or canceled yet.
     *
     * @param tripId UUID of the trip.
     */
    @Transactional
    public void cancelTrip(UUID tripId) {
        Trip trip = tripsRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException(TRIP_NOT_FOUND + tripId));
        if (trip.getTripStatus() == TripStatus.COMPLETED) {
            throw new ConflictException("Trip has already been completed.");
        } else if (trip.getTripStatus() == TripStatus.CANCELLED) {
            throw new ConflictException("Trip has already been cancelled.");
        } else {
            trip.setTripStatus(TripStatus.CANCELLED);
            trip.setEndedAt(Instant.now());
            tripsRepository.save(trip);
            readyEvent(tripId, TripEventTypes.TRIP_CANCELLED, trip, BigDecimal.ZERO);
        }
    }

    /**
     * Helper method to prepare and save a trip event to the outbox.
     *
     * @param tripId    ID of the trip
     * @param eventType Type of the event
     * @param trip      Trip entity
     * @param fare      Calculated fare
     */
    private void readyEvent(UUID tripId, TripEventTypes eventType, @NonNull Trip trip, BigDecimal fare) {
        TripMessage kafkaMessage = TripMessage.builder()
                .eventType(eventType)
                .tripId(tripId)
                .driverId(trip.getDriverId())
                .passengerId(trip.getPassengerId())
                .createdAt(Instant.now())
                .fare(fare)
                .currentLongitude(trip.getTripStatus() == TripStatus.COMPLETED ? trip.getEndLongitude() : trip.getStartLongitude())
                .currentLatitude(trip.getTripStatus() == TripStatus.COMPLETED ? trip.getEndLatitude() : trip.getStartLatitude())
                .build();
        TripOutbox outbox = new TripOutbox();
        outbox.setAggregateType("TRIP");
        outbox.setAggregateId(tripId.toString());
        outbox.setEventType(eventType.toString());

        try {
            outbox.setPayload(objectMapper.writeValueAsString(kafkaMessage));
        } catch (JsonProcessingException e) {
            log.error("Event serialization error for TripID: {}", tripId, e);
            throw new SerializationException("Error while serializing data");
        }
        tripOutboxRepository.save(outbox);
    }
}