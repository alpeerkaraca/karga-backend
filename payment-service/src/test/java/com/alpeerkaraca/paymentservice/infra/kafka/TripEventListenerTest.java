package com.alpeerkaraca.paymentservice.infra.kafka;

import com.alpeerkaraca.common.event.TripMessage;
import com.alpeerkaraca.common.model.TripEventTypes;
import com.alpeerkaraca.paymentservice.model.Payment;
import com.alpeerkaraca.paymentservice.repository.PaymentInboxRepository;
import com.alpeerkaraca.paymentservice.service.StripePaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TripEventListener Tests")
class TripEventListenerTest {

    @Mock
    private StripePaymentService paymentService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PaymentInboxRepository paymentInboxRepository;

    @InjectMocks
    private TripEventListener tripEventListener;

    private JsonNode wrappedPayloadJson(String innerJson) {
        ObjectNode root = new ObjectMapper().createObjectNode();
        root.put("payload", innerJson);
        return root;
    }

    @Test
    @DisplayName("Should create payment session when trip is completed")
    void handleTripEvent_WhenTripCompleted_CreatesPaymentSession() throws JsonProcessingException {
        // Arrange
        UUID tripId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        BigDecimal fare = new BigDecimal("45.75");

        TripMessage message = TripMessage.builder()
                .eventType(TripEventTypes.TRIP_COMPLETED)
                .tripId(tripId)
                .passengerId(passengerId)
                .driverId(driverId)
                .fare(fare)
                .createdAt(Instant.now())
                .build();

        Payment mockPayment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .tripId(tripId)
                .passengerId(passengerId)
                .paymentAmount(fare)
                .build();

        when(paymentService.createPaymentSession(tripId, passengerId, fare)).thenReturn(mockPayment);
        when(paymentInboxRepository.existsById(anyString())).thenReturn(false);
        when(objectMapper.readTree(anyString())).thenReturn(wrappedPayloadJson("INNER"));
        when(objectMapper.readValue(eq("INNER"), eq(TripMessage.class))).thenReturn(message);
        // Act
        tripEventListener.handleTripEvent("OUTER", "TRIP_COMPLETED", UUID.randomUUID().toString());

        // Assert
        verify(paymentService).createPaymentSession(tripId, passengerId, fare);
    }

    @Test
    @DisplayName("Should not create payment when trip is accepted")
    void handleTripEvent_WhenTripAccepted_DoesNotCreatePayment() throws JsonProcessingException {
        // Arrange
        TripMessage message = TripMessage.builder()
                .eventType(TripEventTypes.TRIP_ACCEPTED)
                .tripId(UUID.randomUUID())
                .passengerId(UUID.randomUUID())
                .driverId(UUID.randomUUID())
                .fare(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .build();

        when(paymentInboxRepository.existsById(anyString())).thenReturn(false);
        when(objectMapper.readTree(anyString())).thenReturn(wrappedPayloadJson("INNER"));
        when(objectMapper.readValue(eq("INNER"), eq(TripMessage.class))).thenReturn(message);

        // Act
        tripEventListener.handleTripEvent("OUTER", "TRIP_ACCEPTED", UUID.randomUUID().toString());

        // Assert
        verify(paymentService, never()).createPaymentSession(any(), any(), any());
    }

    @Test
    @DisplayName("Should not create payment when trip is started")
    void handleTripEvent_WhenTripStarted_DoesNotCreatePayment() throws JsonProcessingException {
        // Arrange
        TripMessage message = TripMessage.builder()
                .eventType(TripEventTypes.TRIP_STARTED)
                .tripId(UUID.randomUUID())
                .passengerId(UUID.randomUUID())
                .driverId(UUID.randomUUID())
                .fare(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .build();

        when(paymentInboxRepository.existsById(anyString())).thenReturn(false);
        when(objectMapper.readTree(anyString())).thenReturn(wrappedPayloadJson("INNER"));
        when(objectMapper.readValue(eq("INNER"), eq(TripMessage.class))).thenReturn(message);

        // Act
        tripEventListener.handleTripEvent("OUTER", "TRIP_STARTED", UUID.randomUUID().toString());


        // Assert
        verify(paymentService, never()).createPaymentSession(any(), any(), any());
    }

    @Test
    @DisplayName("Should not create payment when trip is cancelled")
    void handleTripEvent_WhenTripCancelled_DoesNotCreatePayment() throws JsonProcessingException {
        // Arrange
        TripMessage message = TripMessage.builder()
                .eventType(TripEventTypes.TRIP_CANCELLED)
                .tripId(UUID.randomUUID())
                .passengerId(UUID.randomUUID())
                .driverId(UUID.randomUUID())
                .fare(BigDecimal.ZERO)
                .createdAt(Instant.now())

                .build();
        when(paymentInboxRepository.existsById(anyString())).thenReturn(false);
        when(objectMapper.readTree(anyString())).thenReturn(wrappedPayloadJson("INNER"));
        when(objectMapper.readValue(eq("INNER"), eq(TripMessage.class))).thenReturn(message);

        // Act
        tripEventListener.handleTripEvent("OUTER", "TRIP_CANCELLED", UUID.randomUUID().toString());


        // Assert
        verify(paymentService, never()).createPaymentSession(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle multiple completed trip events")
    void handleTripEvent_MultipleCompletedTrips_CreatesMultiplePayments() throws JsonProcessingException {
        // Arrange
        Payment mockPayment1 = Payment.builder().paymentId(UUID.randomUUID()).build();
        Payment mockPayment2 = Payment.builder().paymentId(UUID.randomUUID()).build();

        when(paymentService.createPaymentSession(any(), any(), any()))
                .thenReturn(mockPayment1)
                .thenReturn(mockPayment2);

        TripMessage message1 = TripMessage.builder()
                .eventType(TripEventTypes.TRIP_COMPLETED)
                .tripId(UUID.randomUUID())
                .passengerId(UUID.randomUUID())
                .driverId(UUID.randomUUID())
                .fare(new BigDecimal("30.00"))
                .createdAt(Instant.now())

                .build();

        TripMessage message2 = TripMessage.builder()
                .eventType(TripEventTypes.TRIP_COMPLETED)
                .tripId(UUID.randomUUID())
                .passengerId(UUID.randomUUID())
                .driverId(UUID.randomUUID())
                .fare(new BigDecimal("50.00"))
                .createdAt(Instant.now())

                .build();
        when(paymentInboxRepository.existsById(anyString())).thenReturn(false);
        when(objectMapper.readTree(anyString())).thenReturn(wrappedPayloadJson("INNER_1"),
                wrappedPayloadJson("INNER_2"));
        when(objectMapper.readValue(eq("INNER_1"), eq(TripMessage.class))).thenReturn(message1);
        when(objectMapper.readValue(eq("INNER_2"), eq(TripMessage.class))).thenReturn(message2);

        // Act

        // Act
        tripEventListener.handleTripEvent("OUTER_1", "TRIP_COMPLETED", UUID.randomUUID().toString());
        tripEventListener.handleTripEvent("OUTER_2", "TRIP_COMPLETED", UUID.randomUUID().toString());

        // Assert
        verify(paymentService, times(2)).createPaymentSession(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle high fare amounts correctly")
    void handleTripEvent_WithHighFare_CreatesPaymentCorrectly() throws JsonProcessingException {
        // Arrange
        UUID tripId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();
        BigDecimal highFare = new BigDecimal("999.99");

        TripMessage message = TripMessage.builder()
                .eventType(TripEventTypes.TRIP_COMPLETED)
                .tripId(tripId)
                .passengerId(passengerId)
                .driverId(UUID.randomUUID())
                .fare(highFare)
                .createdAt(Instant.now())
                .build();

        Payment mockPayment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .tripId(tripId)
                .passengerId(passengerId)
                .paymentAmount(highFare)
                .build();

        when(paymentService.createPaymentSession(tripId, passengerId, highFare)).thenReturn(mockPayment);
        when(paymentInboxRepository.existsById(anyString())).thenReturn(false);
        when(objectMapper.readTree(anyString())).thenReturn(wrappedPayloadJson("INNER"));
        when(objectMapper.readValue("INNER", TripMessage.class)).thenReturn(message);

        // Act
        tripEventListener.handleTripEvent("OUTER", "TRIP_COMPLETED", UUID.randomUUID().toString());

        // Assert
        verify(paymentService).createPaymentSession(tripId, passengerId, highFare);
    }
}

