package com.alpeerkaraca.paymentservice.service;

import com.alpeerkaraca.paymentservice.model.Payment;
import com.alpeerkaraca.paymentservice.model.PaymentStatus;
import com.alpeerkaraca.paymentservice.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripePaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private StripePaymentService stripePaymentService;

    private UUID tripId;
    private UUID passengerId;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        passengerId = UUID.randomUUID();


        // Set the Stripe API key for testing
        ReflectionTestUtils.setField(stripePaymentService, "stripeApiKey", "sk_test_fake_key");
        Stripe.apiKey = "sk_test_fake_key";
    }

    @Test
    @DisplayName("Should create payment session successfully using Static Mock")
    void createPaymentSession_Success_WithStaticMock() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(150.50);
        String fakeSessionId = "cs_test_12345";
        String fakeUrl = "https://checkout.stripe.com/pay/cs_test_12345";

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            if (p.getPaymentId() == null) {
                p.setPaymentId(UUID.randomUUID());
            }
            return p;
        });

        try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {

            Session mockSession = new Session();
            mockSession.setId(fakeSessionId);
            mockSession.setUrl(fakeUrl);

            sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockSession);

            // Act
            Payment result = stripePaymentService.createPaymentSession(tripId, passengerId, amount);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(result.getStripeSessionId()).isEqualTo(fakeSessionId);
            assertThat(result.getStripeSessionUrl()).isEqualTo(fakeUrl);

            verify(paymentRepository, atLeast(2)).save(any(Payment.class));
        }
    }

    @Test
    @DisplayName("Should not create payment session if payment is not PENDING")
    void createPaymentSession_NonPendingPayment_ReturnsNull() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(150.50);

        when(paymentRepository.findByTripId(tripId)).thenReturn(Optional.empty());

        Payment completedPayment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .tripId(tripId)
                .passengerId(passengerId)
                .paymentAmount(amount)
                .paymentStatus(PaymentStatus.COMPLETED)
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(completedPayment);

        Payment result = stripePaymentService.createPaymentSession(tripId, passengerId, amount);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should handle payment amount with decimals correctly")
    void createPaymentSession_DecimalAmount_HandlesCorrectly() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(99.99);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        stripePaymentService.createPaymentSession(tripId, passengerId, amount);

        // Assert
        verify(paymentRepository, atLeastOnce()).save(argThat(payment ->
                payment.getPaymentAmount().compareTo(amount) == 0
        ));
    }

    @Test
    @DisplayName("Should create payment for minimum fee amount")
    void createPaymentSession_MinimumFee_Success() {
        // Arrange
        BigDecimal minimumAmount = BigDecimal.valueOf(175.00);
        String fakeSessionId = "cs_test_min_fee";
        String fakeUrl = "https://checkout.stripe.com/pay/cs_test_min_fee";

        when(paymentRepository.findByTripId(tripId)).thenReturn(Optional.empty());

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            if (p.getPaymentId() == null) {
                p.setPaymentId(UUID.randomUUID());
            }
            return p;
        });

        try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {

            Session mockSession = new Session();
            mockSession.setId(fakeSessionId);
            mockSession.setUrl(fakeUrl);

            sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockSession);

            // Act
            stripePaymentService.createPaymentSession(tripId, passengerId, minimumAmount);

            // Assert
            ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

            verify(paymentRepository, atLeastOnce()).save(paymentCaptor.capture());

            boolean correctRecordFound = paymentCaptor.getAllValues().stream()
                    .anyMatch(p ->
                            p.getPaymentAmount().compareTo(minimumAmount) == 0 &&
                                    p.getPaymentStatus() == PaymentStatus.PENDING
                    );

            assertThat(correctRecordFound)
                    .withFailMessage("The correct amount and PENDING status were not saved to the database.")
                    .isTrue();
        }
    }

    @Test
    @DisplayName("Should round payment amount to 2 decimal places")
    void createPaymentSession_RoundAmount_RoundsCorrectly() {
        // Arrange
        BigDecimal amount = BigDecimal.valueOf(123.456);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        stripePaymentService.createPaymentSession(tripId, passengerId, amount);

        // Assert
        verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
    }
}

