package com.alpeerkaraca.authservice.integration;

import com.alpeerkaraca.authservice.AbstractIntegrationTest;
import com.alpeerkaraca.authservice.dto.UserLoginRequest;
import com.alpeerkaraca.authservice.dto.UserRegisterRequest;
import com.alpeerkaraca.authservice.model.Role;
import com.alpeerkaraca.authservice.model.User;
import com.alpeerkaraca.authservice.repository.UserRepository;
import com.alpeerkaraca.authservice.service.AuthService;
import com.alpeerkaraca.common.dto.TokenPair;
import com.alpeerkaraca.common.exception.ConflictException;
import com.alpeerkaraca.common.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthService Integration Tests")
class AuthServiceIT extends AbstractIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should register and login user successfully - Full flow")
    void registerAndLogin_Success() {
        // Arrange
        UserRegisterRequest registerRequest = UserRegisterRequest.builder()
                .email("integration@test.com")
                .password("Password123!")
                .firstName("Integration")
                .lastName("Test")
                .phoneNumber("+905551234567")
                .build();

        // Act - Register
        authService.registerUser(registerRequest);

        // Assert - User created in database
        User savedUser = userRepository.findByEmail("integration@test.com").orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("integration@test.com");
        assertThat(savedUser.getRole()).isEqualTo(Role.PASSENGER);
        assertThat(savedUser.isActive()).isFalse();

        // Act - Login
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setEmail("integration@test.com");
        loginRequest.setPassword("Password123!");

        TokenPair tokenPair = authService.login(loginRequest);

        // Assert - Token received
        assertThat(tokenPair).isNotNull();
        assertThat(tokenPair.getAccessToken()).isNotBlank();
        assertThat(tokenPair.getRefreshToken()).isNotBlank();
    }

    @Test
    @DisplayName("Should persist user with encoded password")
    void registerUser_PasswordEncoded() {
        // Arrange
        UserRegisterRequest request = UserRegisterRequest.builder()
                .email("security@test.com")
                .password("PlainPassword123!")
                .firstName("Security")
                .lastName("Test")
                .phoneNumber("+905551234567")
                .build();

        // Act
        authService.registerUser(request);

        // Assert
        User savedUser = userRepository.findByEmail("security@test.com").orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getPassword()).isNotEqualTo("PlainPassword123!");
        assertThat(savedUser.getPassword()).startsWith("$2a$"); // BCrypt hash
    }

    @Test
    @DisplayName("Should throw ConflictException for duplicate email")
    void registerUser_DuplicateEmail_ThrowsConflict() {
        // Arrange
        UserRegisterRequest request1 = UserRegisterRequest.builder()
                .email("duplicate@test.com")
                .password("Password123!")
                .firstName("First")
                .lastName("User")
                .phoneNumber("+905551234567")
                .build();

        UserRegisterRequest request2 = UserRegisterRequest.builder()
                .email("duplicate@test.com")
                .password("DifferentPass123!")
                .firstName("Second")
                .lastName("User")
                .phoneNumber("+905559876543")
                .build();

        // Act
        authService.registerUser(request1);

        // Assert
        assertThatThrownBy(() -> authService.registerUser(request2))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already in use");
    }

    @Test
    @DisplayName("Should reject login with wrong password")
    void login_WrongPassword_ThrowsException() {
        // Arrange
        UserRegisterRequest registerRequest = UserRegisterRequest.builder()
                .email("wrongpass@test.com")
                .password("CorrectPassword123!")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("+905551234567")
                .build();
        authService.registerUser(registerRequest);

        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setEmail("wrongpass@test.com");
        loginRequest.setPassword("WrongPassword123!");

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Should reject login for non-existent user")
    void login_NonExistentUser_ThrowsException() {
        // Arrange
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setEmail("nonexistent@test.com");
        loginRequest.setPassword("Password123!");

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Should create user with PASSENGER role by default")
    void registerUser_DefaultRole_IsPassenger() {
        // Arrange
        UserRegisterRequest request = UserRegisterRequest.builder()
                .email("role@test.com")
                .password("Password123!")
                .firstName("Role")
                .lastName("Test")
                .phoneNumber("+905551234567")
                .build();

        // Act
        authService.registerUser(request);

        // Assert
        User savedUser = userRepository.findByEmail("role@test.com").orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getRole()).isEqualTo(Role.PASSENGER);
    }

    @Test
    @DisplayName("Should create user as inactive by default")
    void registerUser_DefaultStatus_IsInactive() {
        // Arrange
        UserRegisterRequest request = UserRegisterRequest.builder()
                .email("inactive@test.com")
                .password("Password123!")
                .firstName("Inactive")
                .lastName("Test")
                .phoneNumber("+905551234567")
                .build();

        // Act
        authService.registerUser(request);

        // Assert
        User savedUser = userRepository.findByEmail("inactive@test.com").orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should handle multiple user registrations")
    void registerMultipleUsers_AllPersisted() {
        // Arrange & Act
        for (int i = 1; i <= 5; i++) {
            UserRegisterRequest request = UserRegisterRequest.builder()
                    .email("user" + i + "@test.com")
                    .password("Password123!")
                    .firstName("User")
                    .lastName("" + i)
                    .phoneNumber("+90555123456" + i)
                    .build();
            authService.registerUser(request);
        }

        // Assert
        List<User> allUsers = userRepository.findAll();
        assertThat(allUsers).hasSize(5);
    }

    @Test
    @DisplayName("Should preserve user data integrity")
    void registerUser_DataIntegrity_Preserved() {
        // Arrange
        UserRegisterRequest request = UserRegisterRequest.builder()
                .email("integrity@test.com")
                .password("Password123!")
                .firstName("Data")
                .lastName("Integrity")
                .phoneNumber("+905551234567")
                .build();

        // Act
        User registered = authService.registerUser(request);

        // Assert
        User fetched = userRepository.findById(registered.getUserId()).orElse(null);
        assertThat(fetched).isNotNull();
        assertThat(fetched.getEmail()).isEqualTo("integrity@test.com");
        assertThat(fetched.getUserId()).isEqualTo(registered.getUserId());
    }

    @Test
    @DisplayName("Should generate unique user IDs")
    void registerMultipleUsers_UniqueUserIds() {
        // Arrange & Act
        UserRegisterRequest request1 = UserRegisterRequest.builder()
                .email("unique1@test.com")
                .password("Password123!")
                .firstName("User")
                .lastName("One")
                .phoneNumber("+905551234561")
                .build();

        UserRegisterRequest request2 = UserRegisterRequest.builder()
                .email("unique2@test.com")
                .password("Password123!")
                .firstName("User")
                .lastName("Two")
                .phoneNumber("+905551234562")
                .build();

        User user1 = authService.registerUser(request1);
        User user2 = authService.registerUser(request2);

        // Assert
        assertThat(user1.getUserId()).isNotEqualTo(user2.getUserId());
    }

    @Test
    @DisplayName("Should persist international characters in names")
    void registerUser_InternationalCharacters_Persisted() {
        // Arrange
        UserRegisterRequest request = UserRegisterRequest.builder()
                .email("international@test.com")
                .password("Password123!")
                .firstName("Müller")
                .lastName("Özdemir")
                .phoneNumber("+905551234567")
                .build();

        // Act
        authService.registerUser(request);

        // Assert
        User savedUser = userRepository.findByEmail("international@test.com").orElse(null);
        assertThat(savedUser).isNotNull();
    }

    @Test
    @DisplayName("Should find user by email case-insensitively")
    void findByEmail_CaseInsensitive_ReturnsUser() {
        // Arrange
        UserRegisterRequest request = UserRegisterRequest.builder()
                .email("CaseSensitive@Test.COM")
                .password("Password123!")
                .firstName("Case")
                .lastName("Test")
                .phoneNumber("+905551234567")
                .build();
        authService.registerUser(request);

        // Act
        User found = userRepository.findByEmail("casesensitive@test.com").orElse(null);

        // Assert - Depending on database configuration
        assertThat(found).isNotNull();
    }
}


