package com.alpeerkaraca.tripservice.service;

import com.alpeerkaraca.tripservice.dto.NearbyDriversResponse;
import com.alpeerkaraca.tripservice.dto.TripRequest;
import com.alpeerkaraca.tripservice.model.Trip;
import com.alpeerkaraca.tripservice.model.TripStatus;
import com.alpeerkaraca.tripservice.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling trip requests and driver discovery.
 * <p>
 * Uses Redis Geo-Spatial features to find nearby online drivers
 * and creates new trip requests in the system.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class TripRequestService {
    private static final String ONLINE_DRIVERS_GEO_KEY = "online_drivers_locations";
    private final TripRepository tripRepository;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Finds drivers within a specified radius using Redis Geo commands.
     *
     * @param latitude  User's latitude.
     * @param longitude User's longitude.
     * @param radiusKm  Search radius in kilometers.
     * @return List of nearby drivers with their coordinates and distance.
     */
    public List<NearbyDriversResponse> findNearbyDrivers(double latitude, double longitude, double radiusKm) {
        GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();

        Point center = new Point(longitude, latitude);
        Distance radius = new Distance(radiusKm, Metrics.KILOMETERS);
        Circle circle = new Circle(center, radius);

        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeCoordinates()
                .includeDistance()
                .sortAscending()
                .limit(10);

        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = geoOps.radius(
                ONLINE_DRIVERS_GEO_KEY,
                circle,
                args
        );

        return geoResults.getContent().stream()
                .map(result -> {
                    String driverIdStr = result.getContent().getName();
                    UUID driverId = UUID.fromString(driverIdStr);
                    Point driverPoint = result.getContent().getPoint();
                    double driverLatitude = driverPoint.getY();
                    double driverLongitude = driverPoint.getX();
                    double distanceKm = result.getDistance().getValue();

                    return new NearbyDriversResponse(driverId, driverLatitude, driverLongitude, distanceKm);
                }).toList();
    }

    /**
     * Creates a new trip request for a passenger.
     *
     * @param request     DTO containing start/end coordinates.
     * @param passengerId UUID of the passenger requesting the trip.
     * @return The newly created {@link Trip} entity with status REQUESTED.
     */
    public Trip requestTrip(TripRequest request, UUID passengerId) {
        Trip trips = Trip.builder()
                .requestedAt(Timestamp.valueOf(LocalDateTime.now()))
                .passengerId(passengerId)
                .startLatitude(request.startLatitude())
                .startLongitude(request.startLongitude())
                .endLatitude(request.endLatitude())
                .endLongitude(request.endLongitude())
                .tripStatus(TripStatus.REQUESTED)
                .build();
        return tripRepository.save(trips);
    }
}