package com.alpeerkaraca.driverservice.infra.kafka;

import com.alpeerkaraca.driverservice.dto.TripMessage;
import com.alpeerkaraca.driverservice.model.DriverStatus;
import com.alpeerkaraca.driverservice.service.DriverStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TripEventConsumerService {
    private static final String TOPIC_TRIP_EVENTS = "trip_events";
    private final DriverStatusService driverStatusService;

    @KafkaListener(topics = TOPIC_TRIP_EVENTS, groupId = "driver_status_group")
    public void handleTripEvent(TripMessage message) {
        try {
            String eventType = message.eventType();

            if ("TRIP_ACCEPTED".equals(eventType) || "TRIP_STARTED".equals(eventType)) {
                UUID driverId = message.driverId();
                log.info("Yolculuk kabul edildi/başladı olayı alındı. Sürücü {} BUSY olarak ayarlanıyor.", driverId);
                driverStatusService.updateDriverStatus(driverId, DriverStatus.BUSY, null, null);
            } else if ("TRIP_COMPLETED".equals(eventType) || "TRIP_CANCELED".equals(eventType)) {
                UUID driverId = message.driverId();
                log.info("Yolculuk tamamlandı/iptal edildi olayı alındı. Sürücü {} ONLINE olarak ayarlanıyor.", driverId);
                driverStatusService.updateDriverStatus(driverId, DriverStatus.ONLINE, null, null);
            } else {
                log.warn("Bilinmeyen yolculuk olayı türü alındı: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Yolculuk olayı işlenirken hata oluştu: {}", message, e);

        }

    }
}
