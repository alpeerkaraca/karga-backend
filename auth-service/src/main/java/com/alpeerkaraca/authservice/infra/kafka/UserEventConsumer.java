package com.alpeerkaraca.authservice.infra.kafka;

import com.alpeerkaraca.authservice.model.AuthInbox;
import com.alpeerkaraca.authservice.repository.AuthInboxRepository;
import com.alpeerkaraca.authservice.service.AuthService;
import com.alpeerkaraca.common.model.InboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserEventConsumer {

    private final AuthService authService;
    private final AuthInboxRepository authInboxRepository;

    @KafkaListener(topics = "user_events", groupId = "auth-service-group")
    @Transactional
    public void consumeUserEvent(
            @Header(value = "id", required = false) String messageId,
            @Header(value = "eventType", required = false) String eventType,
            @Header(value = "aggregateId", required = false) String aggregateId) {

        log.info("Saga Reply received. Type: {}, ID: {}", eventType, messageId);
        if (messageId != null && authInboxRepository.existsById(messageId)) {
            log.info("Message already processed: {}", messageId);
            return;
        }
        try {
            UUID userId = aggregateId != null ? UUID.fromString(aggregateId) : null;

            if ("ProfileCreated".equals(eventType)) {
                authService.activateUser(userId);
            } else if ("ProfileCreateFailed".equals(eventType)) {
                authService.rollbackUser(userId);
            }

            AuthInbox inboxEvent = new AuthInbox();
            inboxEvent.setMessageId(messageId);
            inboxEvent.setStatus(InboxStatus.COMPLETED);
            inboxEvent.setEventType(eventType);
            authInboxRepository.save(inboxEvent);
        } catch (Exception e) {
            log.error("Error processing message ID {}: {}", messageId, e.getMessage());
        }
    }

}
