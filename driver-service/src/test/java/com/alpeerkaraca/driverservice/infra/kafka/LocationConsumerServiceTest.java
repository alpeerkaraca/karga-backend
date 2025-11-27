package com.alpeerkaraca.driverservice.infra.kafka;

import com.alpeerkaraca.driverservice.dto.DriverLocationMessage;
import com.alpeerkaraca.driverservice.model.DriverStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocationConsumerService Tests")
class LocationConsumerServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private GeoOperations<String, String> geoOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private LocationConsumerService locationConsumerService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should update location in Redis when driver is ONLINE")
    void consumeLocationUpdate_WhenDriverIsOnline_UpdatesLocation() {
        // Arrange
        UUID driverId = UUID.randomUUID();
        double latitude = 40.7128;
        double longitude = -74.0060;

        DriverLocationMessage message = new DriverLocationMessage(
                driverId,
                latitude,
                longitude,
                Timestamp.valueOf(LocalDateTime.now())
        );

        String driverIdStr = driverId.toString();
        String statusKey = "driver:status:" + driverIdStr;

        when(valueOperations.get(statusKey)).thenReturn(DriverStatus.ONLINE.name());
        when(geoOperations.add(anyString(), any(Point.class), anyString())).thenReturn(1L);

        // Act
        locationConsumerService.consumeLocationUpdate(message);

        // Assert
        ArgumentCaptor<Point> pointCaptor = ArgumentCaptor.forClass(Point.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> memberCaptor = ArgumentCaptor.forClass(String.class);

        verify(geoOperations).add(keyCaptor.capture(), pointCaptor.capture(), memberCaptor.capture());

        assertThat(keyCaptor.getValue()).isEqualTo("online_drivers_locations");
        assertThat(pointCaptor.getValue().getX()).isEqualTo(longitude);
        assertThat(pointCaptor.getValue().getY()).isEqualTo(latitude);
        assertThat(memberCaptor.getValue()).isEqualTo(driverIdStr);
    }

    @Test
    @DisplayName("Should not update location when driver is OFFLINE")
    void consumeLocationUpdate_WhenDriverIsOffline_DoesNotUpdateLocation() {
        // Arrange
        UUID driverId = UUID.randomUUID();
        double latitude = 40.7128;
        double longitude = -74.0060;

        DriverLocationMessage message = new DriverLocationMessage(
                driverId,
                latitude,
                longitude,
                Timestamp.valueOf(LocalDateTime.now())
        );

        String driverIdStr = driverId.toString();
        String statusKey = "driver:status:" + driverIdStr;

        when(valueOperations.get(statusKey)).thenReturn(DriverStatus.OFFLINE.name());

        // Act
        locationConsumerService.consumeLocationUpdate(message);

        // Assert
        verify(geoOperations, never()).add(anyString(), any(Point.class), anyString());
    }

    @Test
    @DisplayName("Should not update location when driver is BUSY")
    void consumeLocationUpdate_WhenDriverIsBusy_DoesNotUpdateLocation() {
        // Arrange
        UUID driverId = UUID.randomUUID();
        double latitude = 40.7128;
        double longitude = -74.0060;

        DriverLocationMessage message = new DriverLocationMessage(
                driverId,
                latitude,
                longitude,
                Timestamp.valueOf(LocalDateTime.now())
        );

        String driverIdStr = driverId.toString();
        String statusKey = "driver:status:" + driverIdStr;

        when(valueOperations.get(statusKey)).thenReturn(DriverStatus.BUSY.name());

        // Act
        locationConsumerService.consumeLocationUpdate(message);

        // Assert
        verify(geoOperations, never()).add(anyString(), any(Point.class), anyString());
    }

    @Test
    @DisplayName("Should not update location when driver status is null")
    void consumeLocationUpdate_WhenDriverStatusIsNull_DoesNotUpdateLocation() {
        // Arrange
        UUID driverId = UUID.randomUUID();
        double latitude = 40.7128;
        double longitude = -74.0060;

        DriverLocationMessage message = new DriverLocationMessage(
                driverId,
                latitude,
                longitude,
                Timestamp.valueOf(LocalDateTime.now())
        );

        String driverIdStr = driverId.toString();
        String statusKey = "driver:status:" + driverIdStr;

        when(valueOperations.get(statusKey)).thenReturn(null);

        // Act
        locationConsumerService.consumeLocationUpdate(message);

        // Assert
        verify(geoOperations, never()).add(anyString(), any(Point.class), anyString());
    }

    @Test
    @DisplayName("Should handle different coordinates correctly")
    void consumeLocationUpdate_WithDifferentCoordinates_UpdatesCorrectly() {
        // Arrange
        UUID driverId = UUID.randomUUID();
        double latitude = -33.8688;  // Sydney
        double longitude = 151.2093;

        DriverLocationMessage message = new DriverLocationMessage(
                driverId,
                latitude,
                longitude,
                Timestamp.valueOf(LocalDateTime.now())
        );

        String driverIdStr = driverId.toString();
        String statusKey = "driver:status:" + driverIdStr;

        when(valueOperations.get(statusKey)).thenReturn(DriverStatus.ONLINE.name());
        when(geoOperations.add(anyString(), any(Point.class), anyString())).thenReturn(1L);

        // Act
        locationConsumerService.consumeLocationUpdate(message);

        // Assert
        ArgumentCaptor<Point> pointCaptor = ArgumentCaptor.forClass(Point.class);
        verify(geoOperations).add(anyString(), pointCaptor.capture(), anyString());

        assertThat(pointCaptor.getValue().getX()).isEqualTo(longitude);
        assertThat(pointCaptor.getValue().getY()).isEqualTo(latitude);
    }
}

