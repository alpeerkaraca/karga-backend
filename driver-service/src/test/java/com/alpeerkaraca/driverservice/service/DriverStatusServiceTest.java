package com.alpeerkaraca.driverservice.service;

import com.alpeerkaraca.common.exception.InvalidStatusException;
import com.alpeerkaraca.driverservice.model.DriverStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverStatusServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private GeoOperations<String, String> geoOperations;

    @InjectMocks
    private DriverStatusService driverStatusService;

    private UUID testDriverId;

    @BeforeEach
    void setUp() {
        testDriverId = UUID.randomUUID();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
    }

    @Test
    @DisplayName("Should update driver status to ONLINE with location")
    void updateDriverStatus_OnlineWithLocation_Success() {
        // Arrange
        Double latitude = 41.0082;
        Double longitude = 28.9784;

        // Act
        driverStatusService.updateDriverStatus(testDriverId, DriverStatus.ONLINE, longitude, latitude);

        // Assert
        verify(valueOperations).set("driver:status:" + testDriverId, DriverStatus.ONLINE.name());
        verify(geoOperations).add(eq("online_drivers_locations"), any(Point.class), eq(testDriverId.toString()));
    }

    @Test
    @DisplayName("Should throw exception when setting ONLINE status without location")
    void updateDriverStatus_OnlineWithoutLocation_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> driverStatusService.updateDriverStatus(
                testDriverId, DriverStatus.ONLINE, null, null))
                .isInstanceOf(InvalidStatusException.class)
                .hasMessageContaining("konum bilgisi zorunludur");

        verify(valueOperations, never()).set(anyString(), anyString());
        verify(geoOperations, never()).add(anyString(), any(Point.class), anyString());
    }

    @Test
    @DisplayName("Should remove driver from Redis when going OFFLINE")
    void updateDriverStatus_Offline_RemovesFromRedis() {
        // Act
        driverStatusService.updateDriverStatus(testDriverId, DriverStatus.OFFLINE, null, null);

        // Assert
        verify(redisTemplate).delete("driver:status:" + testDriverId);
        verify(geoOperations).remove("online_drivers_locations", testDriverId.toString());
    }

    @Test
    @DisplayName("Should remove driver when status is BUSY and update status")
    void updateDriverStatus_Busy_RemovesFromRedisAndUpdatesStatus() {
        // Act
        driverStatusService.updateDriverStatus(testDriverId, DriverStatus.BUSY, 1.23, 0.56);

        // Assert
        verify(valueOperations).set("driver:status:" + testDriverId, DriverStatus.BUSY.toString());
        verify(geoOperations).remove("online_drivers_locations", testDriverId.toString());
        verify(geoOperations).add(eq("busy_drivers_locations"), any(Point.class), eq(testDriverId.toString()));
    }

    @Test
    @DisplayName("Should handle ONLINE status with valid coordinates")
    void updateDriverStatus_ValidCoordinates_Success() {
        // Arrange
        Double latitude = 40.7128;
        Double longitude = -74.0060;

        // Act
        driverStatusService.updateDriverStatus(testDriverId, DriverStatus.ONLINE, longitude, latitude);

        // Assert
        verify(valueOperations).set("driver:status:" + testDriverId, DriverStatus.ONLINE.name());
        verify(geoOperations).add(eq("online_drivers_locations"), argThat(point ->
                point.getX() == longitude && point.getY() == latitude
        ), eq(testDriverId.toString()));
    }
}

