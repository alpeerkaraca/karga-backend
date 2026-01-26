package com.alpeerkaraca.tripservice.infra.kafka;

import com.alpeerkaraca.common.event.PaymentMessage;
import com.alpeerkaraca.common.model.TripEventTypes;
import com.alpeerkaraca.tripservice.model.Trip;
import com.alpeerkaraca.tripservice.model.TripInbox;
import com.alpeerkaraca.tripservice.model.TripOutbox;
import com.alpeerkaraca.tripservice.model.TripStatus;
import com.alpeerkaraca.tripservice.repository.TripInboxRepository;
import com.alpeerkaraca.tripservice.repository.TripOutboxRepository;
import com.alpeerkaraca.tripservice.repository.TripRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventListener {

    private static final String PAYMENT_EVENTS_TOPIC = "payment_events";
    private final TripRepository tripRepository;
    private final TripInboxRepository tripInboxRepository;
    private final ObjectMapper objectMapper;
    private final TripOutboxRepository tripOutboxRepository;


    @KafkaListener(topics = PAYMENT_EVENTS_TOPIC, groupId = "trip-service-saga-group")
    @Transactional
    public void handlePaymentEvent(
            @Payload String messagePayload,
            @Header("eventType") String eventType,
            @Header(value = "id", required = false) String messageId
    ) {
        if (messageId != null && tripInboxRepository.existsById(messageId)) {
            log.info("Duplicate message ignored: {}", messageId);
            return;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(messagePayload);
            if (!rootNode.has("payload") || rootNode.get("payload").isNull()) {
                log.warn("Received message without payload: {}", messagePayload);
                return;
            }
            String innerJson = rootNode.get("payload").asText();
            PaymentMessage event = objectMapper.readValue(innerJson, PaymentMessage.class);

            Trip trip = tripRepository.findById(event.getTripId())
                    .orElseThrow(() -> new RuntimeException("Trip not found: " + event.getTripId()));

            if (eventType.equals(TripEventTypes.PAYMENT_SUCCESSFUL.toString())) {
                log.info("Trip {} paid successfully.", trip.getTripId());
                trip.setTripStatus(TripStatus.PAID);
            } else if (eventType.equals(TripEventTypes.PAYMENT_FAILED.toString())) {
                log.warn("SAGA COMPENSATION: Payment failed for trip {}", trip.getTripId());
                trip.setTripStatus(TripStatus.PAYMENT_FAILED);

                TripOutbox tripOutbox = new TripOutbox();
                tripOutbox.setAggregateId(trip.getTripId().toString());
                tripOutbox.setEventType("TripCancelled");
                tripOutbox.setPayload("{\"reason\": \"Payment Failed\"}");
                tripOutboxRepository.save(tripOutbox);
            }
            tripRepository.save(trip);

            if (messageId != null) {
                TripInbox inbox = new TripInbox();
                inbox.setMessageId(messageId);
                inbox.setEventType(eventType);
                tripInboxRepository.save(inbox);
            }
        } catch (Exception e) {
            log.error("Error processing payment event", e);
            throw new RuntimeException("Temporary failure processing payment event", e);
        }
    }
}
