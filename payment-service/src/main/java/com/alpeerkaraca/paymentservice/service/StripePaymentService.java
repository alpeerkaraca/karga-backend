package com.alpeerkaraca.paymentservice.service;

import com.alpeerkaraca.common.event.PaymentMessage;
import com.alpeerkaraca.common.exception.ResourceNotFoundException;
import com.alpeerkaraca.common.exception.SerializationException;
import com.alpeerkaraca.common.model.TripEventTypes;
import com.alpeerkaraca.paymentservice.model.Payment;
import com.alpeerkaraca.paymentservice.model.PaymentOutbox;
import com.alpeerkaraca.paymentservice.model.PaymentStatus;
import com.alpeerkaraca.paymentservice.repository.PaymentOutboxRepository;
import com.alpeerkaraca.paymentservice.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service managing Stripe API integration for payments.
 * <p>
 * This service handles the creation of Stripe Checkout Sessions for trips
 * and processes asynchronous Webhook events (e.g., payment_succeeded, payment_failed).
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentService {

    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final PaymentOutboxRepository paymentOutboxRepository;

    @Value("${stripe.apiKey}")
    private String stripeApiKey;

    /**
     * Initializes the Stripe API key during application startup.
     */
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    /**
     * Creates a Stripe Checkout Session for a specific trip payment.
     * <p>
     * If a pending payment session already exists for the trip, it returns that session.
     * Otherwise, creates a new session with line items and success/cancel URLs.
     * </p>
     *
     * @param tripId        The ID of the trip to pay for.
     * @param passengerId   The ID of the passenger making the payment.
     * @param paymentAmount The amount to be paid.
     * @return The persisted {@link Payment} entity with Stripe session details, or null if already processed.
     */
    @Transactional
    public Payment createPaymentSession(UUID tripId, UUID passengerId, BigDecimal paymentAmount) {
        var existingPayment = paymentRepository.findByTripId(tripId);
        if (existingPayment.isPresent()) {
            return existingPayment.get();
        }

        Payment payment = Payment.builder()
                .tripId(tripId)
                .passengerId(passengerId)
                .paymentAmount(paymentAmount)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);

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

    /**
     * Asynchronously processes incoming Stripe Webhook events.
     * <p>
     * Handles 'payment_intent.succeeded' and 'payment_intent.payment_failed' events
     * to update the local payment status accordingly.
     * </p>
     *
     * @param event The Stripe event object.
     */
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

                approvePayment(intent);
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

    @Transactional
    protected void approvePayment(PaymentIntent intent) {
        Payment payment = paymentRepository.findById(UUID.fromString(intent.getMetadata().get("payment_id")))
                .orElseThrow(() -> new ResourceNotFoundException("Payment could not found." + intent.toJson()));
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(Timestamp.valueOf(LocalDateTime.now()));
        paymentRepository.save(payment);

        saveToOutbox(payment, TripEventTypes.PAYMENT_SUCCESSFUL, null);
    }

    @Transactional
    protected void failedPayment(PaymentIntent intent) {
        Payment payment = paymentRepository.findByStripeSessionId(intent.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment could not found." + intent.toJson()));
        payment.setPaymentStatus(PaymentStatus.FAILED);

        // Send a notification or email to user.
        paymentRepository.save(payment);

        String failureReason = intent.getLastPaymentError() != null ?
                intent.getLastPaymentError().getMessage() : "Unknown reason";
        saveToOutbox(payment, TripEventTypes.PAYMENT_FAILED, failureReason);
    }

    private void saveToOutbox(Payment payment, TripEventTypes eventType, String failureReason) {
        try {
            PaymentMessage message = PaymentMessage.builder()
                    .paymentId(payment.getPaymentId().toString())
                    .tripId(payment.getTripId())
                    .passengerId(payment.getPassengerId())
                    .amount(payment.getPaymentAmount())
                    .eventType(eventType)
                    .failureReason(failureReason)
                    .createdAt(Instant.now())
                    .build();
            PaymentOutbox outbox = new PaymentOutbox();
            outbox.setAggregateType("PAYMENT");
            outbox.setAggregateId(payment.getPaymentId().toString());
            outbox.setEventType(eventType.toString());
            outbox.setPayload(objectMapper.writeValueAsString(message));

            paymentOutboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Outbox serialization error", e);
            throw new SerializationException("Error saving payment event");
        }

    }
}