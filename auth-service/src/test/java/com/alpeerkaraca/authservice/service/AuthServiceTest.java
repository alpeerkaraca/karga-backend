package com.alpeerkaraca.authservice.service;

import com.alpeerkaraca.authservice.dto.UserLoginRequest;
import com.alpeerkaraca.authservice.dto.UserRegisterMessage;
import com.alpeerkaraca.authservice.dto.UserRegisterRequest;
import com.alpeerkaraca.authservice.model.Role;
import com.alpeerkaraca.authservice.model.User;
import com.alpeerkaraca.authservice.repository.UserRepository;
import com.alpeerkaraca.common.dto.RefreshTokenRequest;
import com.alpeerkaraca.common.dto.TokenPair;
import com.alpeerkaraca.common.exception.ConflictException;
import com.alpeerkaraca.common.exception.InvalidCredentialsException;
import com.alpeerkaraca.common.exception.InvalidTokenException;
import com.alpeerkaraca.common.security.JWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JWTService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private KafkaTemplate<String, UserRegisterMessage> kafkaTemplate;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private UserRegisterRequest registerRequest;
    private UserLoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(UUID.randomUUID())
                .email("test@example.com")
                .password("encodedPassword")
                .role(Role.PASSENGER)
                .isActive(false)
                .build();

        registerRequest = UserRegisterRequest.builder()
                .email("test@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1234567890123")
                .build();

        loginRequest = new UserLoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");
    }

    @Test
    @DisplayName("Should register new user successfully")
    void registerUser_NewUser_Success() {
        // Arrange
        when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = authService.registerUser(registerRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(registerRequest.getEmail());
        assertThat(result.getRole()).isEqualTo(Role.PASSENGER);
        assertThat(result.isActive()).isFalse();

        verify(userRepository).findByEmail(registerRequest.getEmail());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(any(User.class));
        verify(kafkaTemplate).send(eq("user_created"), any(UserRegisterMessage.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when email already exists")
    void registerUser_EmailExists_ThrowsConflictException() {
        // Arrange
        when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> authService.registerUser(registerRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already in use");

        verify(userRepository).findByEmail(registerRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    @DisplayName("Should login user successfully with valid credentials")
    void login_ValidCredentials_Success() {
        // Arrange
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                loginRequest.getEmail(),
                loginRequest.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_PASSENGER"))
        );
        TokenPair expectedTokenPair = new TokenPair("accessToken", "refreshToken");

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateTokenPair(authentication, testUser.getUserId())).thenReturn(expectedTokenPair);

        // Act
        TokenPair result = authService.login(loginRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("accessToken");
        assertThat(result.getRefreshToken()).isEqualTo("refreshToken");
        assertThat(result.getAccessToken()).isEqualTo("accessToken");
        assertThat(result.getRefreshToken()).isEqualTo("refreshToken");
        verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPassword());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateTokenPair(authentication, testUser.getUserId());
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when user not found during login")
    void login_UserNotFound_ThrowsInvalidCredentialsException() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Email or password is incorrect");

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when password is incorrect")
    void login_IncorrectPassword_ThrowsInvalidCredentialsException() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Email or password is incorrect");

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPassword());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("Should refresh token successfully with valid refresh token")
    void refreshToken_ValidToken_Success() {
        // Arrange
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest("validRefreshToken");
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(testUser.getEmail())
                .password(testUser.getPassword())
                .authorities("ROLE_PASSENGER")
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        TokenPair expectedTokenPair = new TokenPair("newAccessToken", "newRefreshToken");

        when(jwtService.isRefreshToken(refreshTokenRequest.refreshToken())).thenReturn(true);
        when(jwtService.extractUsername(refreshTokenRequest.refreshToken())).thenReturn(testUser.getEmail());
        when(userDetailsService.loadUserByUsername(testUser.getEmail())).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(jwtService.generateTokenPair(authentication, testUser.getUserId())).thenReturn(expectedTokenPair);

        // Act
        TokenPair result = authService.refreshToken(refreshTokenRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("newAccessToken");
        assertThat(result.getRefreshToken()).isEqualTo("newRefreshToken");
        assertThat(result.getAccessToken()).isEqualTo("newAccessToken");
        assertThat(result.getRefreshToken()).isEqualTo("newRefreshToken");
        assertThat(result.getAccessToken()).isEqualTo("newAccessToken");
        assertThat(result.getRefreshToken()).isEqualTo("newRefreshToken");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Should throw InvalidTokenException when token is not a refresh token")
    void refreshToken_NotRefreshToken_ThrowsInvalidTokenException() {
        // Arrange
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest("accessToken");
        when(jwtService.isRefreshToken(refreshTokenRequest.refreshToken())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(refreshTokenRequest))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Credentials could not be verified");

        verify(jwtService).isRefreshToken(refreshTokenRequest.refreshToken());
        verify(jwtService, never()).extractUsername(anyString());
    }

    @Test
    @DisplayName("Should throw InvalidTokenException when username cannot be extracted from token")
    void refreshToken_NullUsername_ThrowsInvalidTokenException() {
        // Arrange
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest("validRefreshToken");
        when(jwtService.isRefreshToken(refreshTokenRequest.refreshToken())).thenReturn(true);
        when(jwtService.extractUsername(refreshTokenRequest.refreshToken())).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(refreshTokenRequest))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Credentials could not be verified");

        verify(jwtService).isRefreshToken(refreshTokenRequest.refreshToken());
        verify(jwtService).extractUsername(refreshTokenRequest.refreshToken());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    @DisplayName("Should publish user created event with correct data")
    void publishUserCreatedEvent_ValidData_Success() {
        // Arrange
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String firstName = "John";
        String lastName = "Doe";
        String phoneNumber = "+1234567890";

        // Act
        authService.publishUserCreatedEvent(userId, email, firstName, lastName, phoneNumber);

        // Assert
        verify(kafkaTemplate).send(eq("user_created"), argThat(message ->
                message.getId().equals(userId.toString()) &&
                        message.getEmail().equals(email) &&
                        message.getFirstName().equals(firstName) &&
                        message.getLastName().equals(lastName) &&
                        message.getPhoneNumber().equals(phoneNumber) &&
                        message.getRating() == 0.0
        ));
    }
}

