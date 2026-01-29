package com.alpeerkaraca.common.security;

import com.alpeerkaraca.common.dto.TokenPair;
import com.alpeerkaraca.common.exception.TokenGenerationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.function.Function;

/**
 * Central service responsible for JSON Web Token (JWT) management.
 *
 * <p>
 * This service handles token generation (Access & Refresh), token validation,
 * and extraction of data (Claims) from tokens. It is part of the {@code karga-common}
 * module and is shared across all microservices.
 * </p>
 */
@Service
@Slf4j
public class JWTService {

    /**
     * Claim key used to store the user identifier (UUID as string).
     */
    private static final String CLAIM_USER_ID = "userId";

    /**
     * Claim key used to store the roles assigned to the user.
     */
    private static final String CLAIM_ROLES = "roles";

    /**
     * Claim key used to indicate the token type (access or refresh).
     */
    private static final String CLAIM_TOKEN_TYPE = "tokenType";

    /**
     * Claim value representing a refresh token.
     */
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    /**
     * Claim value representing an access token.
     */
    private static final String TOKEN_TYPE_ACCESS = "access";

    /**
     * RSA private key used to sign JWTs.
     */
    private final RSAPrivateKey privateKey;

    /**
     * RSA public key used to verify JWT signatures.
     */
    private final RSAPublicKey publicKey;

    /**
     * Expiration time for access tokens in milliseconds.
     * Injected from application properties: {@code app.jwt.expiration}.
     */
    @Value("${app.jwt.expiration}")
    private long jwtExpirationMs;

    /**
     * Expiration time for refresh tokens in milliseconds.
     * Injected from application properties: {@code app.jwt.refresh-expiration}.
     */
    @Value("${app.jwt.refresh-expiration}")
    private long refreshTokenExpirationMs;

    /**
     * Constructs the JWTService with the RSA key pair.
     *
     * @param privateKey RSA private key used to sign tokens.
     * @param publicKey  RSA public key used to verify tokens.
     */
    public JWTService(
            @Autowired(required = false) RSAPrivateKey privateKey,
            RSAPublicKey publicKey) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    /**
     * Generates an Access and Refresh token pair for an authenticated user.
     *
     * <p>
     * The Access Token includes {@code userId} and {@code roles} as claims.
     * The Refresh Token includes only the {@code tokenType=refresh} claim.
     * </p>
     *
     * @param authentication The authentication object provided by Spring Security.
     * @param userId         The unique UUID of the user in the system (embedded in the token).
     * @return A {@link TokenPair} object containing the generated accessToken and refreshToken.
     * @throws TokenGenerationException if any error occurs during token creation.
     */
    public TokenPair generateTokenPair(Authentication authentication, UUID userId) {
        try {
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            Map<String, Object> accessClaims = new HashMap<>();
            accessClaims.put(CLAIM_USER_ID, userId.toString());
            accessClaims.put(CLAIM_ROLES, roles);
            accessClaims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);

            String username = authentication.getName();

            String accessToken = generateToken(accessClaims, username, jwtExpirationMs);

            Map<String, Object> refreshClaims = new HashMap<>();
            refreshClaims.put(CLAIM_USER_ID, userId.toString());
            refreshClaims.put(CLAIM_ROLES, roles);
            refreshClaims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);

            String refreshToken = generateToken(refreshClaims, username, refreshTokenExpirationMs);

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
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT Token: {}", e.getMessage());
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
        return extractClaim(token, claims -> claims.get(CLAIM_ROLES, List.class));
    }

    /**
     * Extracts the custom 'userId' claim from the token.
     *
     * @param token The JWT to parse.
     * @return The user's UUID as a String.
     */
    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_USER_ID, String.class));
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

    /**
     * Checks if the provided token is a Refresh Token.
     *
     * <p>
     * Refresh tokens are created with a {@code tokenType: "refresh"} claim in the payload.
     * This method verifies the existence and value of this claim.
     * </p>
     *
     * @param token The JWT to check.
     * @return {@code true} if the token type is 'refresh', otherwise {@code false'}.
     */
    public boolean isRefreshToken(String token) {
        try {
            return TOKEN_TYPE_REFRESH.equals(extractClaim(token, claims -> claims.get(CLAIM_TOKEN_TYPE, String.class)));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parses the provided JWT and returns all claims after verifying the signature with the configured public key.
     *
     * <p>
     * This method builds a JWT parser configured with the RSA public key and parses the signed claims payload.
     * It will throw a {@link io.jsonwebtoken.JwtException} for invalid signatures or malformed tokens.
     * </p>
     *
     * @param token The JWT string to parse.
     * @return The {@link Claims} extracted from the token.
     * @throws JwtException             if token signature is invalid or token is malformed.
     * @throws IllegalArgumentException if token is null or empty.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Generates a signed JWT with the provided extra claims, subject, and expiration.
     *
     * <p>
     * The token will contain:
     * - provided {@code extraClaims} as payload claims
     * - {@code subject} as the JWT subject
     * - a random {@code id}
     * - issued at and expiration timestamps
     * The token is signed using the configured RSA private key with RS256.
     * </p>
     *
     * @param extraClaims  Additional claims to include in the token payload.
     * @param subject      The subject (typically username/email) for the token.
     * @param expirationMs Expiration duration in milliseconds from now.
     * @return The compact serialized JWT string.
     */
    public String generateToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }
}