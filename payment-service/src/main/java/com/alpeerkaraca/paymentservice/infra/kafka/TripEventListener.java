package com.alpeerkaraca.paymentservice.infra.kafka;

import com.alpeerkaraca.common.event.TripMessage;
import com.alpeerkaraca.common.model.TripEventTypes;
import com.alpeerkaraca.paymentservice.model.PaymentInbox;
import com.alpeerkaraca.paymentservice.repository.PaymentInboxRepository;
import com.alpeerkaraca.paymentservice.service.StripePaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class TripEventListener {
    private static final String TOPIC_TRIP_EVENTS = "trip_events";
    private final StripePaymentService paymentService;
    private final PaymentInboxRepository paymentInboxRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = TOPIC_TRIP_EVENTS, groupId = "payment-service-group")
    public void handleTripEvent(
            @Payload String messagePayload,
            @Header("eventType") String eventType,
            @Header("id") String messageId
    ) {
        if (paymentInboxRepository.existsById(messageId)) {
            return;
        }
        try {
            JsonNode rootNode = objectMapper.readTree(messagePayload);
            if (!rootNode.has("payload") || rootNode.get("payload").isNull()) {
                log.warn("Received message without payload: {}", messagePayload);
                return;
            }
            String innerJson = rootNode.get("payload").asText();
            TripMessage event = objectMapper.readValue(innerJson, TripMessage.class);
            if (event.getEventType() == TripEventTypes.TRIP_COMPLETED) {
                log.info("Trip completed event received: {}", event.getTripId());

                paymentService.createPaymentSession(
                        event.getTripId(),
                        event.getPassengerId(),
                        event.getFare()
                );
            }
            PaymentInbox paymentInbox = new PaymentInbox();
            paymentInbox.setMessageId(messageId);
            paymentInbox.setEventType(eventType);
            paymentInbox.setProcessedAt(Instant.now());
            paymentInboxRepository.save(paymentInbox);
        } catch (Exception e) {
            log.error("Payment failed or processing error", e);


        }
    }
}
