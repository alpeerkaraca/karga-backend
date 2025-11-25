package com.alpeerkaraca.driverservice.infra.kafka;

import com.alpeerkaraca.driverservice.dto.DriverLocationMessage;
import com.alpeerkaraca.driverservice.model.DriverStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocationConsumerService {
    private static final String KEY_DRIVER_STATUS = "driver:status:";
    private static final String KEY_ONLINE_DRIVERS_GEO = "online_drivers_locations";
    private static final String TOPIC_LOCATION_UPDATES = "driver_location_updates";
    private final RedisTemplate<String, String> redisTemplate;

    @KafkaListener(topics = TOPIC_LOCATION_UPDATES, groupId = "location_consumer_group")
    public void consumeLocationUpdate(DriverLocationMessage message) {
        String driverIdStr = message.driverId().toString();
        String statusKey = KEY_DRIVER_STATUS + driverIdStr;

        String status = redisTemplate.opsForValue().get(statusKey);

        if (DriverStatus.ONLINE.name().equals(status)) {
            redisTemplate.opsForGeo().add(
                    KEY_ONLINE_DRIVERS_GEO,
                    new Point(message.longitude(), message.latitude()),
                    driverIdStr
            );
        } else {
            log.warn("Çevrimdışı sürücü için güncelleme alındı:{}", driverIdStr);
        }
    }
}
