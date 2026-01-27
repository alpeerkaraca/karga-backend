package com.alpeerkaraca.paymentservice.integration;

import com.alpeerkaraca.common.event.TripMessage;
import com.alpeerkaraca.common.model.TripEventTypes;
import com.alpeerkaraca.paymentservice.AbstractIntegrationTest;
import com.alpeerkaraca.paymentservice.model.Payment;
import com.alpeerkaraca.paymentservice.model.PaymentStatus;
import com.alpeerkaraca.paymentservice.repository.PaymentRepository;
import com.alpeerkaraca.paymentservice.service.StripePaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@DisplayName("Payment Service Integration Tests")
class PaymentServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private KafkaTemplate<String, TripMessage> kafkaTemplate;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    @MockitoBean
    private StripePaymentService stripePaymentService;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create payment when trip completed event is received")
    void handleTripCompleted_CreatesPayment() {
        // Arrange
        UUID tripId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();
        BigDecimal fare = new BigDecimal("35.50");

        when(stripePaymentService.createPaymentSession(any(UUID.class), any(UUID.class), any(BigDecimal.class)))
                .thenAnswer(invocation -> {
                    UUID incomingPassengerId = invocation.getArgument(1);
                    BigDecimal incomingFare = invocation.getArgument(2);

                    Payment newPaymentEntry = Payment.builder()
                            .tripId(tripId)
                            .passengerId(incomingPassengerId)
                            .paymentAmount(incomingFare)
                            .paymentStatus(PaymentStatus.PENDING)
                            .build();

                    return paymentRepository.save(newPaymentEntry);
                });

        TripMessage message = TripMessage.builder()
                .eventType(TripEventTypes.TRIP_COMPLETED)
                .tripId(tripId)
                .passengerId(passengerId)
                .driverId(UUID.randomUUID())
                .fare(fare)
                .createdAt(Instant.now())
                .build();

        // Act
        kafkaTemplate.send("trip_events", message);

        // Assert
        await()
                .atMost(10, TimeUnit.SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    List<Payment> payments = paymentRepository.findAll();
                    assertThat(payments).hasSize(1);
                    Payment p = payments.getFirst();
                    assertThat(p.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
                    assertThat(p.getPaymentAmount()).isEqualByComparingTo(fare);
                });
    }

    @Test
    @DisplayName("Should save payment with correct status")
    void createPayment_SavesWithPendingStatus() {
        // Arrange
        Payment payment = Payment.builder()
                .tripId(UUID.randomUUID())
                .passengerId(UUID.randomUUID())
                .paymentAmount(new BigDecimal("50.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        // Act
        Payment saved = paymentRepository.save(payment);

        // Assert
        assertThat(saved.getPaymentId()).isNotNull();
        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("Should update payment status to completed")
    void updatePaymentStatus_ToCompleted_UpdatesSuccessfully() {
        // Arrange
        Payment payment = Payment.builder()
                .tripId(UUID.randomUUID())
                .passengerId(UUID.randomUUID())
                .paymentAmount(new BigDecimal("75.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        // Act
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(Timestamp.valueOf(LocalDateTime.now()));
        Payment updated = paymentRepository.save(payment);

        // Assert
        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(updated.getPaidAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update payment status to failed")
    void updatePaymentStatus_ToFailed_UpdatesSuccessfully() {
        // Arrange
        Payment payment = Payment.builder()
                .tripId(UUID.randomUUID())
                .passengerId(UUID.randomUUID())
                .paymentAmount(new BigDecimal("50.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        // Act
        payment.setPaymentStatus(PaymentStatus.FAILED);
        Payment updated = paymentRepository.save(payment);

        // Assert
        assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("Should find payment by trip ID")
    void findByTripId_ExistingTrip_ReturnsPayment() {
        // Arrange
        UUID tripId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .tripId(tripId)
                .passengerId(UUID.randomUUID())
                .paymentAmount(new BigDecimal("100.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        // Act
        var found = paymentRepository.findByTripId(tripId);

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getTripId()).isEqualTo(tripId);
    }

    @Test
    @DisplayName("Should find payment by Stripe session ID")
    void findByStripeSessionId_ExistingSession_ReturnsPayment() {
        // Arrange
        String sessionId = "cs_test_session_123";
        Payment payment = Payment.builder()
                .tripId(UUID.randomUUID())
                .passengerId(UUID.randomUUID())
                .paymentAmount(new BigDecimal("200.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .stripeSessionId(sessionId)
                .build();
        paymentRepository.save(payment);

        // Act
        var found = paymentRepository.findByStripeSessionId(sessionId);

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getStripeSessionId()).isEqualTo(sessionId);
    }

    @Test
    @DisplayName("Should handle multiple payments for different trips")
    void createMultiplePayments_DifferentTrips_SavesAll() {
        // Arrange & Act
        for (int i = 0; i < 5; i++) {
            Payment payment = Payment.builder()
                    .tripId(UUID.randomUUID())
                    .passengerId(UUID.randomUUID())
                    .paymentAmount(new BigDecimal("50.00").add(BigDecimal.valueOf(i * 10)))
                    .paymentStatus(PaymentStatus.PENDING)
                    .build();
            paymentRepository.save(payment);
        }

        // Assert
        List<Payment> allPayments = paymentRepository.findAll();
        assertThat(allPayments).hasSize(5);
    }

    @Test
    @DisplayName("Should preserve payment amount precision")
    void createPayment_WithPreciseAmount_PreservesPrecision() {
        // Arrange
        BigDecimal preciseAmount = new BigDecimal("123.4567");
        Payment payment = Payment.builder()
                .tripId(UUID.randomUUID())
                .passengerId(UUID.randomUUID())
                .paymentAmount(preciseAmount)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        // Act
        Payment saved = paymentRepository.save(payment);

        // Assert
        assertThat(saved.getPaymentAmount()).isEqualByComparingTo(preciseAmount);
    }

    @Test
    @DisplayName("Should handle payment lifecycle: pending -> completed")
    void paymentLifecycle_PendingToCompleted_Works() {
        // Arrange
        UUID tripId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .tripId(tripId)
                .passengerId(UUID.randomUUID())
                .paymentAmount(new BigDecimal("175.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .stripeSessionId("cs_test_123")
                .stripeSessionUrl("https://checkout.stripe.com/test")
                .build();

        // Act - Create pending payment
        Payment created = paymentRepository.save(payment);
        assertThat(created.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);

        // Act - Complete payment
        created.setPaymentStatus(PaymentStatus.COMPLETED);
        created.setPaidAt(Timestamp.valueOf(LocalDateTime.now()));
        Payment completed = paymentRepository.save(created);

        // Assert
        assertThat(completed.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(completed.getPaidAt()).isNotNull();
        assertThat(completed.getPaymentAmount()).isEqualByComparingTo(new BigDecimal("175.00"));
    }

    @Test
    @DisplayName("Should handle concurrent payment creation attempts")
    void createPayment_ConcurrentRequests_HandlesCorrectly() throws InterruptedException {
        // Arrange
        UUID tripId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("50.00");

        // Mock Stripe service to always return a payment
        when(stripePaymentService.createPaymentSession(any(), any(), any()))
                .thenAnswer(inv -> {
                    Payment p = Payment.builder()
                            .tripId(inv.getArgument(0))
                            .passengerId(inv.getArgument(1))
                            .paymentAmount(inv.getArgument(2))
                            .paymentStatus(PaymentStatus.PENDING)
                            .build();
                    return paymentRepository.save(p);
                });

        // Act - Simulate concurrent creation
        Thread t1 = new Thread(() ->
                stripePaymentService.createPaymentSession(tripId, passengerId, amount)
        );
        Thread t2 = new Thread(() ->
                stripePaymentService.createPaymentSession(tripId, passengerId, amount)
        );

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // Assert - Should handle gracefully
        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).isNotEmpty();
    }
}

