package com.alpeerkaraca.common.security;

import com.alpeerkaraca.common.dto.TokenPair;
import com.alpeerkaraca.common.exception.TokenGenerationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

/**
 * Central service responsible for JSON Web Token (JWT) management.
 * <p>
 * This service handles token generation (Access & Refresh), token validation,
 * and extraction of data (Claims) from tokens. It is part of the {@code karga-common}
 * module and is shared across all microservices.
 * </p>
 */
@Service
@Slf4j
public class JWTService {

    @Value("${app.jwt.expiration}")
    private long jwtExpirationMs;
    @Value("${app.jwt.refresh-expiration}")
    private long refreshTokenExpirationMs;
    private final KeyLoader keyLoader;

    public JWTService(KeyLoader keyLoader) {
        this.keyLoader = keyLoader;
    }

    /**
     * Generates an Access and Refresh token pair for an authenticated user.
     * <p>
     * The Access Token includes {@code userId} and {@code roles} as claims.
     * The Refresh Token includes only the {@code tokenType=refresh} claim.
     * </p>
     *
     * @param authentication The authentication object provided by Spring Security.
     * @param userId         The unique UUID of the user in the system (embedded in the token).
     * @return A {@link TokenPair} object containing the generated accessToken and refreshToken.
     */
    public TokenPair generateTokenPair(Authentication authentication, UUID userId) {
        try {
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", userId.toString());
            claims.put("roles", roles);

            String username = authentication.getName();

            String accessToken = generateToken(claims, username, jwtExpirationMs);
            String refreshToken = generateToken(Map.of("tokenType", "refresh"), username, refreshTokenExpirationMs);

            return new TokenPair(accessToken, refreshToken);
        } catch (Exception e) {
            throw new TokenGenerationException("Error while generating token pair", e);
        }
    }

    /**
     * Validates the token by checking its signature and expiration.
     *
     * @param token The JWT string to validate.
     * @return {@code true} if the token is valid, {@code false} if expired or signature is invalid.
     */
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error while JWT validation due to: {}", e.getMessage());

            return false;
        }
    }

    /**
     * Extracts the 'roles' information from the token as a list.
     *
     * @param token The JWT to parse.
     * @return A list of user roles (e.g., ["ROLE_PASSENGER", "ROLE_DRIVER"]).
     */
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> claims.get("roles", List.class));
    }

    /**
     * Extracts the custom 'userId' claim from the token.
     *
     * @param token The JWT to parse.
     * @return The user's UUID as a String.
     */
    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    /**
     * Extracts the 'Subject' (usually email/username) from the token.
     *
     * @param token The JWT to parse.
     * @return The username (email).
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Generic helper method to extract a specific Claim from the token.
     *
     * @param token          The JWT to inspect.
     * @param claimsResolver The function to process the claims.
     * @param <T>            The return type.
     * @return The value of the desired Claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Checks if the provided token is a Refresh Token.
     * <p>
     * Refresh tokens are created with a {@code tokenType: "refresh"} claim in the payload.
     * This method verifies the existence and value of this claim.
     * </p>
     *
     * @param token The JWT to check.
     * @return {@code true} if the token type is 'refresh', otherwise {@code false}.
     */
    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(extractAllClaims(token).get("tokenType"));
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(keyLoader.loadPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();


    }

    public String generateToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(keyLoader.loadPrivateKey())
                .compact();
    }

}