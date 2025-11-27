package com.alpeerkaraca.driverservice.controller;

import com.alpeerkaraca.common.exception.InvalidStatusException;
import com.alpeerkaraca.driverservice.AbstractIntegrationTest;
import com.alpeerkaraca.driverservice.dto.DriverUpdateStatus;
import com.alpeerkaraca.driverservice.model.DriverStatus;
import com.alpeerkaraca.driverservice.service.DriverStatusService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DriverStatusController Tests")
class DriverStatusControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DriverStatusService driverStatusService;

    @Nested
    @DisplayName("POST /api/v1/drivers/status - Status Updates")
    class StatusUpdateTests {

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should update driver status to ONLINE with location")
        void updateDriverStatus_ToOnlineWithLocation_ReturnsSuccess() throws Exception {
            // Arrange
            DriverUpdateStatus request = new DriverUpdateStatus(DriverStatus.ONLINE, -74.0060, 40.7128);
            doNothing().when(driverStatusService).updateDriverStatus(any(UUID.class), eq(DriverStatus.ONLINE), eq(-74.0060), eq(40.7128));

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/status")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Driver status updated successfully."));

            verify(driverStatusService).updateDriverStatus(any(UUID.class), eq(DriverStatus.ONLINE), eq(-74.0060), eq(40.7128));
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should update driver status to OFFLINE")
        void updateDriverStatus_ToOffline_ReturnsSuccess() throws Exception {
            // Arrange
            DriverUpdateStatus request = new DriverUpdateStatus(DriverStatus.OFFLINE, 0.0, 0.0);
            doNothing().when(driverStatusService).updateDriverStatus(any(UUID.class), eq(DriverStatus.OFFLINE), eq(0.0), eq(0.0));

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/status")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(driverStatusService).updateDriverStatus(any(UUID.class), eq(DriverStatus.OFFLINE), eq(0.0), eq(0.0));
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should update driver status to BUSY")
        void updateDriverStatus_ToBusy_ReturnsSuccess() throws Exception {
            // Arrange
            DriverUpdateStatus request = new DriverUpdateStatus(DriverStatus.BUSY, 0.0, 0.0);
            doNothing().when(driverStatusService).updateDriverStatus(any(UUID.class), eq(DriverStatus.BUSY), eq(0.0), eq(0.0));

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/status")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(driverStatusService).updateDriverStatus(any(UUID.class), eq(DriverStatus.BUSY), eq(0.0), eq(0.0));
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should handle multiple status transitions")
        void updateDriverStatus_MultipleTransitions_AllSucceed() throws Exception {
            // Test OFFLINE -> ONLINE -> BUSY -> OFFLINE
            DriverUpdateStatus[] statuses = {
                    new DriverUpdateStatus(DriverStatus.ONLINE, -74.0, 40.7),
                    new DriverUpdateStatus(DriverStatus.BUSY, -74.0, 40.7),
                    new DriverUpdateStatus(DriverStatus.OFFLINE, 0.0, 0.0)
            };

            for (DriverUpdateStatus status : statuses) {
                mockMvc.perform(post("/api/v1/drivers/status")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(status)))
                        .andExpect(status().isOk());
            }
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"ADMIN"})
        @DisplayName("Should allow admin to update driver status")
        void updateDriverStatus_AsAdmin_ReturnsSuccess() throws Exception {
            // Arrange
            DriverUpdateStatus request = new DriverUpdateStatus(DriverStatus.ONLINE, -74.0060, 40.7128);
            doNothing().when(driverStatusService).updateDriverStatus(any(UUID.class), eq(DriverStatus.ONLINE), eq(-74.0060), eq(40.7128));

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/status")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Should return unauthorized when user is not authenticated")
        void updateDriverStatus_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
            // Arrange
            DriverUpdateStatus request = new DriverUpdateStatus(DriverStatus.ONLINE, -74.0060, 40.7128);

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/status")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());

            verify(driverStatusService, never()).updateDriverStatus(any(), any(), anyDouble(), anyDouble());
        }

        @Test
        @WithMockUser(username = "invalid-uuid", roles = {"DRIVER"})
        @DisplayName("Should handle invalid UUID format")
        void updateDriverStatus_WithInvalidUUID_ReturnsError() throws Exception {
            // Arrange
            DriverUpdateStatus request = new DriverUpdateStatus(DriverStatus.ONLINE, -74.0060, 40.7128);

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/status")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should reject empty request body")
        void updateDriverStatus_WithEmptyBody_ReturnsBadRequest() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/status")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should reject malformed JSON")
        void updateDriverStatus_WithMalformedJson_ReturnsBadRequest() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/status")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("invalid-json"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should handle service exceptions gracefully")
        void updateDriverStatus_WhenServiceThrowsException_ReturnsError() throws Exception {
            // Arrange
            DriverUpdateStatus request = new DriverUpdateStatus(DriverStatus.ONLINE, 0.0, 0.0);
            doThrow(new InvalidStatusException("Location required for ONLINE status"))
                    .when(driverStatusService).updateDriverStatus(any(UUID.class), eq(DriverStatus.ONLINE), eq(0.0), eq(0.0));

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/status")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should handle concurrent status updates")
        void updateDriverStatus_ConcurrentUpdates_HandlesCorrectly() throws Exception {
            // Arrange
            DriverUpdateStatus request = new DriverUpdateStatus(DriverStatus.ONLINE, -74.0060, 40.7128);
            doNothing().when(driverStatusService).updateDriverStatus(any(), any(), anyDouble(), anyDouble());

            // Act - Multiple rapid requests
            for (int i = 0; i < 3; i++) {
                mockMvc.perform(post("/api/v1/drivers/status")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk());
            }

            // Assert - All requests processed
            verify(driverStatusService, times(3)).updateDriverStatus(any(), any(), anyDouble(), anyDouble());
        }

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
        @DisplayName("Should handle status update with precise coordinates")
        void updateDriverStatus_WithPreciseCoordinates_ReturnsSuccess() throws Exception {
            // Arrange
            DriverUpdateStatus request = new DriverUpdateStatus(DriverStatus.ONLINE, -74.005974, 40.712776);
            doNothing().when(driverStatusService).updateDriverStatus(any(), any(), anyDouble(), anyDouble());

            // Act & Assert
            mockMvc.perform(post("/api/v1/drivers/status")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }
}

