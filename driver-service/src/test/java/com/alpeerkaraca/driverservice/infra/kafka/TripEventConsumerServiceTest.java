package com.alpeerkaraca.driverservice.infra.kafka;

import com.alpeerkaraca.driverservice.dto.TripMessage;
import com.alpeerkaraca.driverservice.model.DriverStatus;
import com.alpeerkaraca.driverservice.service.DriverStatusService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TripEventConsumerService Tests")
class TripEventConsumerServiceTest {

    //
    private final double TEST_LATITUDE = 1.23;
    private final double TEST_LONGITUDE = 0.56;
    @Mock
    private DriverStatusService driverStatusService;
    @InjectMocks
    private TripEventConsumerService tripEventConsumerService;

    @Test
    @DisplayName("Should set driver status to BUSY when trip is accepted")
    void handleTripEvent_WhenTripAccepted_SetsDriverToBusy() {
        // Arrange
        UUID driverId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();

        TripMessage message = new TripMessage(
                "TRIP_ACCEPTED",
                tripId,
                driverId,
                Timestamp.valueOf(LocalDateTime.now()),
                BigDecimal.ZERO,
                passengerId,
                TEST_LATITUDE,
                TEST_LONGITUDE
        );

        // Stubbing
        doNothing().when(driverStatusService).updateDriverStatus(
                any(UUID.class),
                eq(DriverStatus.BUSY),
                eq(TEST_LATITUDE),
                eq(TEST_LONGITUDE)
        );

        // Act
        tripEventConsumerService.handleTripEvent(message);

        // Assert
        verify(driverStatusService).updateDriverStatus(
                driverId,
                DriverStatus.BUSY,
                TEST_LATITUDE,
                TEST_LONGITUDE
        );
    }

    @Test
    @DisplayName("Should set driver status to BUSY when trip is started")
    void handleTripEvent_WhenTripStarted_SetsDriverToBusy() {
        // Arrange
        UUID driverId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();

        TripMessage message = new TripMessage(
                "TRIP_STARTED",
                tripId,
                driverId,
                Timestamp.valueOf(LocalDateTime.now()),
                BigDecimal.ZERO,
                passengerId,
                TEST_LATITUDE,
                TEST_LONGITUDE
        );

        doNothing().when(driverStatusService).updateDriverStatus(
                any(UUID.class),
                eq(DriverStatus.BUSY),
                eq(TEST_LATITUDE),
                eq(TEST_LONGITUDE)
        );

        // Act
        tripEventConsumerService.handleTripEvent(message);

        // Assert
        verify(driverStatusService).updateDriverStatus(
                driverId,
                DriverStatus.BUSY,
                TEST_LATITUDE,
                TEST_LONGITUDE
        );
    }

    @Test
    @DisplayName("Should set driver status to ONLINE when trip is completed")
    void handleTripEvent_WhenTripCompleted_SetsDriverToOnline() {
        // Arrange
        UUID driverId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();

        TripMessage message = new TripMessage(
                "TRIP_COMPLETED",
                tripId,
                driverId,
                Timestamp.valueOf(LocalDateTime.now()),
                new BigDecimal("25.50"),
                passengerId,
                TEST_LATITUDE,
                TEST_LONGITUDE
        );

        doNothing().when(driverStatusService).updateDriverStatus(
                any(UUID.class),
                eq(DriverStatus.ONLINE),
                eq(TEST_LATITUDE),
                eq(TEST_LONGITUDE)
        );

        // Act
        tripEventConsumerService.handleTripEvent(message);

        // Assert
        verify(driverStatusService).updateDriverStatus(
                driverId,
                DriverStatus.ONLINE,
                TEST_LATITUDE,
                TEST_LONGITUDE
        );
    }

    @Test
    @DisplayName("Should set driver status to ONLINE when trip is canceled")
    void handleTripEvent_WhenTripCanceled_SetsDriverToOnline() {
        // Arrange
        UUID driverId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();

        TripMessage message = new TripMessage(
                "TRIP_CANCELED",
                tripId,
                driverId,
                Timestamp.valueOf(LocalDateTime.now()),
                BigDecimal.ZERO,
                passengerId,
                TEST_LATITUDE,
                TEST_LONGITUDE
        );

        doNothing().when(driverStatusService).updateDriverStatus(
                any(UUID.class),
                eq(DriverStatus.ONLINE),
                eq(TEST_LATITUDE),
                eq(TEST_LONGITUDE)
        );

        // Act
        tripEventConsumerService.handleTripEvent(message);

        // Assert
        verify(driverStatusService).updateDriverStatus(
                driverId,
                DriverStatus.ONLINE,
                TEST_LATITUDE,
                TEST_LONGITUDE
        );
    }

    @Test
    @DisplayName("Should log warning for unknown event types")
    void handleTripEvent_WhenUnknownEventType_LogsWarning() {
        // Arrange
        UUID driverId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();

        TripMessage message = new TripMessage(
                "UNKNOWN_EVENT",
                tripId,
                driverId,
                Timestamp.valueOf(LocalDateTime.now()),
                BigDecimal.ZERO,
                passengerId,
                TEST_LATITUDE,
                TEST_LONGITUDE
        );

        // Act
        tripEventConsumerService.handleTripEvent(message);

        // Assert
        verify(driverStatusService, never()).updateDriverStatus(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should handle exceptions gracefully")
    void handleTripEvent_WhenExceptionOccurs_HandlesGracefully() {
        // Arrange
        UUID driverId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();

        TripMessage message = new TripMessage(
                "TRIP_ACCEPTED",
                tripId,
                driverId,
                Timestamp.valueOf(LocalDateTime.now()),
                BigDecimal.ZERO,
                passengerId,
                TEST_LATITUDE,
                TEST_LONGITUDE
        );

        doThrow(new RuntimeException("Redis connection failed"))
                .when(driverStatusService).updateDriverStatus(
                        any(UUID.class),
                        eq(DriverStatus.BUSY),
                        eq(TEST_LATITUDE),
                        eq(TEST_LONGITUDE)
                );

        // Act
        tripEventConsumerService.handleTripEvent(message);

        // Assert
        verify(driverStatusService).updateDriverStatus(
                driverId,
                DriverStatus.BUSY,
                TEST_LATITUDE,
                TEST_LONGITUDE
        );
    }
}