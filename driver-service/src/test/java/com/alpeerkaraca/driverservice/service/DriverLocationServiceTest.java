package com.alpeerkaraca.driverservice.service;

import com.alpeerkaraca.driverservice.dto.DriverLocationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DriverLocationServiceTest {

    @Mock
    private KafkaTemplate<String, DriverLocationMessage> kafkaTemplate;

    @InjectMocks
    private DriverLocationService driverLocationService;

    private UUID testDriverId;

    @BeforeEach
    void setUp() {
        testDriverId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should publish driver location message to Kafka")
    void publishDriverLocationMessage_ValidData_Success() {
        // Arrange
        double latitude = 41.0082;
        double longitude = 28.9784;

        // Act
        driverLocationService.publishDriverLocationMessage(testDriverId, latitude, longitude);

        // Assert
        verify(kafkaTemplate).send(eq("driver_location_updates"), argThat(message ->
                message.driverId().equals(testDriverId) &&
                        message.latitude() == latitude &&
                        message.longitude() == longitude &&
                        message.timestamp() != null
        ));
    }

    @Test
    @DisplayName("Should publish location with different coordinates")
    void publishDriverLocationMessage_DifferentCoordinates_Success() {
        // Arrange
        double latitude = 40.7128;
        double longitude = -74.0060;

        // Act
        driverLocationService.publishDriverLocationMessage(testDriverId, latitude, longitude);

        // Assert
        verify(kafkaTemplate).send(eq("driver_location_updates"), argThat(message ->
                message.driverId().equals(testDriverId) &&
                        message.latitude() == latitude &&
                        message.longitude() == longitude
        ));
    }

    @Test
    @DisplayName("Should publish multiple location updates")
    void publishDriverLocationMessage_MultipleUpdates_Success() {
        // Act
        driverLocationService.publishDriverLocationMessage(testDriverId, 41.0, 28.0);
        driverLocationService.publishDriverLocationMessage(testDriverId, 41.1, 28.1);
        driverLocationService.publishDriverLocationMessage(testDriverId, 41.2, 28.2);

        // Assert
        verify(kafkaTemplate).send(eq("driver_location_updates"), argThat(message ->
                message.latitude() == 41.0 && message.longitude() == 28.0
        ));
        verify(kafkaTemplate).send(eq("driver_location_updates"), argThat(message ->
                message.latitude() == 41.1 && message.longitude() == 28.1
        ));
        verify(kafkaTemplate).send(eq("driver_location_updates"), argThat(message ->
                message.latitude() == 41.2 && message.longitude() == 28.2
        ));
    }
}

