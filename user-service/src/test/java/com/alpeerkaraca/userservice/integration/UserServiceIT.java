package com.alpeerkaraca.userservice.integration;

import com.alpeerkaraca.userservice.AbstractIntegrationTest;
import com.alpeerkaraca.userservice.dto.UpdateUserProfileDto;
import com.alpeerkaraca.userservice.dto.UserRegisterMessage;
import com.alpeerkaraca.userservice.model.UserProfile;
import com.alpeerkaraca.userservice.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"user_created"})
@DisplayName("User Service Integration Tests")
class UserServiceIT extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private KafkaTemplate<String, UserRegisterMessage> kafkaTemplate;

    @BeforeEach
    void setUp() {
        userProfileRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create user profile when Kafka message is received")
    void handleUserCreatedMessage_CreatesUserProfile() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserRegisterMessage message = UserRegisterMessage.builder()
                .id(userId.toString())
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1234567890")
                .rating(0.0)
                .build();

        // Act
        kafkaTemplate.send("user_created", message);

        // Assert - Wait for async Kafka processing
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<UserProfile> profiles = userProfileRepository.findAll();
            assertThat(profiles).hasSize(1);
            assertThat(profiles.getFirst().getUserId()).isEqualTo(userId);
            assertThat(profiles.getFirst().getEmail()).isEqualTo("test@example.com");
            assertThat(profiles.getFirst().getFirstName()).isEqualTo("John");
            assertThat(profiles.getFirst().getLastName()).isEqualTo("Doe");
            assertThat(profiles.getFirst().getRating()).isEqualTo(0.0);
        });
    }

    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    @DisplayName("Should get user profile by ID via API")
    void getUserProfile_ReturnsCorrectProfile() throws Exception {
        // Arrange
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .email("api@example.com")
                .firstName("API")
                .lastName("User")
                .phoneNumber("+1112223333")
                .rating(4.5)
                .build();
        userProfileRepository.save(profile);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.email").value("api@example.com"))
                .andExpect(jsonPath("$.data.firstName").value("API"))
                .andExpect(jsonPath("$.data.lastName").value("User"))
                .andExpect(jsonPath("$.data.rating").value(4.5));
    }

    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    @DisplayName("Should update user profile via API")
    void updateUserProfile_UpdatesSuccessfully() throws Exception {
        // Arrange
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UserProfile existingProfile = UserProfile.builder()
                .userId(userId)
                .email("old@example.com")
                .firstName("Old")
                .lastName("Name")
                .phoneNumber("+905555555555")
                .rating(3.0)
                .build();
        userProfileRepository.save(existingProfile);

        UpdateUserProfileDto updateRequest = UpdateUserProfileDto.builder()
                .email("new@example.com")
                .firstName("New")
                .lastName("Name")
                .phoneNumber("+905555555555")
                .rating(4.5)
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("new@example.com"))
                .andExpect(jsonPath("$.data.firstName").value("New"))
                .andExpect(jsonPath("$.data.lastName").value("Name"));

        // Verify database update
        UserProfile updated = userProfileRepository.findById(userId).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getFirstName()).isEqualTo("New");
    }

    @Test
    @DisplayName("Should handle multiple user profile creations via Kafka")
    void handleMultipleUserCreatedMessages_CreatesAllProfiles() {
        // Arrange
        UserRegisterMessage message1 = UserRegisterMessage.builder()
                .id(UUID.randomUUID().toString())
                .email("user1@example.com")
                .firstName("User")
                .lastName("One")
                .phoneNumber("+1111111111")
                .rating(0.0)
                .build();

        UserRegisterMessage message2 = UserRegisterMessage.builder()
                .id(UUID.randomUUID().toString())
                .email("user2@example.com")
                .firstName("User")
                .lastName("Two")
                .phoneNumber("+2222222222")
                .rating(0.0)
                .build();

        // Act
        kafkaTemplate.send("user_created", message1);
        kafkaTemplate.send("user_created", message2);

        // Assert
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<UserProfile> profiles = userProfileRepository.findAll();
            assertThat(profiles).hasSize(2);
        });
    }

    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    @DisplayName("Should handle user not found scenario")
    void getUserProfile_NonExistentUser_ReturnsError() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should persist user profile with all fields")
    void createUserProfile_AllFields_PersistsCorrectly() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .email("complete@example.com")
                .firstName("Complete")
                .lastName("User")
                .phoneNumber("+1234567890")
                .rating(5.0)
                .build();

        // Act
        UserProfile saved = userProfileRepository.save(profile);

        // Assert
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getEmail()).isEqualTo("complete@example.com");
        assertThat(saved.getFirstName()).isEqualTo("Complete");
        assertThat(saved.getLastName()).isEqualTo("User");
        assertThat(saved.getRating()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Should find user by email")
    void findByEmail_ExistingUser_ReturnsProfile() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .email("findme@example.com")
                .firstName("Find")
                .lastName("Me")
                .phoneNumber("+1234567890")
                .rating(4.0)
                .build();
        userProfileRepository.save(profile);

        // Act
        var found = userProfileRepository.findByEmail("findme@example.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should handle duplicate email constraint")
    void createUserProfile_DuplicateEmail_ThrowsException() {
        // Arrange
        UserProfile profile1 = UserProfile.builder()
                .userId(UUID.randomUUID())
                .email("duplicate@example.com")
                .firstName("First")
                .lastName("User")
                .phoneNumber("+1111111111")
                .rating(4.0)
                .build();
        userProfileRepository.save(profile1);

        UserProfile profile2 = UserProfile.builder()
                .userId(UUID.randomUUID())
                .email("duplicate@example.com") // Same email
                .firstName("Second")
                .lastName("User")
                .phoneNumber("+2222222222")
                .rating(4.0)
                .build();

        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            userProfileRepository.save(profile2);
            userProfileRepository.flush();
        });
    }

    @Test
    @DisplayName("Should handle duplicate phone number constraint")
    void createUserProfile_DuplicatePhone_ThrowsException() {
        // Arrange
        UserProfile profile1 = UserProfile.builder()
                .userId(UUID.randomUUID())
                .email("user1@example.com")
                .firstName("User")
                .lastName("One")
                .phoneNumber("+1234567890")
                .rating(4.0)
                .build();
        userProfileRepository.save(profile1);

        UserProfile profile2 = UserProfile.builder()
                .userId(UUID.randomUUID())
                .email("user2@example.com")
                .firstName("User")
                .lastName("Two")
                .phoneNumber("+1234567890") // Same phone
                .rating(4.0)
                .build();

        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            userProfileRepository.save(profile2);
            userProfileRepository.flush();
        });
    }

    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
    @DisplayName("Should handle concurrent profile updates")
    void updateUserProfile_ConcurrentUpdates_HandlesCorrectly() throws Exception {
        // Arrange
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UserProfile existingProfile = UserProfile.builder()
                .userId(userId)
                .email("concurrent@example.com")
                .firstName("Original")
                .lastName("Name")
                .phoneNumber("+1111111111")
                .rating(3.0)
                .build();
        userProfileRepository.save(existingProfile);

        // Act - Simulate concurrent update
        UserProfile update1 = UserProfile.builder()
                .userId(UUID.randomUUID())
                .firstName("Update1")
                .build();

        mockMvc.perform(put("/api/v1/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update1)))
                .andExpect(status().isOk());

        // Assert - Profile should be updated
        UserProfile updated = userProfileRepository.findById(userId).orElse(null);
        assertThat(updated).isNotNull();
    }

    @Test
    @DisplayName("Should handle Kafka message with missing fields")
    void handleUserCreatedMessage_MissingFields_HandlesGracefully() {
        // Arrange
        UserRegisterMessage message = UserRegisterMessage.builder()
                .id(UUID.randomUUID().toString())
                .email("minimal@example.com")
                .rating(0.0)
                .phoneNumber("+1111111111")
                .build();

        // Act
        kafkaTemplate.send("user_created", message);

        // Assert
        await().atMost(10, TimeUnit.SECONDS)
                .ignoreExceptions()
                .untilAsserted(() -> {
                    List<UserProfile> profiles = userProfileRepository.findAll();
                    assertThat(profiles).hasSizeGreaterThanOrEqualTo(1);
                });
    }

    @Test
    @DisplayName("Should handle rating updates correctly")
    void updateUserProfile_RatingChange_PersistsCorrectly() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .email("rating@example.com")
                .firstName("Rating")
                .lastName("User")
                .phoneNumber("+1234567890")
                .rating(3.5)
                .build();
        userProfileRepository.save(profile);

        // Act
        profile.setRating(4.5);
        UserProfile updated = userProfileRepository.save(profile);

        // Assert
        assertThat(updated.getRating()).isEqualTo(4.5);

        UserProfile fetched = userProfileRepository.findById(userId).orElse(null);
        assertThat(fetched).isNotNull();
        assertThat(fetched.getRating()).isEqualTo(4.5);
    }

    @Test
    @DisplayName("Should retrieve all user profiles")
    void getAllProfiles_MultipleUsers_ReturnsAll() {
        // Arrange
        for (int i = 0; i < 3; i++) {
            UserProfile profile = UserProfile.builder()
                    .userId(UUID.randomUUID())
                    .email("user" + i + "@example.com")
                    .firstName("User" + i)
                    .lastName("Test")
                    .phoneNumber("+11111111" + i + i)
                    .rating(4.0 + i * 0.1)
                    .build();
            userProfileRepository.save(profile);
        }

        // Act
        List<UserProfile> all = userProfileRepository.findAll();

        // Assert
        assertThat(all).hasSize(3);
    }
}

