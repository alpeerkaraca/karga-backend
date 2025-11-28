package com.alpeerkaraca.driverservice.service;

import com.alpeerkaraca.driverservice.dto.DriverLocationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for processing real-time driver location data.
 * <p>
 * It accepts high-frequency GPS data from the mobile app and asynchronously pushes
 * it to a Kafka topic ('driver_location_updates').
 * This architecture prevents overloading the database.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class DriverLocationService {
    private static final String TOPIC_DRIVER_LOCATIONS = "driver_location_updates";
    private final KafkaTemplate<String, DriverLocationMessage> kafkaTemplate;

    /**
     * Publishes the driver's current location to the Kafka event stream.
     *
     * @param driverId  The ID of the driver updating their location.
     * @param latitude  Latitude value.
     * @param longitude Longitude value.
     */
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