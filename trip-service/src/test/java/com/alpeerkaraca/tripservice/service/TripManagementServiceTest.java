package com.alpeerkaraca.tripservice.service;

import com.alpeerkaraca.common.exception.ConflictException;
import com.alpeerkaraca.common.exception.ResourceNotFoundException;
import com.alpeerkaraca.tripservice.model.Trip;
import com.alpeerkaraca.tripservice.model.TripStatus;
import com.alpeerkaraca.tripservice.repository.TripRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripManagementServiceTest {

    @InjectMocks
    private TripManagementService tripManagementService;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void acceptTrip_ValidTrip_ShouldAcceptAndPublishEvent() {
        UUID tripId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        Trip trip = new Trip();
        trip.setTripStatus(TripStatus.REQUESTED);
        when(tripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenReturn(trip);

        tripManagementService.acceptTrip(tripId, driverId);

        verify(tripRepository).save(any(Trip.class));
        verify(kafkaTemplate).send(anyString(), any());
    }

    @Test
    void acceptTrip_TripNotFound_ShouldThrowResourceNotFoundException() {
        UUID tripId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        when(tripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tripManagementService.acceptTrip(tripId, driverId));
    }

    @Test
    void acceptTrip_TripNotRequested_ShouldThrowConflictException() {
        UUID tripId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        Trip trip = new Trip();
        trip.setTripStatus(TripStatus.ACCEPTED);
        when(tripRepository.findByIdForUpdate(tripId)).thenReturn(Optional.of(trip));

        assertThrows(ConflictException.class, () -> tripManagementService.acceptTrip(tripId, driverId));
    }
}

