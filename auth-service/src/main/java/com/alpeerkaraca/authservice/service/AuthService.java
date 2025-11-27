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
        publishUserCreatedEvent(
                savedUser.getUserId(), savedUser.getEmail(),
                request.getFirstName(), request.getLastName(),
                request.getPhoneNumber()
        );
        return savedUser;
    }

    public TokenPair login(@Valid UserLoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new InvalidCredentialsException("Email or password is incorrect"));
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

    public TokenPair refreshToken(@Valid RefreshTokenRequest request) {
        String token = request.refreshToken();
        if (!jwtService.isRefreshToken(token)) {
            throw new InvalidTokenException("Credentials could not be verified. Please login again.");
        }
        String username = jwtService.extractUsername(token);
        if (username == null) {
            throw new InvalidTokenException("Credentials could not be verified. Please login again.");
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return jwtService.generateTokenPair(authentication, userRepository.findByEmail(username).get().getUserId());
    }

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
                .rating(0.0)
                .build();
        kafkaTemplate.send(USER_CREATED_TOPIC, userRegisterMessage);
    }
}
