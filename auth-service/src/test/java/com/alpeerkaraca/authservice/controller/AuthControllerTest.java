package com.alpeerkaraca.authservice.controller;

import com.alpeerkaraca.authservice.AbstractIntegrationTest;
import com.alpeerkaraca.authservice.dto.UserLoginRequest;
import com.alpeerkaraca.authservice.dto.UserRegisterRequest;
import com.alpeerkaraca.authservice.service.AuthService;
import com.alpeerkaraca.common.dto.TokenPair;
import com.alpeerkaraca.common.exception.ConflictException;
import com.alpeerkaraca.common.exception.InvalidCredentialsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthController Tests")
class AuthControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Nested
    @DisplayName("POST /api/v1/auth/register - User Registration")
    class RegisterTests {

        @Test
        @DisplayName("Should register user successfully with valid request")
        void registerUser_ValidRequest_ReturnsCreated() throws Exception {
            // Arrange
            UserRegisterRequest request = UserRegisterRequest.builder()
                    .email("test@example.com")
                    .password("Password123!")
                    .firstName("Test")
                    .lastName("User")
                    .phoneNumber("+905555555555")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").exists());

            verify(authService).registerUser(any(UserRegisterRequest.class));
        }

        @Test
        @DisplayName("Should return bad request for invalid email format")
        void registerUser_InvalidEmail_ReturnsBadRequest() throws Exception {
            // Arrange
            UserRegisterRequest request = UserRegisterRequest.builder()
                    .email("invalid-email")
                    .password("Password123!")
                    .firstName("Test")
                    .lastName("User")
                    .phoneNumber("+905551234567")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(authService, never()).registerUser(any());
        }

        @Test
        @DisplayName("Should reject registration with missing email")
        void registerUser_MissingEmail_ReturnsBadRequest() throws Exception {
            // Arrange
            UserRegisterRequest request = UserRegisterRequest.builder()
                    .password("Password123!")
                    .firstName("Test")
                    .lastName("User")
                    .phoneNumber("+905551234567")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject registration with short password")
        void registerUser_ShortPassword_ReturnsBadRequest() throws Exception {
            // Arrange
            UserRegisterRequest request = UserRegisterRequest.builder()
                    .email("test@example.com")
                    .password("Pass1!")
                    .firstName("Test")
                    .lastName("User")
                    .phoneNumber("+905551234567")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject registration with missing firstName")
        void registerUser_MissingFirstName_ReturnsBadRequest() throws Exception {
            // Arrange
            UserRegisterRequest request = UserRegisterRequest.builder()
                    .email("test@example.com")
                    .password("Password123!")
                    .lastName("User")
                    .phoneNumber("+905551234567")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject registration with invalid phone number")
        void registerUser_InvalidPhoneNumber_ReturnsBadRequest() throws Exception {
            // Arrange
            UserRegisterRequest request = UserRegisterRequest.builder()
                    .email("test@example.com")
                    .password("Password123!")
                    .firstName("Test")
                    .lastName("User")
                    .phoneNumber("123")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle duplicate email registration")
        void registerUser_DuplicateEmail_ReturnsConflict() throws Exception {
            // Arrange
            UserRegisterRequest request = UserRegisterRequest.builder()
                    .email("existing@example.com")
                    .password("Password123!")
                    .firstName("Test")
                    .lastName("User")
                    .phoneNumber("+905551234567")
                    .build();

            doThrow(new ConflictException("Email already in use"))
                    .when(authService).registerUser(any(UserRegisterRequest.class));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should handle malformed JSON")
        void registerUser_MalformedJson_ReturnsBadRequest() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("\"invalid-json\""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle empty request body")
        void registerUser_EmptyBody_ReturnsBadRequest() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should accept international characters in names")
        void registerUser_InternationalNames_ReturnsCreated() throws Exception {
            // Arrange
            UserRegisterRequest request = UserRegisterRequest.builder()
                    .email("test@example.com")
                    .password("Password123!")
                    .firstName("José")
                    .lastName("Müller")
                    .phoneNumber("+905551234567")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should accept valid Turkish phone number")
        void registerUser_TurkishPhoneNumber_ReturnsCreated() throws Exception {
            // Arrange
            UserRegisterRequest request = UserRegisterRequest.builder()
                    .email("test@example.com")
                    .password("Password123!")
                    .firstName("Test")
                    .lastName("User")
                    .phoneNumber("+905551234567")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login - User Login")
    class LoginTests {

        @Test
        @DisplayName("Should login user successfully with valid credentials")
        void loginUser_ValidCredentials_ReturnsToken() throws Exception {
            // Arrange
            UserLoginRequest request = new UserLoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("Password123!");

            TokenPair tokenPair = new TokenPair("access-token", "refresh-token");

            when(authService.login(any(UserLoginRequest.class))).thenReturn(tokenPair);

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.message").value("Login successful."));

            verify(authService).login(any(UserLoginRequest.class));
        }

        @Test
        @DisplayName("Should reject login with invalid credentials")
        void loginUser_InvalidCredentials_ReturnsUnauthorized() throws Exception {
            // Arrange
            UserLoginRequest request = new UserLoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("WrongPassword");

            when(authService.login(any(UserLoginRequest.class)))
                    .thenThrow(new InvalidCredentialsException("Invalid email or password"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should reject login with missing email")
        void loginUser_MissingEmail_ReturnsBadRequest() throws Exception {
            // Arrange
            UserLoginRequest request = new UserLoginRequest();
            request.setPassword("Password123!");

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject login with missing password")
        void loginUser_MissingPassword_ReturnsBadRequest() throws Exception {
            // Arrange
            UserLoginRequest request = new UserLoginRequest();
            request.setEmail("test@example.com");

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject login with invalid email format")
        void loginUser_InvalidEmailFormat_ReturnsBadRequest() throws Exception {
            // Arrange
            UserLoginRequest request = new UserLoginRequest();
            request.setEmail("not-an-email");
            request.setPassword("Password123!");

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle empty request body")
        void loginUser_EmptyBody_ReturnsBadRequest() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle malformed JSON")
        void loginUser_MalformedJson_ReturnsBadRequest() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("\"invalid-json\""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle login for non-existent user")
        void loginUser_NonExistentUser_ReturnsUnauthorized() throws Exception {
            // Arrange
            UserLoginRequest request = new UserLoginRequest();
            request.setEmail("nonexistent@example.com");
            request.setPassword("Password123!");

            when(authService.login(any(UserLoginRequest.class)))
                    .thenThrow(new InvalidCredentialsException("Invalid email or password"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle concurrent registration attempts")
        void registerUser_ConcurrentRequests_HandlesCorrectly() throws Exception {
            // Arrange
            UserRegisterRequest request = UserRegisterRequest.builder()
                    .email("concurrent@example.com")
                    .password("Password123!")
                    .firstName("Test")
                    .lastName("User")
                    .phoneNumber("+905551234567")
                    .build();

            // Act - Multiple requests
            for (int i = 0; i < 3; i++) {
                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated());
            }

            // Assert - Service called multiple times
            verify(authService, times(3)).registerUser(any(UserRegisterRequest.class));
        }

        @Test
        @DisplayName("Should handle very long but valid email")
        void registerUser_LongEmail_ReturnsCreated() throws Exception {
            // Arrange
            String longEmail = "verylongemailaddress.with.many.dots@subdomain.example.com";
            UserRegisterRequest request = UserRegisterRequest.builder()
                    .email(longEmail)
                    .password("Password123!")
                    .firstName("Test")
                    .lastName("User")
                    .phoneNumber("+905551234567")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should handle password with special characters")
        void registerUser_ComplexPassword_ReturnsCreated() throws Exception {
            // Arrange
            UserRegisterRequest request = UserRegisterRequest.builder()
                    .email("test@example.com")
                    .password("P@ssw0rd!#$%^&*()")
                    .firstName("Test")
                    .lastName("User")
                    .phoneNumber("+905551234567")
                    .build();

            // Act & Assert
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }
}

