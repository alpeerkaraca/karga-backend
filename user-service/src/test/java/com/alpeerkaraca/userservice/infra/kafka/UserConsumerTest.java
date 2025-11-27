package com.alpeerkaraca.userservice.infra.kafka;

import com.alpeerkaraca.userservice.dto.UserRegisterMessage;
import com.alpeerkaraca.userservice.model.UserProfile;
import com.alpeerkaraca.userservice.repository.UserProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserConsumer Tests")
class UserConsumerTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserConsumer userConsumer;

    @Test
    @DisplayName("Should create user profile from Kafka message successfully")
    void consumeUserCreatedMessage_WithValidMessage_CreatesUserProfile() {
        // Arrange
        String userId = UUID.randomUUID().toString();
        UserRegisterMessage message = UserRegisterMessage.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1234567890")
                .rating(0.0)
                .build();

        UserProfile savedProfile = UserProfile.builder()
                .userId(UUID.fromString(userId))
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1234567890")
                .rating(0.0)
                .build();

        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(savedProfile);

        // Act
        userConsumer.consumeUserCreatedMessage(message);

        // Assert
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());

        UserProfile capturedProfile = captor.getValue();
        assertThat(capturedProfile.getUserId()).isEqualTo(UUID.fromString(userId));
        assertThat(capturedProfile.getEmail()).isEqualTo("test@example.com");
        assertThat(capturedProfile.getFirstName()).isEqualTo("John");
        assertThat(capturedProfile.getLastName()).isEqualTo("Doe");
        assertThat(capturedProfile.getPhoneNumber()).isEqualTo("+1234567890");
        assertThat(capturedProfile.getRating()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should set default rating to 0.0 for new user profiles")
    void consumeUserCreatedMessage_SetsDefaultRating() {
        // Arrange
        String userId = UUID.randomUUID().toString();
        UserRegisterMessage message = UserRegisterMessage.builder()
                .id(userId)
                .email("newuser@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .phoneNumber("+9876543210")
                .rating(0.0)
                .build();

        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userConsumer.consumeUserCreatedMessage(message);

        // Assert
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());

        UserProfile capturedProfile = captor.getValue();
        assertThat(capturedProfile.getRating()).isEqualTo(0.0);
    }
}

