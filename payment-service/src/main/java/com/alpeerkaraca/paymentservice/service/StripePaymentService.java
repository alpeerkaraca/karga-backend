package com.alpeerkaraca.paymentservice.service;

import com.alpeerkaraca.common.exception.ResourceNotFoundException;
import com.alpeerkaraca.paymentservice.model.Payment;
import com.alpeerkaraca.paymentservice.model.PaymentStatus;
import com.alpeerkaraca.paymentservice.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentService {

    private final PaymentRepository paymentRepository;
    @Value("${stripe.apiKey}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @Transactional
    public Payment createPaymentSession(UUID tripId, UUID passengerId, BigDecimal paymentAmount) {
        Payment payment = Payment.builder()
                .tripId(tripId)
                .passengerId(passengerId)
                .paymentAmount(paymentAmount)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);

        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            log.warn("This payment already has been proceeded: {}", payment.getPaymentId());
            return null;
        }

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://localhost:8086/api/v1/payments/success?id=" + payment.getPaymentId())
                    .setCancelUrl("http://localhost:8086/api/v1/payments/cancel?id=" + payment.getPaymentId())
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("TRY")
                                    .setUnitAmountDecimal(paymentAmount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP))
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Karga Trip: " + tripId)
                                            .build())
                                    .build()

                            ).build())
                    .setClientReferenceId(payment.getPaymentId().toString())
                    .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                            .putMetadata("trip_id", tripId.toString())
                            .putMetadata("passenger_id", passengerId.toString())
                            .putMetadata("payment_id", payment.getPaymentId().toString())
                            .build()
                    )
                    .putMetadata("trip_id", tripId.toString())
                    .putMetadata("passenger_id", passengerId.toString())
                    .build();
            Session session = Session.create(params);
            payment.setStripeSessionId(session.getId());
            payment.setStripeSessionUrl(session.getUrl());
            payment.setPaymentAmount(paymentAmount.setScale(2, RoundingMode.HALF_UP));
            payment.setPaymentStatus(PaymentStatus.PENDING);
            return paymentRepository.save(payment);
        } catch (Exception e) {
            log.error("Stripe Error: {}", e.getMessage());
            payment.setPaymentStatus(PaymentStatus.FAILED);
            return paymentRepository.save(payment);
        }
    }

    @Async
    public void processWebhookEvent(Event event) {
        PaymentIntent intent;
        switch (event.getType()) {
            case "payment_intent.succeeded":
                intent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject().orElse(null);
                if (intent == null) {
                    log.error("Invalid PaymentIntent in succeeded event");
                    return;
                }
                log.info("Payment succeeded: {}", intent.getId());
                log.info("Payment event: {}\n-----------------------------------------------------------------------------------", event.toJson());

                this.approvePayment(intent);
                break;

            case "payment_intent.payment_failed":
                intent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject().orElse(null);
                if (intent == null) {
                    log.error("Invalid PaymentIntent in failed event");
                    return;
                }
                String message = intent.getLastPaymentError() != null ?
                        intent.getLastPaymentError().getMessage() : "";
                log.warn("Failed Payment Operation: {}, {}", intent.getId(), message);
                this.failedPayment(intent);
                break;

            default:
                log.info("Unhandled event type: {}", event.getType());
        }
    }

    private void approvePayment(PaymentIntent intent) {
        Payment payment = paymentRepository.findById(UUID.fromString(intent.getMetadata().get("payment_id"))).orElseThrow( () -> new ResourceNotFoundException("Payment could not found." +  intent.toJson()));
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(Timestamp.valueOf(LocalDateTime.now()));
        // TODO: Send a notification or email to user.
        paymentRepository.save(payment);
    }

    private void failedPayment(PaymentIntent intent) {
        Payment payment = paymentRepository.findByStripeSessionId(intent.getId()).orElseThrow( () -> new ResourceNotFoundException("Payment could not found." +  intent.toJson()));
        payment.setPaymentStatus(PaymentStatus.FAILED);

        // Send a notification or email to user.
        paymentRepository.save(payment);

    }
}
