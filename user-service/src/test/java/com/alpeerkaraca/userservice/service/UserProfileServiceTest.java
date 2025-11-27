package com.alpeerkaraca.userservice.service;

import com.alpeerkaraca.common.exception.ResourceNotFoundException;
import com.alpeerkaraca.userservice.dto.UpdateUserProfileDto;
import com.alpeerkaraca.userservice.model.UserProfile;
import com.alpeerkaraca.userservice.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileService Tests")
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    private UserProfile testUserProfile;
    private UUID testUserId;
    private String testEmail;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testEmail = "test@example.com";

        testUserProfile = UserProfile.builder()
                .userId(testUserId)
                .email(testEmail)
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1234567890")
                .rating(4.5)
                .build();
    }

    @Test
    @DisplayName("Should get user by email successfully")
    void getUserByEmail_WithValidEmail_ReturnsUser() {
        // Arrange
        when(userProfileRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUserProfile));

        // Act
        UserProfile result = userProfileService.getUserByEmail(testEmail);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(testEmail);
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        verify(userProfileRepository).findByEmail(testEmail);
    }

    @Test
    @DisplayName("Should throw exception when user not found by email")
    void getUserByEmail_WithNonExistentEmail_ThrowsException() {
        // Arrange
        String nonExistentEmail = "nonexistent@example.com";
        when(userProfileRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userProfileService.getUserByEmail(nonExistentEmail))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found.");

        verify(userProfileRepository).findByEmail(nonExistentEmail);
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void getUserById_WithValidId_ReturnsUser() {
        // Arrange
        when(userProfileRepository.findById(testUserId)).thenReturn(Optional.of(testUserProfile));

        // Act
        UserProfile result = userProfileService.getUserById(testUserId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUserId);
        assertThat(result.getEmail()).isEqualTo(testEmail);
        assertThat(result.getRating()).isEqualTo(4.5);
        verify(userProfileRepository).findById(testUserId);
    }

    @Test
    @DisplayName("Should throw exception when user not found by ID")
    void getUserById_WithNonExistentId_ThrowsException() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(userProfileRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userProfileService.getUserById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found.");

        verify(userProfileRepository).findById(nonExistentId);
    }

    @Test
    @DisplayName("Should update user profile successfully")
    void updateProfile_WithValidProfile_ReturnsUpdatedProfile() {
        // Arrange
        UserProfile updatedProfile = UserProfile.builder()
                .userId(testUserId)
                .email(testEmail)
                .firstName("Jane")
                .lastName("Smith")
                .phoneNumber("+9876543210")
                .rating(4.8)
                .build();

        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(updatedProfile);

        // Act
        UserProfile result = userProfileService.updateProfile(updatedProfile);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Jane");
        assertThat(result.getLastName()).isEqualTo("Smith");
        assertThat(result.getPhoneNumber()).isEqualTo("+9876543210");
        assertThat(result.getRating()).isEqualTo(4.8);
        verify(userProfileRepository).save(updatedProfile);
    }

    @Test
    @DisplayName("Should update only specific fields")
    void updateProfile_WithPartialUpdate_UpdatesOnlySpecifiedFields() {
        // Arrange
        UserProfile partialUpdate = UserProfile.builder()
                .userId(testUserId)
                .email(testEmail)
                .firstName("UpdatedName")
                .lastName("Doe")
                .phoneNumber("+1234567890")
                .rating(5.0)
                .build();

        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(partialUpdate);

        // Act
        UserProfile result = userProfileService.updateProfile(partialUpdate);

        // Assert
        assertThat(result.getFirstName()).isEqualTo("UpdatedName");
        assertThat(result.getRating()).isEqualTo(5.0);
        verify(userProfileRepository).save(partialUpdate);
    }

    @Test
    @DisplayName("Should update profile using DTO with PARTIAL data (skips nulls)")
    void updateProfile_FromDto_WithPartialData_UpdatesOnlyNotNullFields() {
        // Arrange
        UserProfile existingUser = UserProfile.builder()
                .userId(testUserId)
                .email("old@example.com")
                .firstName("OldName")
                .lastName("OldSurname")
                .phoneNumber("+905555555555")
                .rating(1.0)
                .build();

        UpdateUserProfileDto partialUpdateDto = UpdateUserProfileDto.builder()
                .firstName("NewName")
                .rating(5.0)
                .build();

        when(userProfileRepository.findById(testUserId)).thenReturn(Optional.of(existingUser));

        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserProfile result = userProfileService.updateProfile(partialUpdateDto, testUserId);

        // Assert
        assertThat(result.getFirstName()).isEqualTo("NewName");
        assertThat(result.getRating()).isEqualTo(5.0);

        assertThat(result.getLastName()).isEqualTo("OldSurname");
        assertThat(result.getEmail()).isEqualTo("old@example.com");
        assertThat(result.getPhoneNumber()).isEqualTo("+905555555555");

        verify(userProfileRepository).save(existingUser);
    }

    @Test
    @DisplayName("Should update profile using DTO with ALL fields")
    void updateProfile_FromDto_WithFullData_UpdatesAllFields() {
        // Arrange
        UserProfile existingUser = UserProfile.builder()
                .userId(testUserId)
                .email("old@example.com")
                .firstName("OldName")
                .build();

        UpdateUserProfileDto fullUpdateDto = UpdateUserProfileDto.builder()
                .firstName("NewName")
                .lastName("NewSurname")
                .email("new@example.com")
                .phoneNumber("+905555555555")
                .rating(4.0)
                .build();

        when(userProfileRepository.findById(testUserId)).thenReturn(Optional.of(existingUser));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserProfile result = userProfileService.updateProfile(fullUpdateDto, testUserId);

        // Assert
        assertThat(result.getFirstName()).isEqualTo("NewName");
        assertThat(result.getLastName()).isEqualTo("NewSurname");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getPhoneNumber()).isEqualTo("+905555555555");
        assertThat(result.getRating()).isEqualTo(4.0);
    }
}

