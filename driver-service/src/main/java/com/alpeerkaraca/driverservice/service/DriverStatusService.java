package com.alpeerkaraca.driverservice.service;

import com.alpeerkaraca.driverservice.model.DriverStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverStatusService {
    private static final String KEY_DRIVER_STATUS = "driver:status:";
    private static final String KEY_ONLINE_DRIVERS_GEO = "online_drivers_locations";
    private final RedisTemplate<String, String> redisTemplate;

    public void updateDriverStatus(UUID driverId, DriverStatus status, Double longitude, Double latitude) {
        String driverIdStr = driverId.toString();
        String statusKey = KEY_DRIVER_STATUS + driverIdStr;

        if (status == DriverStatus.ONLINE) {
            if (latitude == null || longitude == null) {
                throw new IllegalArgumentException("Çevrimiçi olurken konum bilgisi zorunludur.");
            }
            redisTemplate.opsForValue().set(statusKey, status.name());
            redisTemplate.opsForGeo().add(
                    KEY_ONLINE_DRIVERS_GEO,
                    new Point(longitude, latitude),
                    driverIdStr
            );
        } else {
            redisTemplate.delete(statusKey);
            redisTemplate.opsForGeo().remove(KEY_ONLINE_DRIVERS_GEO, driverIdStr);
        }
    }

}
