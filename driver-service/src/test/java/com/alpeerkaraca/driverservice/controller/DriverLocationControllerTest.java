package com.alpeerkaraca.driverservice.controller;

import com.alpeerkaraca.driverservice.AbstractIntegrationTest;
import com.alpeerkaraca.driverservice.dto.LocationUpdateRequest;
import com.alpeerkaraca.driverservice.repository.DriverRepository;
import com.alpeerkaraca.driverservice.service.DriverLocationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DriverLocationController Tests")
class DriverLocationControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DriverLocationService driverLocationService;

    @MockitoBean
    private DriverRepository driverRepository;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    @Nested
    @DisplayName("POST /api/v1/drivers/location - Update Location")
    class UpdateLocationTests {

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should update driver location successfully when authenticated")
        void updateLocation_WithValidRequest_ReturnsAccepted() throws Exception {
            // Arrange
            LocationUpdateRequest request = new LocationUpdateRequest(40.7128, -74.0060);
            doNothing().when(driverLocationService).publishDriverLocationMessage(any(UUID.class), anyDouble(), anyDouble());

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/location")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isAccepted());

            verify(driverLocationService).publishDriverLocationMessage(any(UUID.class), eq(40.7128), eq(-74.0060));
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should handle zero coordinates (equator/prime meridian)")
        void updateLocation_WithZeroCoordinates_ReturnsAccepted() throws Exception {
            // Arrange
            LocationUpdateRequest request = new LocationUpdateRequest(0.0, 0.0);
            doNothing().when(driverLocationService).publishDriverLocationMessage(any(UUID.class), anyDouble(), anyDouble());

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/location")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted());

            verify(driverLocationService).publishDriverLocationMessage(any(UUID.class), eq(0.0), eq(0.0));
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should handle maximum valid latitude (90)")
        void updateLocation_WithMaxLatitude_ReturnsAccepted() throws Exception {
            // Arrange
            LocationUpdateRequest request = new LocationUpdateRequest(90.0, 0.0);
            doNothing().when(driverLocationService).publishDriverLocationMessage(any(UUID.class), anyDouble(), anyDouble());

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/location")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted());
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should handle minimum valid latitude (-90)")
        void updateLocation_WithMinLatitude_ReturnsAccepted() throws Exception {
            // Arrange
            LocationUpdateRequest request = new LocationUpdateRequest(-90.0, 0.0);
            doNothing().when(driverLocationService).publishDriverLocationMessage(any(UUID.class), anyDouble(), anyDouble());

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/location")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted());
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should handle maximum valid longitude (180)")
        void updateLocation_WithMaxLongitude_ReturnsAccepted() throws Exception {
            // Arrange
            LocationUpdateRequest request = new LocationUpdateRequest(0.0, 180.0);
            doNothing().when(driverLocationService).publishDriverLocationMessage(any(UUID.class), anyDouble(), anyDouble());

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/location")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted());
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should handle minimum valid longitude (-180)")
        void updateLocation_WithMinLongitude_ReturnsAccepted() throws Exception {
            // Arrange
            LocationUpdateRequest request = new LocationUpdateRequest(0.0, -180.0);
            doNothing().when(driverLocationService).publishDriverLocationMessage(any(UUID.class), anyDouble(), anyDouble());

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/location")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should reject invalid latitude (> 90)")
        void updateLocation_WithInvalidLatitude_ReturnsBadRequest() throws Exception {
            // Arrange
            LocationUpdateRequest request = new LocationUpdateRequest(91.0, -74.0060);

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/location")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should reject invalid latitude (< -90)")
        void updateLocation_WithNegativeInvalidLatitude_ReturnsBadRequest() throws Exception {
            // Arrange
            LocationUpdateRequest request = new LocationUpdateRequest(-91.0, -74.0060);

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/location")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should reject invalid longitude (> 180)")
        void updateLocation_WithInvalidLongitude_ReturnsBadRequest() throws Exception {
            // Arrange
            LocationUpdateRequest request = new LocationUpdateRequest(40.7128, 181.0);

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/location")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should reject invalid longitude (< -180)")
        void updateLocation_WithNegativeInvalidLongitude_ReturnsBadRequest() throws Exception {
            // Arrange
            LocationUpdateRequest request = new LocationUpdateRequest(40.7128, -181.0);

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/location")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should reject empty request body")
        void updateLocation_WithEmptyBody_ReturnsBadRequest() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/location")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should reject malformed JSON")
        void updateLocation_WithMalformedJson_ReturnsBadRequest() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/location")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("invalid-json"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should return unauthorized when user is not authenticated")
        void updateLocation_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
            // Arrange
            LocationUpdateRequest request = new LocationUpdateRequest(40.7128, -74.0060);

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/location")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());

            verify(driverLocationService, never()).publishDriverLocationMessage(any(), anyDouble(), anyDouble());
        }

        @Test
        @WithMockUser(username = "invalid-uuid", roles = {"DRIVER"})
        @DisplayName("Should handle invalid UUID format")
        void updateLocation_WithInvalidUUID_ReturnsError() throws Exception {
            // Arrange
            LocationUpdateRequest request = new LocationUpdateRequest(40.7128, -74.0060);

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/location")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should handle precise decimal coordinates")
        void updateLocation_WithPreciseCoordinates_ReturnsAccepted() throws Exception {
            // Arrange
            LocationUpdateRequest request = new LocationUpdateRequest(40.712776, -74.005974);
            doNothing().when(driverLocationService).publishDriverLocationMessage(any(UUID.class), anyDouble(), anyDouble());

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/location")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted());
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should handle service exception gracefully")
        void updateLocation_ServiceThrowsException_ReturnsError() throws Exception {
            // Arrange
            LocationUpdateRequest request = new LocationUpdateRequest(40.7128, -74.0060);
            doThrow(new RuntimeException("Kafka service unavailable"))
                    .when(driverLocationService).publishDriverLocationMessage(any(UUID.class), anyDouble(), anyDouble());

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/location")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is5xxServerError());
        }
    }
}

