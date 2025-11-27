package com.alpeerkaraca.tripservice.controller;

import com.alpeerkaraca.common.exception.ConflictException;
import com.alpeerkaraca.common.exception.ResourceNotFoundException;
import com.alpeerkaraca.tripservice.dto.NearbyDriversResponse;
import com.alpeerkaraca.tripservice.dto.TripRequest;
import com.alpeerkaraca.tripservice.model.Trip;
import com.alpeerkaraca.tripservice.model.TripStatus;
import com.alpeerkaraca.tripservice.service.TripManagementService;
import com.alpeerkaraca.tripservice.service.TripRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripsController.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TripsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TripRequestService tripRequestService;

    @MockitoBean
    private TripManagementService tripManagementService;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    private UUID testPassengerId;
    private UUID testTripId;
    private Trip testTrip;

    @BeforeEach
    void setUp() {
        testPassengerId = UUID.randomUUID();
        testTripId = UUID.randomUUID();

        testTrip = Trip.builder()
                .tripId(testTripId)
                .passengerId(testPassengerId)
                .startLatitude(41.0082)
                .startLongitude(28.9784)
                .endLatitude(41.0200)
                .endLongitude(28.9900)
                .tripStatus(TripStatus.REQUESTED)
                .requestedAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/trips/nearby-drivers - Get Nearby Drivers")
    class GetNearbyDriversTests {

        @Test
        @WithMockUser
        @DisplayName("Should return nearby drivers successfully")
        void getNearbyDrivers_ValidCoordinates_ReturnsDrivers() throws Exception {
            // Arrange
            double latitude = 41.0082;
            double longitude = 28.9784;

            List<NearbyDriversResponse> nearbyDrivers = Arrays.asList(
                    new NearbyDriversResponse(UUID.randomUUID(), 41.0100, 28.9800, 1.5),
                    new NearbyDriversResponse(UUID.randomUUID(), 41.0050, 28.9750, 2.3)
            );

            when(tripRequestService.findNearbyDrivers(latitude, longitude, 5.0))
                    .thenReturn(nearbyDrivers);

            // Act & Assert
            mockMvc.perform(get("/api/v1/trips/nearby-drivers")
                            .param("latitude", String.valueOf(latitude))
                            .param("longitude", String.valueOf(longitude))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Yakındaki sürücüler listelendi."))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].distanceKm").value(1.5))
                    .andExpect(jsonPath("$.data[1].distanceKm").value(2.3));

            verify(tripRequestService).findNearbyDrivers(latitude, longitude, 5.0);
        }

        @Test
        @WithMockUser
        @DisplayName("Should return empty list when no drivers nearby")
        void getNearbyDrivers_NoDrivers_ReturnsEmptyList() throws Exception {
            // Arrange
            double latitude = 41.0082;
            double longitude = 28.9784;

            when(tripRequestService.findNearbyDrivers(latitude, longitude, 5.0))
                    .thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/v1/trips/nearby-drivers")
                            .param("latitude", String.valueOf(latitude))
                            .param("longitude", String.valueOf(longitude))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(tripRequestService).findNearbyDrivers(latitude, longitude, 5.0);
        }

        @Test
        @WithMockUser
        @DisplayName("Should fail when latitude parameter is missing")
        void getNearbyDrivers_MissingLatitude_ReturnsBadRequest() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/trips/nearby-drivers")
                            .param("longitude", "28.9784")
                            .with(csrf()))
                    .andExpect(status().isBadRequest());

            verify(tripRequestService, never()).findNearbyDrivers(anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        @WithMockUser
        @DisplayName("Should fail when longitude parameter is missing")
        void getNearbyDrivers_MissingLongitude_ReturnsBadRequest() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/trips/nearby-drivers")
                            .param("latitude", "41.0082")
                            .with(csrf()))
                    .andExpect(status().isBadRequest());

            verify(tripRequestService, never()).findNearbyDrivers(anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle invalid coordinate format")
        void getNearbyDrivers_InvalidCoordinates_ReturnsBadRequest() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/trips/nearby-drivers")
                            .param("latitude", "invalid")
                            .param("longitude", "invalid")
                            .with(csrf()))
                    .andExpect(status().isBadRequest());

            verify(tripRequestService, never()).findNearbyDrivers(anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        @WithMockUser
        @DisplayName("Should handle extreme coordinates")
        void getNearbyDrivers_ExtremeCoordinates_Works() throws Exception {
            // Arrange
            double latitude = -89.9999;
            double longitude = 179.9999;

            when(tripRequestService.findNearbyDrivers(latitude, longitude, 5.0))
                    .thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/v1/trips/nearby-drivers")
                            .param("latitude", String.valueOf(latitude))
                            .param("longitude", String.valueOf(longitude))
                            .with(csrf()))
                    .andExpect(status().isOk());

            verify(tripRequestService).findNearbyDrivers(latitude, longitude, 5.0);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/trips/request - Request Trip")
    class RequestTripTests {

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = "PASSENGER")
        @DisplayName("Should create trip request successfully")
        void requestTrip_ValidRequest_CreatesTrip() throws Exception {
            // Arrange
            TripRequest request = new TripRequest(41.0082, 28.9784, 41.0200, 28.9900);
            UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

            when(tripRequestService.requestTrip(any(TripRequest.class), eq(userId)))
                    .thenReturn(testTrip);

            // Act & Assert
            mockMvc.perform(post("/api/v1/trips/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Yolculuk talebiniz alındı."))
                    .andExpect(jsonPath("$.data.tripId").value(testTripId.toString()))
                    .andExpect(jsonPath("$.data.tripStatus").value("REQUESTED"))
                    .andExpect(jsonPath("$.data.startLatitude").value(41.0082))
                    .andExpect(jsonPath("$.data.startLongitude").value(28.9784));

            verify(tripRequestService).requestTrip(any(TripRequest.class), eq(userId));
        }

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = "PASSENGER")
        @DisplayName("Should fail when start latitude is out of range")
        void requestTrip_InvalidStartLatitude_ReturnsBadRequest() throws Exception {
            // Arrange - latitude > 90
            TripRequest request = new TripRequest(91.0, 28.9784, 41.0200, 28.9900);

            // Act & Assert
            mockMvc.perform(post("/api/v1/trips/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());

            verify(tripRequestService, never()).requestTrip(any(TripRequest.class), any(UUID.class));
        }

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = "PASSENGER")
        @DisplayName("Should fail when start longitude is out of range")
        void requestTrip_InvalidStartLongitude_ReturnsBadRequest() throws Exception {
            // Arrange - longitude > 180
            TripRequest request = new TripRequest(41.0082, 181.0, 41.0200, 28.9900);

            // Act & Assert
            mockMvc.perform(post("/api/v1/trips/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());

            verify(tripRequestService, never()).requestTrip(any(TripRequest.class), any(UUID.class));
        }

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = "PASSENGER")
        @DisplayName("Should fail when end latitude is negative and out of range")
        void requestTrip_InvalidEndLatitude_ReturnsBadRequest() throws Exception {
            // Arrange - latitude < -90
            TripRequest request = new TripRequest(41.0082, 28.9784, -91.0, 28.9900);

            // Act & Assert
            mockMvc.perform(post("/api/v1/trips/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());

            verify(tripRequestService, never()).requestTrip(any(TripRequest.class), any(UUID.class));
        }

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = "PASSENGER")
        @DisplayName("Should fail when end longitude is negative and out of range")
        void requestTrip_InvalidEndLongitude_ReturnsBadRequest() throws Exception {
            // Arrange - longitude < -180
            TripRequest request = new TripRequest(41.0082, 28.9784, 41.0200, -181.0);

            // Act & Assert
            mockMvc.perform(post("/api/v1/trips/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());

            verify(tripRequestService, never()).requestTrip(any(TripRequest.class), any(UUID.class));
        }

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = "PASSENGER")
        @DisplayName("Should handle valid boundary coordinates")
        void requestTrip_BoundaryCoordinates_Works() throws Exception {
            // Arrange - min and max valid values
            TripRequest request = new TripRequest(-90.0, -180.0, 90.0, 180.0);
            UUID userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

            Trip trip = Trip.builder()
                    .tripId(UUID.randomUUID())
                    .passengerId(userId)
                    .startLatitude(-90.0)
                    .startLongitude(-180.0)
                    .endLatitude(90.0)
                    .endLongitude(180.0)
                    .tripStatus(TripStatus.REQUESTED)
                    .build();

            when(tripRequestService.requestTrip(any(TripRequest.class), eq(userId)))
                    .thenReturn(trip);

            // Act & Assert
            mockMvc.perform(post("/api/v1/trips/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(tripRequestService).requestTrip(any(TripRequest.class), eq(userId));
        }

        @Test
        @WithMockUser(username = "invalid-uuid", roles = "PASSENGER")
        @DisplayName("Should fail when user ID is not a valid UUID")
        void requestTrip_InvalidUserId_ReturnsError() throws Exception {
            // Arrange
            TripRequest request = new TripRequest(41.0082, 28.9784, 41.0200, 28.9900);

            // Act & Assert
            mockMvc.perform(post("/api/v1/trips/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().is5xxServerError());

            verify(tripRequestService, never()).requestTrip(any(TripRequest.class), any(UUID.class));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/trips/available - Get Available Trips")
    class GetAvailableTripsTests {

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("Should return available trips successfully")
        void getAvailableTrips_TripsExist_ReturnsList() throws Exception {
            // Arrange
            List<Trip> availableTrips = Arrays.asList(
                    testTrip,
                    Trip.builder()
                            .tripId(UUID.randomUUID())
                            .passengerId(UUID.randomUUID())
                            .startLatitude(40.9999)
                            .startLongitude(28.8888)
                            .endLatitude(41.1111)
                            .endLongitude(29.0000)
                            .tripStatus(TripStatus.REQUESTED)
                            .build()
            );

            when(tripManagementService.getAvailableTrips()).thenReturn(availableTrips);

            // Act & Assert
            mockMvc.perform(get("/api/v1/trips/available")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Mevcut yolculuklar listelendi."))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].tripStatus").value("REQUESTED"))
                    .andExpect(jsonPath("$.data[1].tripStatus").value("REQUESTED"));

            verify(tripManagementService).getAvailableTrips();
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("Should return empty list when no trips available")
        void getAvailableTrips_NoTrips_ReturnsEmptyList() throws Exception {
            // Arrange
            when(tripManagementService.getAvailableTrips()).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/v1/trips/available")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(tripManagementService).getAvailableTrips();
        }
    }

    @Nested
    @DisplayName("POST /api/v1/trips/{tripId}/accept - Accept Trip")
    class AcceptTripTests {

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = "DRIVER")
        @DisplayName("Should accept trip successfully as driver")
        void acceptTrip_ValidDriverAndTrip_AcceptsTrip() throws Exception {
            // Arrange
            UUID driverId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
            testTrip.setDriverId(driverId);
            testTrip.setTripStatus(TripStatus.ACCEPTED);

            when(tripManagementService.acceptTrip(testTripId, driverId))
                    .thenReturn(testTrip);

            // Act & Assert
            mockMvc.perform(post("/api/v1/trips/" + testTripId + "/accept")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Trip accepted."))
                    .andExpect(jsonPath("$.data.tripStatus").value("ACCEPTED"))
                    .andExpect(jsonPath("$.data.driverId").value(driverId.toString()));

            verify(tripManagementService).acceptTrip(testTripId, driverId);
        }

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = "USER")
        @DisplayName("Should fail when user is not a driver")
        void acceptTrip_NonDriver_ReturnsForbidden() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/trips/" + testTripId + "/accept")
                            .with(csrf()))
                    .andExpect(status().isForbidden());

            verify(tripManagementService, never()).acceptTrip(any(UUID.class), any(UUID.class));
        }

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = "DRIVER")
        @DisplayName("Should fail when trip not found")
        void acceptTrip_TripNotFound_ReturnsNotFound() throws Exception {
            // Arrange
            UUID driverId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
            UUID nonExistentTripId = UUID.randomUUID();

            when(tripManagementService.acceptTrip(nonExistentTripId, driverId))
                    .thenThrow(new ResourceNotFoundException("Trip not found: " + nonExistentTripId));

            // Act & Assert
            mockMvc.perform(post("/api/v1/trips/" + nonExistentTripId + "/accept")
                            .with(csrf()))
                    .andExpect(status().isNotFound());

            verify(tripManagementService).acceptTrip(nonExistentTripId, driverId);
        }

        @Test
        @WithMockUser(username = "123e4567-e89b-12d3-a456-426614174000", roles = "DRIVER")
        @DisplayName("Should fail when trip already accepted")
        void acceptTrip_AlreadyAccepted_ReturnsConflict() throws Exception {
            // Arrange
            UUID driverId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

            when(tripManagementService.acceptTrip(testTripId, driverId))
                    .thenThrow(new ConflictException("Trip has already been accepted or completed."));

            // Act & Assert
            mockMvc.perform(post("/api/v1/trips/" + testTripId + "/accept")
                            .with(csrf()))
                    .andExpect(status().isConflict());

            verify(tripManagementService).acceptTrip(testTripId, driverId);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/trips/{tripId}/start - Start Trip")
    class StartTripTests {

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("Should start trip successfully")
        void startTrip_ValidTrip_StartsTrip() throws Exception {
            // Arrange
            doNothing().when(tripManagementService).startTrip(testTripId);

            // Act & Assert
            mockMvc.perform(post("/api/v1/trips/" + testTripId + "/start")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Yolculuk başlatıldı."))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(tripManagementService).startTrip(testTripId);
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("Should fail when trip not found")
        void startTrip_TripNotFound_ReturnsNotFound() throws Exception {
            // Arrange
            UUID nonExistentTripId = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("Trip not found: " + nonExistentTripId))
                    .when(tripManagementService).startTrip(nonExistentTripId);

            // Act & Assert
            mockMvc.perform(post("/api/v1/trips/" + nonExistentTripId + "/start")
                            .with(csrf()))
                    .andExpect(status().isNotFound());

            verify(tripManagementService).startTrip(nonExistentTripId);
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("Should fail when trip not in ACCEPTED status")
        void startTrip_InvalidStatus_ReturnsConflict() throws Exception {
            // Arrange
            doThrow(new ConflictException("Trip can not be started. Trip status must be ACCEPTED."))
                    .when(tripManagementService).startTrip(testTripId);

            // Act & Assert
            mockMvc.perform(post("/api/v1/trips/" + testTripId + "/start")
                            .with(csrf()))
                    .andExpect(status().isConflict());

            verify(tripManagementService).startTrip(testTripId);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/trips/{tripId}/complete - Complete Trip")
    class CompleteTripTests {

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("Should complete trip successfully")
        void completeTrip_ValidTrip_CompletesTrip() throws Exception {
            // Arrange
            doNothing().when(tripManagementService).completeTrip(testTripId);

            // Act & Assert
            mockMvc.perform(post("/api/v1/trips/" + testTripId + "/complete")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Yolculuk tamamlandı."))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(tripManagementService).completeTrip(testTripId);
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("Should fail when trip not found")
        void completeTrip_TripNotFound_ReturnsNotFound() throws Exception {
            // Arrange
            UUID nonExistentTripId = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("Trip not found: " + nonExistentTripId))
                    .when(tripManagementService).completeTrip(nonExistentTripId);

            // Act & Assert
            mockMvc.perform(post("/api/v1/trips/" + nonExistentTripId + "/complete")
                            .with(csrf()))
                    .andExpect(status().isNotFound());

            verify(tripManagementService).completeTrip(nonExistentTripId);
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("Should fail when trip not in IN_PROGRESS status")
        void completeTrip_InvalidStatus_ReturnsConflict() throws Exception {
            // Arrange
            doThrow(new ConflictException("Trip can not be completed. Trip status must be started."))
                    .when(tripManagementService).completeTrip(testTripId);

            // Act & Assert
            mockMvc.perform(post("/api/v1/trips/" + testTripId + "/complete")
                            .with(csrf()))
                    .andExpect(status().isConflict());

            verify(tripManagementService).completeTrip(testTripId);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/trips/{tripId}/cancel - Cancel Trip")
    class CancelTripTests {
        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("Should cancel trip successfully")
        void cancelTrip_ValidTrip_CancelsTrip() throws Exception {
            // Arrange
            doNothing().when(tripManagementService).cancelTrip(testTripId);

            // Act & Assert
            mockMvc.perform(post("/api/v1/trips/" + testTripId + "/cancel")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Yolculuk iptal edildi."))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(tripManagementService).cancelTrip(testTripId);
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("Should fail when trip not found")
        void cancelTrip_TripNotFound_ReturnsNotFound() throws Exception {
            // Arrange
            UUID nonExistentTripId = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("Trip not found: " + nonExistentTripId))
                    .when(tripManagementService).cancelTrip(nonExistentTripId);

            // Act & Assert
            mockMvc.perform(post("/api/v1/trips/" + nonExistentTripId + "/cancel")
                            .with(csrf()))
                    .andExpect(status().isNotFound());

            verify(tripManagementService).cancelTrip(nonExistentTripId);
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("Should fail when trip already completed")
        void cancelTrip_AlreadyCompleted_ReturnsConflict() throws Exception {
            // Arrange
            doThrow(new ConflictException("Trip has already been completed."))
                    .when(tripManagementService).cancelTrip(testTripId);

            // Act & Assert
            mockMvc.perform(post("/api/v1/trips/" + testTripId + "/cancel")
                            .with(csrf()))
                    .andExpect(status().isConflict());

            verify(tripManagementService).cancelTrip(testTripId);
        }

        @Test
        @WithMockUser(roles = "DRIVER")
        @DisplayName("Should fail when trip already cancelled")
        void cancelTrip_AlreadyCancelled_ReturnsConflict() throws Exception {
            // Arrange
            doThrow(new ConflictException("Trip has already been cancelled."))
                    .when(tripManagementService).cancelTrip(testTripId);

            // Act & Assert
            mockMvc.perform(post("/api/v1/trips/" + testTripId + "/cancel")
                            .with(csrf()))
                    .andExpect(status().isConflict());

            verify(tripManagementService).cancelTrip(testTripId);
        }
    }
}

