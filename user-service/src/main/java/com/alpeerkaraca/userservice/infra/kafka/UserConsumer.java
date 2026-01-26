package com.alpeerkaraca.userservice.infra.kafka;

import com.alpeerkaraca.common.model.InboxStatus;
import com.alpeerkaraca.userservice.dto.UserRegisterMessage;
import com.alpeerkaraca.userservice.model.UserInbox;
import com.alpeerkaraca.userservice.model.UserOutbox;
import com.alpeerkaraca.userservice.model.UserProfile;
import com.alpeerkaraca.userservice.repository.UserInboxRepository;
import com.alpeerkaraca.userservice.repository.UserOutboxRepository;
import com.alpeerkaraca.userservice.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Kafka consumer responsible for handling user-related events.
 * <p>
 * This service listens to topics such as 'user_created' to perform asynchronous
 * operations like creating user profiles in the local database.
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserConsumer {
    private final UserProfileRepository userProfileRepository;
    private final UserInboxRepository userInboxRepository;
    private final UserOutboxRepository userOutboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Consumes user-related events from the Kafka topic `auth_events`.
     * <p>
     * This method handles incoming messages that contain a nested JSON payload
     * representing a `UserRegisterMessage`. It performs the following steps:
     * 1. Logs receipt of the event.
     * 2. Performs an idempotency check using `UserInboxRepository` to skip messages
     * that were already processed.
     * 3. Parses the outer JSON envelope and extracts the inner `payload`.
     * 4. Maps the inner payload to `UserRegisterMessage` and creates a `UserProfile`
     * when the `eventType` equals `UserCreated` and the profile does not already
     * exist.
     * 5. Writes a `UserInbox` entry with status COMPLETED on success or FAILED on error.
     * <p>
     * The method is transactional: database operations (profile creation and inbox
     * write) occur within a single transaction.
     * <p>
     * Parameters:
     *
     * @param messagePayload the raw JSON message received from Kafka (expected to be an envelope with a `payload` field)
     * @param messageId      the unique message identifier provided in the Kafka header `id`
     * @param eventType      the business event type provided in the Kafka header `eventType`
     **/
    // TODO: Add a activation notification email when profile is created.
    @KafkaListener(topics = "auth_events", groupId = "user-service-group")
    @Transactional
    public void consumeUserCreatedMessage(
            @Payload String messagePayload,
            @Header("id") String messageId,
            @Header("eventType") String eventType) {

        log.info("Event received: Type={}, ID={}", eventType, messageId);
        if (userInboxRepository.existsById(messageId)) {
            log.info("Message already processed: {}", messageId);
            return;
        }
        String payloadPlaceholder = "payload";
        UUID userId = null;

        try {
            JsonNode rootNode = objectMapper.readTree(messagePayload);
            if (!rootNode.has(payloadPlaceholder) || rootNode.get(payloadPlaceholder).isNull()) {
                log.warn("Payload is empty, skipping.");
                return;
            }
            String innerJson = rootNode.get(payloadPlaceholder).asText();
            UserRegisterMessage message = objectMapper.readValue(innerJson, UserRegisterMessage.class);
            userId = UUID.fromString(message.getId());

            if ("UserCreated".equals(eventType)) {
                if (userProfileRepository.existsById(userId)) {
                    log.info("User profile already exists for userId: {}", userId);
                    sendSagaReply(userId, "ProfileCreated", "Profile created or existed");
                    return;
                }
                UserProfile userProfile = UserProfile.builder()
                        .userId(userId)
                        .email(message.getEmail())
                        .firstName(message.getFirstName())
                        .lastName(message.getLastName())
                        .phoneNumber(message.getPhoneNumber())
                        .rating(0.0)
                        .build();
                userProfileRepository.save(userProfile);
                log.info("User profile created for userId: {}", userId);

                sendSagaReply(userId, "ProfileCreated", "Profile created successfully");

                UserInbox inboxEntry = new UserInbox();
                inboxEntry.setMessageId(messageId);
                inboxEntry.setEventType(eventType);
                inboxEntry.setStatus(InboxStatus.COMPLETED);
                userInboxRepository.save(inboxEntry);
            }
        } catch (Exception e) {
            log.error("Error processing message ID {}: {}", messageId, e.getMessage());

            if (userId != null) {
                sendSagaReply(userId, "ProfileCreateFailed", "Error: " + e.getMessage());
            }

            UserInbox inboxEntry = new UserInbox();
            inboxEntry.setMessageId(messageId);
            inboxEntry.setEventType(eventType);
            inboxEntry.setStatus(InboxStatus.FAILED);
            userInboxRepository.save(inboxEntry);
        }
    }

    private void sendSagaReply(UUID userId, String eventType, String payload) {
        try {
            UserOutbox outboxEvent = new UserOutbox();
            outboxEvent.setAggregateId(userId.toString());
            outboxEvent.setEventType(eventType);
            outboxEvent.setPayload(payload);
            userOutboxRepository.save(outboxEvent);
            log.info("Saga reply for userId: {}, eventType: {}", userId, eventType);
        } catch (Exception e) {
            log.error("Failed to send saga reply for userId: {}, error: {}", userId, e.getMessage());
            throw e;
        }
    }
}