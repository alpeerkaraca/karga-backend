package com.alpeerkaraca.userservice.infra.kafka;

import com.alpeerkaraca.userservice.dto.UserRegisterMessage;
import com.alpeerkaraca.userservice.model.UserProfile;
import com.alpeerkaraca.userservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserConsumer {
    private final UserProfileRepository userProfileRepository;

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
                .rating(0.0)
                .build();
        userProfileRepository.save(userProfile);
        log.debug("Profile created successfully {}.", userProfile.getUserId());


    }

}
