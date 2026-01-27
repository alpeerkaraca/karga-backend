// file: driver-service/src/test/java/com/alpeerkaraca/driverservice/infra/kafka/TripEventConsumerServiceTest.java
package com.alpeerkaraca.driverservice.infra.kafka;

import com.alpeerkaraca.common.event.TripMessage;
import com.alpeerkaraca.common.model.TripEventTypes;
import com.alpeerkaraca.driverservice.model.DriverStatus;
import com.alpeerkaraca.driverservice.repository.DriverInboxRepository;
import com.alpeerkaraca.driverservice.service.DriverStatusService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TripEventConsumerService Tests")
class TripEventConsumerServiceTest {

    private final double TEST_LATITUDE = 1.23;
    private final double TEST_LONGITUDE = 0.56;

    @Mock
    private DriverStatusService driverStatusService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private DriverInboxRepository driverInboxRepository;

    @InjectMocks
    private TripEventConsumerService tripEventConsumerService;

    private JsonNode wrappedPayloadJson(String innerJson) {
        ObjectNode root = new ObjectMapper().createObjectNode();
        root.put("payload", innerJson);
        return root;
    }

    @Test
    @DisplayName("Should set driver status to BUSY when trip is accepted")
    void handleTripEvent_WhenTripAccepted_SetsDriverToBusy() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();

        TripMessage message = new TripMessage(
                TripEventTypes.TRIP_ACCEPTED,
                tripId,
                driverId,
                Instant.now(),
                BigDecimal.ZERO,
                passengerId,
                TEST_LATITUDE,
                TEST_LONGITUDE
        );

        when(driverInboxRepository.existsById(anyString())).thenReturn(false);
        when(objectMapper.readTree(anyString())).thenReturn(wrappedPayloadJson("INNER"));
        when(objectMapper.readValue(eq("INNER"), eq(TripMessage.class))).thenReturn(message);

        tripEventConsumerService.handleTripEvent("OUTER", UUID.randomUUID().toString(), "TRIP_ACCEPTED");

        verify(driverStatusService).updateDriverStatus(
                driverId,
                DriverStatus.BUSY,
                message.getCurrentLongitude(),
                message.getCurrentLatitude()
        );
    }

    @Test
    @DisplayName("Should set driver status to BUSY when trip is started")
    void handleTripEvent_WhenTripStarted_SetsDriverToBusy() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();

        TripMessage message = new TripMessage(
                TripEventTypes.TRIP_STARTED,
                tripId,
                driverId,
                Instant.now(),
                BigDecimal.ZERO,
                passengerId,
                TEST_LATITUDE,
                TEST_LONGITUDE
        );

        when(driverInboxRepository.existsById(anyString())).thenReturn(false);
        when(objectMapper.readTree(anyString())).thenReturn(wrappedPayloadJson("INNER"));
        when(objectMapper.readValue(eq("INNER"), eq(TripMessage.class))).thenReturn(message);

        tripEventConsumerService.handleTripEvent("OUTER", UUID.randomUUID().toString(), "TRIP_STARTED");

        verify(driverStatusService).updateDriverStatus(
                driverId,
                DriverStatus.BUSY,
                message.getCurrentLongitude(),
                message.getCurrentLatitude()
        );
    }

    @Test
    @DisplayName("Should set driver status to ONLINE when trip is completed")
    void handleTripEvent_WhenTripCompleted_SetsDriverToOnline() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();

        TripMessage message = new TripMessage(
                TripEventTypes.TRIP_COMPLETED,
                tripId,
                driverId,
                Instant.now(),
                new BigDecimal("25.50"),
                passengerId,
                TEST_LATITUDE,
                TEST_LONGITUDE
        );

        when(driverInboxRepository.existsById(anyString())).thenReturn(false);
        when(objectMapper.readTree(anyString())).thenReturn(wrappedPayloadJson("INNER"));
        when(objectMapper.readValue(eq("INNER"), eq(TripMessage.class))).thenReturn(message);

        tripEventConsumerService.handleTripEvent("OUTER", UUID.randomUUID().toString(), "TRIP_COMPLETED");

        verify(driverStatusService).updateDriverStatus(
                driverId,
                DriverStatus.ONLINE,
                message.getCurrentLongitude(),
                message.getCurrentLatitude()
        );
    }

    @Test
    @DisplayName("Should set driver status to ONLINE when trip is canceled")
    void handleTripEvent_WhenTripCanceled_SetsDriverToOnline() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();

        TripMessage message = new TripMessage(
                TripEventTypes.TRIP_CANCELLED,
                tripId,
                driverId,
                Instant.now(),
                BigDecimal.ZERO,
                passengerId,
                TEST_LATITUDE,
                TEST_LONGITUDE
        );

        when(driverInboxRepository.existsById(anyString())).thenReturn(false);
        when(objectMapper.readTree(anyString())).thenReturn(wrappedPayloadJson("INNER"));
        when(objectMapper.readValue(eq("INNER"), eq(TripMessage.class))).thenReturn(message);

        tripEventConsumerService.handleTripEvent("OUTER", UUID.randomUUID().toString(), "TRIP_CANCELLED");

        verify(driverStatusService).updateDriverStatus(
                driverId,
                DriverStatus.ONLINE,
                message.getCurrentLongitude(),
                message.getCurrentLatitude()
        );
    }

    @Test
    @DisplayName("Should log warning for unknown event types")
    void handleTripEvent_WhenUnknownEventType_LogsWarning() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();

        TripMessage message = new TripMessage(
                TripEventTypes.TRIP_ACCEPTED,
                tripId,
                driverId,
                Instant.now(),
                BigDecimal.ZERO,
                passengerId,
                TEST_LATITUDE,
                TEST_LONGITUDE
        );

        when(driverInboxRepository.existsById(anyString())).thenReturn(false);
        when(objectMapper.readTree(anyString())).thenReturn(wrappedPayloadJson("INNER"));
        when(objectMapper.readValue(eq("INNER"), eq(TripMessage.class))).thenReturn(message);

        tripEventConsumerService.handleTripEvent("OUTER", UUID.randomUUID().toString(), "UNKNOWN_EVENT");

        verify(driverStatusService, never()).updateDriverStatus(any(), any(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Should handle exceptions gracefully")
    void handleTripEvent_WhenExceptionOccurs_HandlesGracefully() throws Exception {
        UUID driverId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();

        TripMessage message = new TripMessage(
                TripEventTypes.TRIP_ACCEPTED,
                tripId,
                driverId,
                Instant.now(),
                BigDecimal.ZERO,
                passengerId,
                TEST_LATITUDE,
                TEST_LONGITUDE
        );

        when(driverInboxRepository.existsById(anyString())).thenReturn(false);
        when(objectMapper.readTree(anyString())).thenReturn(wrappedPayloadJson("INNER"));
        when(objectMapper.readValue(eq("INNER"), eq(TripMessage.class))).thenReturn(message);

        doThrow(new RuntimeException("Redis connection failed"))
                .when(driverStatusService)
                .updateDriverStatus(
                        driverId,
                        DriverStatus.BUSY,
                        message.getCurrentLongitude(),
                        message.getCurrentLatitude()
                );

        tripEventConsumerService.handleTripEvent("OUTER", UUID.randomUUID().toString(), "TRIP_ACCEPTED");

        verify(driverStatusService).updateDriverStatus(
                driverId,
                DriverStatus.BUSY,
                message.getCurrentLongitude(),
                message.getCurrentLatitude()
        );
    }
}