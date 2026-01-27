package com.alpeerkaraca.authservice.service;

import com.alpeerkaraca.authservice.dto.UserLoginRequest;
import com.alpeerkaraca.authservice.dto.UserRegisterMessage;
import com.alpeerkaraca.authservice.dto.UserRegisterRequest;
import com.alpeerkaraca.authservice.model.AuthOutbox;
import com.alpeerkaraca.authservice.model.Role;
import com.alpeerkaraca.authservice.model.User;
import com.alpeerkaraca.authservice.repository.AuthOutboxRepository;
import com.alpeerkaraca.authservice.repository.UserRepository;
import com.alpeerkaraca.common.dto.RefreshTokenRequest;
import com.alpeerkaraca.common.dto.TokenPair;
import com.alpeerkaraca.common.exception.*;
import com.alpeerkaraca.common.security.JWTService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service managing user authentication and registration processes.
 * <p>
 * This service is responsible for registering new users, handling logins,
 * refreshing tokens, and publishing post-registration Kafka events.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JWTService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final AuthOutboxRepository authOutboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Registers a new user and publishes the 'user_created' Kafka event.
     * <p>
     * By default, the user role is set to {@link Role#PASSENGER} and status is inactive (isActive=false).
     * If registration is successful, a Kafka message is sent to notify other services like {@code user-service}.
     * </p>
     *
     * @param request DTO containing the registration details.
     * @return The saved {@link User} entity.
     * @throws ConflictException If the provided email address is already registered.
     */
    @Transactional
    public User registerUser(@Valid UserRegisterRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            throw new ConflictException("Email already in use");
        });
        try {
            String encodedPassword = passwordEncoder.encode(request.getPassword());
            User user = User.builder()
                    .email(request.getEmail().toLowerCase())
                    .password(encodedPassword)
                    .role(Role.PASSENGER)
                    .isActive(false)
                    .build();
            final User savedUser = userRepository.save(user);

            UserRegisterMessage userRegisterMessage = UserRegisterMessage.builder()
                    .id(savedUser.getUserId().toString())
                    .email(savedUser.getEmail())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phoneNumber(request.getPhoneNumber())
                    .rating(0.0)
                    .build();

            final AuthOutbox outboxEvent = new AuthOutbox();
            outboxEvent.setAggregateId(savedUser.getUserId().toString());
            outboxEvent.setEventType("UserCreated");
            outboxEvent.setPayload(objectMapper.writeValueAsString(userRegisterMessage));
            authOutboxRepository.save(outboxEvent);

            log.info("Saga Initiated: User {} created (inactive). Outbox event saved.", savedUser.getUserId());
            return savedUser;
        } catch (Exception e) {
            log.error("Error during user registration: {}", e.getMessage());
            throw new UserRegistrationException("An error occurred during user registration. Please try again later.");
        }
    }

    @Transactional
    public void activateUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(true);
        userRepository.save(user);
        log.info("User {} activated successfully.", userId);
    }

    @Transactional
    public void rollbackUser(UUID userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
            log.info("User {} rolled back (deleted) successfully.", userId);
        } else {
            log.warn("Rollback attempted for non-existing user {}", userId);
        }
    }

    /**
     * Authenticates a user and returns a JWT Access/Refresh token pair.
     *
     * @param request Request containing email and password.
     * @return The generated {@link TokenPair}.
     * @throws InvalidCredentialsException If email or password is incorrect.
     */
    public TokenPair login(@Valid UserLoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Email or password is incorrect"));

        if (!user.isActive()) {
            throw new InvalidCredentialsException("Account is not active. Please complete registration.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Email or password is incorrect");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        return jwtService.generateTokenPair(authentication, user.getUserId());
    }

    /**
     * Generates a new Access Token using a valid Refresh Token.
     *
     * @param request Request containing the refresh token.
     * @return A new {@link TokenPair}.
     * @throws InvalidTokenException If the token is invalid, expired, or not a refresh token.
     */
    public TokenPair refreshToken(@Valid RefreshTokenRequest request) {
        String token = request.refreshToken();

        if (!jwtService.isRefreshToken(token)) {
            throw new InvalidTokenException("Credentials could not be verified. Please login again.");
        }

        String username = jwtService.extractUsername(token);
        if (username == null) {
            throw new InvalidTokenException("Credentials could not be verified. Please login again.");
        }

        // Load user details and refresh permissions
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Fetching User ID again incurs DB cost but is necessary for token consistency.
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new InvalidTokenException("Credentials could not be verified. Please login again."));
        return jwtService.generateTokenPair(authentication, user.getUserId());
    }

}