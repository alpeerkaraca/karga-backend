package com.alpeerkaraca.driverservice.integration;

import com.alpeerkaraca.driverservice.AbstractIntegrationTest;
import com.alpeerkaraca.driverservice.dto.DriverLocationMessage;
import com.alpeerkaraca.driverservice.dto.DriverUpdateStatus;
import com.alpeerkaraca.driverservice.dto.TripMessage;
import com.alpeerkaraca.driverservice.model.Driver;
import com.alpeerkaraca.driverservice.model.DriverStatus;
import com.alpeerkaraca.driverservice.model.Vehicle;
import com.alpeerkaraca.driverservice.repository.DriverRepository;
import com.alpeerkaraca.driverservice.repository.VehicleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Driver Service Integration Tests")
class DriverServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DriverRepository driverRepository;
    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private KafkaTemplate<String, DriverLocationMessage> locationKafkaTemplate;
    @Autowired
    private KafkaTemplate<String, TripMessage> tripKafkaTemplate;
    private Driver testDriver;
    private UUID testDriverId;


    @BeforeEach
    void setUp() {
        // Clean up
        driverRepository.deleteAll();
        vehicleRepository.deleteAll();

        // Clear Redis
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();

        // Create test vehicle
        Vehicle vehicle = new Vehicle();
        vehicle.setBrand("Toyota");
        vehicle.setModel("Camry");
        vehicle.setPlate("ABC-123");
        vehicle.setColor("Black");
        vehicle.setYear("2022");
        vehicle = vehicleRepository.save(vehicle);

        // Create test driver
        testDriverId = UUID.randomUUID();
        testDriver = Driver.builder()
                .driverId(testDriverId)
                .vehicle(vehicle)
                .isApproved(true)
                .isActive(true)
                .build();
        testDriver = driverRepository.save(testDriver);
    }

    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
    @DisplayName("Should update driver status to ONLINE and store in Redis")
    void updateDriverStatus_ToOnline_StoresInRedis() throws Exception {
        // Arrange
        DriverUpdateStatus request = new DriverUpdateStatus(DriverStatus.ONLINE, -74.0060, 40.7128);

        // Act
        mockMvc.perform(post("/api/v1/drivers/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Assert
        String statusKey = "driver:status:550e8400-e29b-41d4-a716-446655440000";
        String status = redisTemplate.opsForValue().get(statusKey);
        assertThat(status).isEqualTo("ONLINE");

        // Check geo location
        Long count = redisTemplate.opsForGeo().remove("online_drivers_locations", "550e8400-e29b-41d4-a716-446655440000");
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
    @DisplayName("Should update driver status to OFFLINE and remove from Redis")
    void updateDriverStatus_ToOffline_RemovesFromRedis() throws Exception {
        // Arrange - First set driver to ONLINE
        DriverUpdateStatus onlineRequest = new DriverUpdateStatus(DriverStatus.ONLINE, -74.0060, 40.7128);
        mockMvc.perform(post("/api/v1/drivers/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(onlineRequest)));

        // Act - Set to OFFLINE
        DriverUpdateStatus offlineRequest = new DriverUpdateStatus(DriverStatus.OFFLINE, 0.0, 0.0);
        mockMvc.perform(post("/api/v1/drivers/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offlineRequest)))
                .andExpect(status().isOk());

        // Assert
        String statusKey = "driver:status:550e8400-e29b-41d4-a716-446655440000";
        String status = redisTemplate.opsForValue().get(statusKey);
        assertThat(status).isNull();
    }

    @Test
    @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000", roles = {"DRIVER"})
    @DisplayName("Should publish location update to Kafka")
    void publishLocationUpdate_SendsToKafka() throws Exception {
        // Arrange
        com.alpeerkaraca.driverservice.dto.LocationUpdateRequest request =
                new com.alpeerkaraca.driverservice.dto.LocationUpdateRequest(40.7128, -74.0060);

        // Act
        mockMvc.perform(post("/api/v1/drivers/location")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

    }

    @Test
    @DisplayName("Should handle trip accepted event and set driver to BUSY")
    void handleTripAccepted_SetsDriverToBusy() {
        // Arrange - First set driver ONLINE
        redisTemplate.opsForValue().set("driver:status:" + testDriverId, DriverStatus.ONLINE.name());

        TripMessage message = new TripMessage(
                "TRIP_ACCEPTED",
                UUID.randomUUID(),
                testDriverId,
                Timestamp.valueOf(LocalDateTime.now()),
                BigDecimal.ZERO,
                UUID.randomUUID(),
                0.56,
                1.23
        );

        // Act
        tripKafkaTemplate.send("trip_events", message);

        // Assert - Wait for async processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            String status = redisTemplate.opsForValue().get("driver:status:" + testDriverId);
            assertThat(status).isEqualTo("BUSY");
        });
    }

    @Test
    @DisplayName("Should handle trip completed event and set driver to ONLINE")
    void handleTripCompleted_SetsDriverToOnline() {
        // Arrange - First set driver BUSY
        redisTemplate.opsForValue().set("driver:status:" + testDriverId, DriverStatus.BUSY.name());

        TripMessage message = new TripMessage(
                "TRIP_COMPLETED",
                UUID.randomUUID(),
                testDriverId,
                Timestamp.valueOf(LocalDateTime.now()),
                new BigDecimal("25.50"),
                UUID.randomUUID(),
                1,
                1
        );

        // Act
        tripKafkaTemplate.send("trip_events", message);

        // Assert - Wait for async processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            String status = redisTemplate.opsForValue().get("driver:status:" + testDriverId);
            assertThat(status).isEqualTo("ONLINE");
        });
    }

    @Test
    @DisplayName("Should update location in Redis for online driver")
    void handleLocationUpdate_ForOnlineDriver_UpdatesRedis() {
        // Arrange - Set driver ONLINE with location
        String driverIdStr = testDriverId.toString();
        redisTemplate.opsForValue().set("driver:status:" + driverIdStr, DriverStatus.ONLINE.name());

        DriverLocationMessage message = new DriverLocationMessage(
                testDriverId,
                40.7128,
                -74.0060,
                Timestamp.valueOf(LocalDateTime.now())
        );

        // Act
        locationKafkaTemplate.send("driver_location_updates", message);

        // Assert - Wait for async processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // Check if location was added to geo index
            var position = redisTemplate.opsForGeo().position("online_drivers_locations", driverIdStr);
            assertThat(position).isNotNull();
            assertThat(position).isNotEmpty();
        });
    }

    @Test
    @DisplayName("Should retrieve driver by ID from database")
    void getDriverById_ReturnsDriverDetails() {
        // Act
        Driver found = driverRepository.findDriverByDriverId(testDriverId).orElse(null);

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getDriverId()).isEqualTo(testDriverId);
        assertThat(found.isApproved()).isTrue();
        assertThat(found.isActive()).isTrue();
        assertThat(found.getVehicle()).isNotNull();
        assertThat(found.getVehicle().getPlate()).isEqualTo("ABC-123");
    }
}

