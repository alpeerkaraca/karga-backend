package com.alpeerkaraca.userservice.infra.kafka;

import com.alpeerkaraca.userservice.dto.UserRegisterMessage;
import com.alpeerkaraca.userservice.model.UserProfile;
import com.alpeerkaraca.userservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

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

    /**
     * Consumes 'user_created' messages to create a new user profile.
     * <p>
     * Triggered when a new user registers via Auth Service. It creates a
     * corresponding {@link UserProfile} entity with default ratings.
     * </p>
     *
     * @param message The DTO containing registration details (id, email, name, etc.).
     */
    // TODO: Add a activation notification email when profile is created
    @KafkaListener(topics = "user_created", groupId = "user-service-group")
    public void consumeUserCreatedMessage(UserRegisterMessage message) {
        log.debug("Message received for profile creation.\nReceived message: {}", message);

        UserProfile userProfile = UserProfile.builder()
                .userId(UUID.fromString(message.getId()))
                .email(message.getEmail())
                .firstName(message.getFirstName())
                .lastName(message.getLastName())
                .phoneNumber(message.getPhoneNumber())
                .rating(0.0) // Default rating for new users
                .build();

        userProfileRepository.save(userProfile);
        log.debug("Profile created successfully {}.", userProfile.getUserId());
    }
}