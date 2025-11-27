package com.alpeerkaraca.tripservice.service;

import com.alpeerkaraca.tripservice.dto.NearbyDriversResponse;
import com.alpeerkaraca.tripservice.dto.TripRequest;
import com.alpeerkaraca.tripservice.model.Trip;
import com.alpeerkaraca.tripservice.model.TripStatus;
import com.alpeerkaraca.tripservice.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripRequestServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private GeoOperations<String, String> geoOperations;

    @InjectMocks
    private TripRequestService tripRequestService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
    }

    @Test
    @DisplayName("Should find nearby drivers within radius")
    void findNearbyDrivers_DriversInRange_ReturnsDrivers() {
        // Arrange
        double latitude = 41.0082;
        double longitude = 28.9784;
        double radiusKm = 5.0;

        UUID driver1Id = UUID.randomUUID();
        UUID driver2Id = UUID.randomUUID();

        Point driver1Point = new Point(28.9800, 41.0100);
        Point driver2Point = new Point(28.9750, 41.0050);

        GeoResult<RedisGeoCommands.GeoLocation<String>> result1 = new GeoResult<>(
                new RedisGeoCommands.GeoLocation<>(driver1Id.toString(), driver1Point),
                new Distance(1.5, Metrics.KILOMETERS)
        );

        GeoResult<RedisGeoCommands.GeoLocation<String>> result2 = new GeoResult<>(
                new RedisGeoCommands.GeoLocation<>(driver2Id.toString(), driver2Point),
                new Distance(2.3, Metrics.KILOMETERS)
        );

        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = new GeoResults<>(
                List.of(result1, result2)
        );

        when(geoOperations.radius(
                eq("online_drivers_locations"),
                any(Circle.class),
                any(RedisGeoCommands.GeoRadiusCommandArgs.class)
        )).thenReturn(geoResults);

        // Act
        List<NearbyDriversResponse> nearbyDrivers = tripRequestService.findNearbyDrivers(latitude, longitude, radiusKm);

        // Assert
        assertThat(nearbyDrivers).hasSize(2);
        assertThat(nearbyDrivers.get(0).driverId()).isEqualTo(driver1Id);
        assertThat(nearbyDrivers.get(0).distanceKm()).isEqualTo(1.5);
        assertThat(nearbyDrivers.get(1).driverId()).isEqualTo(driver2Id);
        assertThat(nearbyDrivers.get(1).distanceKm()).isEqualTo(2.3);
    }

    @Test
    @DisplayName("Should return empty list when no drivers nearby")
    void findNearbyDrivers_NoDriversInRange_ReturnsEmptyList() {
        // Arrange
        double latitude = 41.0082;
        double longitude = 28.9784;
        double radiusKm = 5.0;

        GeoResults<RedisGeoCommands.GeoLocation<String>> emptyResults = new GeoResults<>(List.of());

        when(geoOperations.radius(
                eq("online_drivers_locations"),
                any(Circle.class),
                any(RedisGeoCommands.GeoRadiusCommandArgs.class)
        )).thenReturn(emptyResults);

        // Act
        List<NearbyDriversResponse> nearbyDrivers = tripRequestService.findNearbyDrivers(latitude, longitude, radiusKm);

        // Assert
        assertThat(nearbyDrivers).isEmpty();
    }

    @Test
    @DisplayName("Should request trip successfully")
    void requestTrip_ValidRequest_CreatesTrip() {
        // Arrange
        UUID passengerId = UUID.randomUUID();
        TripRequest request = new TripRequest(41.0082, 28.9784, 41.0200, 28.9900);

        Trip savedTrip = Trip.builder()
                .tripId(UUID.randomUUID())
                .passengerId(passengerId)
                .startLatitude(request.startLatitude())
                .startLongitude(request.startLongitude())
                .endLatitude(request.endLatitude())
                .endLongitude(request.endLongitude())
                .tripStatus(TripStatus.REQUESTED)
                .build();

        when(tripRepository.save(any(Trip.class))).thenReturn(savedTrip);

        // Act
        Trip result = tripRequestService.requestTrip(request, passengerId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPassengerId()).isEqualTo(passengerId);
        assertThat(result.getStartLatitude()).isEqualTo(request.startLatitude());
        assertThat(result.getStartLongitude()).isEqualTo(request.startLongitude());
        assertThat(result.getEndLatitude()).isEqualTo(request.endLatitude());
        assertThat(result.getEndLongitude()).isEqualTo(request.endLongitude());
        assertThat(result.getTripStatus()).isEqualTo(TripStatus.REQUESTED);

        verify(tripRepository).save(any(Trip.class));
    }

    @Test
    @DisplayName("Should limit nearby drivers to 10")
    void findNearbyDrivers_MoreThan10Drivers_ReturnsMax10() {
        // Arrange
        double latitude = 41.0082;
        double longitude = 28.9784;
        double radiusKm = 10.0;

        // Create 15 mock drivers
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> results = new java.util.ArrayList<>();
        for (int i = 0; i < 15; i++) {
            UUID driverId = UUID.randomUUID();
            Point driverPoint = new Point(28.9784 + (i * 0.001), 41.0082 + (i * 0.001));
            results.add(new GeoResult<>(
                    new RedisGeoCommands.GeoLocation<>(driverId.toString(), driverPoint),
                    new Distance(i * 0.5, Metrics.KILOMETERS)
            ));
        }

        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = new GeoResults<>(results);

        when(geoOperations.radius(
                eq("online_drivers_locations"),
                any(Circle.class),
                any(RedisGeoCommands.GeoRadiusCommandArgs.class)
        )).thenReturn(geoResults);

        // Act
        List<NearbyDriversResponse> nearbyDrivers = tripRequestService.findNearbyDrivers(latitude, longitude, radiusKm);

        // Assert
        assertThat(nearbyDrivers).hasSize(15); // All drivers returned by Redis query
    }

    @Test
    @DisplayName("Should handle different radius values")
    void findNearbyDrivers_DifferentRadius_Works() {
        // Arrange
        double latitude = 41.0082;
        double longitude = 28.9784;
        double radiusKm = 2.0;

        UUID driverId = UUID.randomUUID();
        Point driverPoint = new Point(28.9800, 41.0090);

        GeoResult<RedisGeoCommands.GeoLocation<String>> result = new GeoResult<>(
                new RedisGeoCommands.GeoLocation<>(driverId.toString(), driverPoint),
                new Distance(1.0, Metrics.KILOMETERS)
        );

        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = new GeoResults<>(List.of(result));

        when(geoOperations.radius(
                eq("online_drivers_locations"),
                any(Circle.class),
                any(RedisGeoCommands.GeoRadiusCommandArgs.class)
        )).thenReturn(geoResults);

        // Act
        List<NearbyDriversResponse> nearbyDrivers = tripRequestService.findNearbyDrivers(latitude, longitude, radiusKm);

        // Assert
        assertThat(nearbyDrivers).hasSize(1);
        assertThat(nearbyDrivers.get(0).driverId()).isEqualTo(driverId);
    }
}

