package com.alpeerkaraca.driverservice.service;

import com.alpeerkaraca.driverservice.dto.DriverLocationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverLocationService {
    private static final String TOPIC_DRIVER_LOCATIONS = "driver_location_updates";
    private final KafkaTemplate<String, DriverLocationMessage> kafkaTemplate;

    public void publishDriverLocationMessage(UUID driverId, double latitude, double longitude) {
        DriverLocationMessage message = new DriverLocationMessage(
                driverId,
                latitude,
                longitude,
                Timestamp.valueOf(LocalDateTime.now())
        );
        kafkaTemplate.send(TOPIC_DRIVER_LOCATIONS, message);
    }

}
