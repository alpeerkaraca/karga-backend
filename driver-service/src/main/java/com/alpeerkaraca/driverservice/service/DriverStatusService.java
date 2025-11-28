package com.alpeerkaraca.driverservice.service;

import com.alpeerkaraca.common.exception.InvalidStatusException;
import com.alpeerkaraca.driverservice.model.DriverStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for managing the real-time status and geographical location of drivers.
 * <p>
 * This class utilizes Redis Geo-Spatial features to position drivers on a map
 * and manages status transitions (ONLINE, BUSY, OFFLINE).
 * It is critical for the "Find Nearby Drivers" feature.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class DriverStatusService {
    private static final String KEY_DRIVER_STATUS = "driver:status:";
    private static final String KEY_ONLINE_DRIVERS_GEO = "online_drivers_locations";
    private static final String KEY_BUSY_DRIVERS_GEO = "busy_drivers_locations";
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Updates the driver's status and manages their geo-spatial index on Redis.
     *
     * <ul>
     * <li><b>ONLINE:</b> Driver is marked as available and added to the 'online_drivers_locations' geo index.</li>
     * <li><b>BUSY:</b> Driver is marked as busy (on a trip), removed from 'online' index, and moved to 'busy' index.</li>
     * <li><b>OFFLINE:</b> Driver is completely removed from the system (Redis keys deleted).</li>
     * </ul>
     *
     * @param driverId  The UUID of the driver.
     * @param status    The target status (ONLINE, OFFLINE, BUSY).
     * @param longitude Longitude (Required for ONLINE/BUSY status).
     * @param latitude  Latitude (Required for ONLINE/BUSY status).
     * @throws InvalidStatusException If location data is missing when switching to ONLINE or BUSY.
     */
    public void updateDriverStatus(UUID driverId, DriverStatus status, Double longitude, Double latitude) {
        String driverIdStr = driverId.toString();
        String statusKey = KEY_DRIVER_STATUS + driverIdStr;

        switch (status) {
            case ONLINE -> {
                if (latitude == null || longitude == null) {
                    throw new InvalidStatusException("Location data is required when going online.");
                }
                redisTemplate.opsForValue().set(statusKey, status.name());
                // Add to available drivers map
                redisTemplate.opsForGeo().add(
                        KEY_ONLINE_DRIVERS_GEO,
                        new Point(longitude, latitude),
                        driverIdStr
                );
            }
            case BUSY -> {
                if (latitude == null || longitude == null) {
                    throw new InvalidStatusException("Location data is required when status is busy.");
                }
                redisTemplate.opsForValue().set(statusKey, status.name());
                // Remove from available list, add to busy list (for analytics or tracking)
                redisTemplate.opsForGeo().remove(KEY_ONLINE_DRIVERS_GEO, driverIdStr);
                redisTemplate.opsForGeo().add(
                        KEY_BUSY_DRIVERS_GEO,
                        new Point(longitude, latitude),
                        driverIdStr
                );
            }
            case null, default -> {
                // Remove from everywhere when offline
                redisTemplate.delete(statusKey);
                redisTemplate.opsForGeo().remove(KEY_ONLINE_DRIVERS_GEO, driverIdStr);
                redisTemplate.opsForGeo().remove(KEY_BUSY_DRIVERS_GEO, driverIdStr);
            }
        }
    }
}