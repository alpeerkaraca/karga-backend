package com.alpeerkaraca.driverservice.infra.kafka;

import com.alpeerkaraca.common.event.TripMessage;
import com.alpeerkaraca.common.model.InboxStatus;
import com.alpeerkaraca.common.model.TripEventTypes;
import com.alpeerkaraca.driverservice.model.DriverInbox;
import com.alpeerkaraca.driverservice.model.DriverStatus;
import com.alpeerkaraca.driverservice.repository.DriverInboxRepository;
import com.alpeerkaraca.driverservice.repository.DriverOutboxRepository;
import com.alpeerkaraca.driverservice.service.DriverStatusService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TripEventConsumerService {
    private static final String TOPIC_TRIP_EVENTS = "trip_events";
    private final DriverStatusService driverStatusService;
    private final DriverInboxRepository driverInboxRepository;
    private final DriverOutboxRepository driverOutboxRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = TOPIC_TRIP_EVENTS, groupId = "driver-service-group")
    @Transactional
    public void handleTripEvent(
            @Payload String messagePayload,
            @Header("id") String messageId,
            @Header("eventType") String eventType
    ) {
        log.info("Event received: Type={}, ID={}", eventType, messageId);
        if (driverInboxRepository.existsById(messageId)) {
            log.info("Message already processed: {}", messageId);
            return;
        }
        try {
            JsonNode rootNode = objectMapper.readTree(messagePayload);
            if (!rootNode.has("payload") || rootNode.get("payload").isNull()) {
                log.warn("Payload is empty, skipping.");
                return;
            }
            String innerJson = rootNode.get("payload").asText();

            TripMessage event = objectMapper.readValue(innerJson, TripMessage.class);
            UUID driverId = event.getDriverId();
            TripEventTypes type = EnumUtils.getEnum(TripEventTypes.class, eventType);

            if (type != null && type == event.getEventType()) {
                switch (event.getEventType()) {
                    case TRIP_ACCEPTED, TRIP_STARTED:
                        log.info("Trip accepted/started event received. Driver {} setting as BUSY .", driverId);
                        driverStatusService.updateDriverStatus(driverId, DriverStatus.BUSY, event.getCurrentLongitude(), event.getCurrentLatitude());
                        break;
                    case TRIP_COMPLETED, TRIP_CANCELLED:
                        log.info("Trip completed/cancelled event received. Driver {} setting as ONLINE .", driverId);
                        driverStatusService.updateDriverStatus(driverId, DriverStatus.ONLINE, event.getCurrentLongitude(), event.getCurrentLatitude());
                        break;
                    default:
                        log.warn("Unknown trip event type received: {}", eventType);
                }
            } else {
                log.warn("Event type mismatch: expected {}, but got {}", event.getEventType(), eventType);
            }

            DriverInbox inboxEntry = new DriverInbox();
            inboxEntry.setMessageId(messageId);
            inboxEntry.setEventType(eventType);
            inboxEntry.setProcessedAt(Instant.now());
            inboxEntry.setStatus(InboxStatus.COMPLETED);
            driverInboxRepository.save(inboxEntry);
        } catch (Exception e) {
            log.error("Error occurred while processing trip event: {}", eventType, e);
        }
    }
}
