package com.alpeerkaraca.paymentservice.infra.kafka;

import com.alpeerkaraca.paymentservice.dto.TripMessage;
import com.alpeerkaraca.paymentservice.model.Payment;
import com.alpeerkaraca.paymentservice.service.StripePaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TripEventListener Tests")
class TripEventListenerTest {

    @Mock
    private KafkaTemplate<String, com.alpeerkaraca.paymentservice.dto.PaymentMessage> kafkaTemplate;

    @Mock
    private StripePaymentService paymentService;

    @InjectMocks
    private TripEventListener tripEventListener;

    @Test
    @DisplayName("Should create payment session when trip is completed")
    void handleTripEvent_WhenTripCompleted_CreatesPaymentSession() {
        // Arrange
        UUID tripId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        BigDecimal fare = new BigDecimal("45.75");

        TripMessage message = TripMessage.builder()
                .eventType("TRIP_COMPLETED")
                .tripId(tripId)
                .passengerId(passengerId)
                .driverId(driverId)
                .fare(fare)
                .timestamp(Timestamp.valueOf(LocalDateTime.now()))
                .build();

        Payment mockPayment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .tripId(tripId)
                .passengerId(passengerId)
                .paymentAmount(fare)
                .build();

        when(paymentService.createPaymentSession(tripId, passengerId, fare)).thenReturn(mockPayment);

        // Act
        tripEventListener.handleTripEvent(message);

        // Assert
        verify(paymentService).createPaymentSession(tripId, passengerId, fare);
    }

    @Test
    @DisplayName("Should not create payment when trip is accepted")
    void handleTripEvent_WhenTripAccepted_DoesNotCreatePayment() {
        // Arrange
        TripMessage message = TripMessage.builder()
                .eventType("TRIP_ACCEPTED")
                .tripId(UUID.randomUUID())
                .passengerId(UUID.randomUUID())
                .driverId(UUID.randomUUID())
                .fare(BigDecimal.ZERO)
                .timestamp(Timestamp.valueOf(LocalDateTime.now()))
                .build();

        // Act
        tripEventListener.handleTripEvent(message);

        // Assert
        verify(paymentService, never()).createPaymentSession(any(), any(), any());
    }

    @Test
    @DisplayName("Should not create payment when trip is started")
    void handleTripEvent_WhenTripStarted_DoesNotCreatePayment() {
        // Arrange
        TripMessage message = TripMessage.builder()
                .eventType("TRIP_STARTED")
                .tripId(UUID.randomUUID())
                .passengerId(UUID.randomUUID())
                .driverId(UUID.randomUUID())
                .fare(BigDecimal.ZERO)
                .timestamp(Timestamp.valueOf(LocalDateTime.now()))
                .build();

        // Act
        tripEventListener.handleTripEvent(message);

        // Assert
        verify(paymentService, never()).createPaymentSession(any(), any(), any());
    }

    @Test
    @DisplayName("Should not create payment when trip is cancelled")
    void handleTripEvent_WhenTripCancelled_DoesNotCreatePayment() {
        // Arrange
        TripMessage message = TripMessage.builder()
                .eventType("TRIP_CANCELLED")
                .tripId(UUID.randomUUID())
                .passengerId(UUID.randomUUID())
                .driverId(UUID.randomUUID())
                .fare(BigDecimal.ZERO)
                .timestamp(Timestamp.valueOf(LocalDateTime.now()))
                .build();

        // Act
        tripEventListener.handleTripEvent(message);

        // Assert
        verify(paymentService, never()).createPaymentSession(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle multiple completed trip events")
    void handleTripEvent_MultipleCompletedTrips_CreatesMultiplePayments() {
        // Arrange
        Payment mockPayment1 = Payment.builder().paymentId(UUID.randomUUID()).build();
        Payment mockPayment2 = Payment.builder().paymentId(UUID.randomUUID()).build();

        when(paymentService.createPaymentSession(any(), any(), any()))
                .thenReturn(mockPayment1)
                .thenReturn(mockPayment2);

        TripMessage message1 = TripMessage.builder()
                .eventType("TRIP_COMPLETED")
                .tripId(UUID.randomUUID())
                .passengerId(UUID.randomUUID())
                .driverId(UUID.randomUUID())
                .fare(new BigDecimal("30.00"))
                .timestamp(Timestamp.valueOf(LocalDateTime.now()))
                .build();

        TripMessage message2 = TripMessage.builder()
                .eventType("TRIP_COMPLETED")
                .tripId(UUID.randomUUID())
                .passengerId(UUID.randomUUID())
                .driverId(UUID.randomUUID())
                .fare(new BigDecimal("50.00"))
                .timestamp(Timestamp.valueOf(LocalDateTime.now()))
                .build();

        // Act
        tripEventListener.handleTripEvent(message1);
        tripEventListener.handleTripEvent(message2);

        // Assert
        verify(paymentService, times(2)).createPaymentSession(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle high fare amounts correctly")
    void handleTripEvent_WithHighFare_CreatesPaymentCorrectly() {
        // Arrange
        UUID tripId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();
        BigDecimal highFare = new BigDecimal("999.99");

        TripMessage message = TripMessage.builder()
                .eventType("TRIP_COMPLETED")
                .tripId(tripId)
                .passengerId(passengerId)
                .driverId(UUID.randomUUID())
                .fare(highFare)
                .timestamp(Timestamp.valueOf(LocalDateTime.now()))
                .build();

        Payment mockPayment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .tripId(tripId)
                .passengerId(passengerId)
                .paymentAmount(highFare)
                .build();

        when(paymentService.createPaymentSession(tripId, passengerId, highFare)).thenReturn(mockPayment);

        // Act
        tripEventListener.handleTripEvent(message);

        // Assert
        verify(paymentService).createPaymentSession(tripId, passengerId, highFare);
    }
}

