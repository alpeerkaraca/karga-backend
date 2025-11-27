package com.alpeerkaraca.userservice.controller;

import com.alpeerkaraca.common.exception.GlobalExceptionHandler;
import com.alpeerkaraca.common.exception.ResourceNotFoundException;
import com.alpeerkaraca.userservice.dto.UpdateUserProfileDto;
import com.alpeerkaraca.userservice.model.UserProfile;
import com.alpeerkaraca.userservice.service.UserProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Import(GlobalExceptionHandler.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserProfileService userProfileService;

    @Nested
    @DisplayName("GET /api/v1/users/me - Get User Profile")
    class GetUserProfileTests {

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
        @DisplayName("Should get current user profile successfully")
        void getMe_WithAuthenticatedUser_ReturnsUserProfile() throws Exception {
            // Arrange
            UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            UserProfile userProfile = UserProfile.builder()
                    .userId(userId)
                    .email("test@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .phoneNumber("+1234567890")
                    .rating(4.5)
                    .build();

            when(userProfileService.getUserById(userId)).thenReturn(userProfile);

            // Act & Assert
            mockMvc.perform(get("/api/v1/users/me"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User profile retrieved successfully."))
                    .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.data.email").value("test@example.com"))
                    .andExpect(jsonPath("$.data.firstName").value("John"))
                    .andExpect(jsonPath("$.data.lastName").value("Doe"))
                    .andExpect(jsonPath("$.data.rating").value(4.5));

            verify(userProfileService).getUserById(userId);
        }

        @Test
        @DisplayName("Should return unauthorized when user is not authenticated")
        void getMe_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isUnauthorized());

            verify(userProfileService, never()).getUserById(any());
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"PASSENGER"})
        @DisplayName("Should handle service exceptions gracefully")
        void getMe_WhenServiceThrowsException_ReturnsError() throws Exception {
            // Arrange
            UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            when(userProfileService.getUserById(userId))
                    .thenThrow(new ResourceNotFoundException("User not found."));

            // Act & Assert
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isNotFound());

            verify(userProfileService).getUserById(userId);
        }

        @Test
        @WithMockUser(username = "invalid-uuid-format")
        @DisplayName("Should handle invalid UUID format")
        void getMe_WithInvalidUUID_ReturnsError() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isInternalServerError());

            verify(userProfileService, never()).getUserById(any());
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
        @DisplayName("Should return profile with zero rating for new users")
        void getMe_NewUser_ReturnsZeroRating() throws Exception {
            // Arrange
            UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            UserProfile userProfile = UserProfile.builder()
                    .userId(userId)
                    .email("newuser@example.com")
                    .firstName("New")
                    .lastName("User")
                    .phoneNumber("+1111111111")
                    .rating(0.0)
                    .build();

            when(userProfileService.getUserById(userId)).thenReturn(userProfile);

            // Act & Assert
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.rating").value(0.0));
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
        @DisplayName("Should handle profile with special characters in name")
        void getMe_WithSpecialCharacters_ReturnsCorrectly() throws Exception {
            // Arrange
            UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            UserProfile userProfile = UserProfile.builder()
                    .userId(userId)
                    .email("test@example.com")
                    .firstName("José")
                    .lastName("O'Brien")
                    .phoneNumber("+1234567890")
                    .rating(4.5)
                    .build();

            when(userProfileService.getUserById(userId)).thenReturn(userProfile);

            // Act & Assert
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.firstName").value("José"))
                    .andExpect(jsonPath("$.data.lastName").value("O'Brien"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me - Update User Profile")
    class UpdateUserProfileTests {

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
        @DisplayName("Should update user profile successfully")
        void updateMe_WithValidProfile_ReturnsUpdatedProfile() throws Exception {
            // Arrange
            UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            UpdateUserProfileDto updateRequest = UpdateUserProfileDto.builder()
                    .email("updated@example.com")
                    .firstName("Jane")
                    .lastName("Smith")
                    .phoneNumber("+93876543210")
                    .rating(4.8)
                    .build();

            UserProfile updatedProfile = UserProfile.builder()
                    .userId(userId)
                    .email("updated@example.com")
                    .firstName("Jane")
                    .lastName("Smith")
                    .phoneNumber("+93876543210")
                    .rating(4.8)
                    .build();

            when(userProfileService.updateProfile(any(UpdateUserProfileDto.class), eq(userId))).thenReturn(updatedProfile);

            // Act & Assert
            mockMvc.perform(put("/api/v1/users/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User profile updated successfully."))
                    .andExpect(jsonPath("$.data.firstName").value("Jane"))
                    .andExpect(jsonPath("$.data.lastName").value("Smith"))
                    .andExpect(jsonPath("$.data.email").value("updated@example.com"));

            verify(userProfileService).updateProfile(any(UpdateUserProfileDto.class), eq(userId));
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"PASSENGER"})
        @DisplayName("Should reject update when user ID matches authenticated user")
        void updateMe_WithSameUserId_ThrowsException() throws Exception {
            // Arrange
            UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            UserProfile updateRequest = UserProfile.builder()
                    .userId(userId) // Same as authenticated user
                    .email("updated@example.com")
                    .firstName("Jane")
                    .lastName("Smith")
                    .phoneNumber("+9876543210")
                    .rating(4.8)
                    .build();

            // Act & Assert
            mockMvc.perform(put("/api/v1/users/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isBadRequest());

            verify(userProfileService, never()).updateProfile(any());
        }

        @Test
        @DisplayName("Should return unauthorized when updating without authentication")
        void updateMe_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
            // Arrange
            UserProfile updateRequest = UserProfile.builder()
                    .userId(UUID.randomUUID())
                    .email("updated@example.com")
                    .firstName("Jane")
                    .lastName("Smith")
                    .phoneNumber("+9876543210")
                    .rating(4.8)
                    .build();

            // Act & Assert
            mockMvc.perform(put("/api/v1/users/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isUnauthorized());

            verify(userProfileService, never()).updateProfile((UserProfile) any());
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
        @DisplayName("Should handle partial profile update")
        void updateMe_PartialUpdate_UpdatesSuccessfully() throws Exception {
            // Arrange
            UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            UpdateUserProfileDto updateRequest = UpdateUserProfileDto.builder()
                    .firstName("NewFirstName")
                    .build();

            UserProfile updatedProfile = UserProfile.builder()
                    .userId(userId)
                    .email("existing@example.com")
                    .firstName("NewFirstName")
                    .lastName("ExistingLastName")
                    .phoneNumber("+1234567890")
                    .rating(4.5)
                    .build();

            when(userProfileService.updateProfile(any(UpdateUserProfileDto.class), eq(userId))).thenReturn(updatedProfile);

            // Act & Assert
            mockMvc.perform(put("/api/v1/users/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.firstName").value("NewFirstName"));
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
        @DisplayName("Should handle empty request body")
        void updateMe_EmptyBody_ReturnsBadRequest() throws Exception {
            // Act & Assert
            mockMvc.perform(put("/api/v1/users/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
        @DisplayName("Should handle malformed JSON")
        void updateMe_MalformedJson_ReturnsBadRequest() throws Exception {
            // Act & Assert
            mockMvc.perform(put("/api/v1/users/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("invalid-json"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000")
        @DisplayName("Should update phone number format")
        void updateMe_PhoneNumberUpdate_UpdatesSuccessfully() throws Exception {
            // Arrange
            UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            UpdateUserProfileDto updateRequest = UpdateUserProfileDto.builder()
                    .phoneNumber("+905551234567") // Turkish phone format
                    .build();

            UserProfile updatedProfile = UserProfile.builder()
                    .userId(userId)
                    .email("test@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .phoneNumber("+905551234567")
                    .rating(4.5)
                    .build();

            when(userProfileService.updateProfile(any(UpdateUserProfileDto.class), eq(userId))).thenReturn(updatedProfile);

            // Act & Assert
            mockMvc.perform(put("/api/v1/users/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.phoneNumber").value("+905551234567"));
        }
    }
}