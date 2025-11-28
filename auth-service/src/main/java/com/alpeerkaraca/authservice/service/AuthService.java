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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service managing user authentication and registration processes.
 * <p>
 * This service is responsible for registering new users, handling logins,
 * refreshing tokens, and publishing post-registration Kafka events.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class AuthService {
    private static final String USER_CREATED_TOPIC = "user_created";
    private final UserRepository userRepository;
    private final JWTService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final KafkaTemplate<String, UserRegisterMessage> kafkaTemplate;

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
    public User registerUser(@Valid UserRegisterRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            throw new ConflictException("Email already in use");
        });
        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.PASSENGER)
                .isActive(false)
                .build();
        var savedUser = userRepository.save(user);

        // Publish Kafka event asynchronously
        publishUserCreatedEvent(
                savedUser.getUserId(), savedUser.getEmail(),
                request.getFirstName(), request.getLastName(),
                request.getPhoneNumber()
        );
        return savedUser;
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
        return jwtService.generateTokenPair(authentication, userRepository.findByEmail(username).get().getUserId());
    }

    /**
     * Publishes a message to the Kafka topic upon successful user registration.
     *
     * @param userId      The user's UUID.
     * @param email       User's email address.
     * @param firstName   First name.
     * @param lastName    Last name.
     * @param phoneNumber Phone number.
     */
    public void publishUserCreatedEvent(
            UUID userId, String email,
            String firstName, String lastName,
            String phoneNumber
    ) {
        UserRegisterMessage userRegisterMessage = UserRegisterMessage.builder()
                .id(userId.toString())
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phoneNumber)
                .rating(0.0) // Default rating for new user
                .build();
        kafkaTemplate.send(USER_CREATED_TOPIC, userRegisterMessage);
    }
}