package com.alpeerkaraca.paymentservice.infra.kafka;

import com.alpeerkaraca.paymentservice.dto.PaymentMessage;
import com.alpeerkaraca.paymentservice.dto.TripMessage;
import com.alpeerkaraca.paymentservice.service.StripePaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TripEventListener {
    private static final String TOPIC_TRIP_EVENTS = "trip_events";
    private final KafkaTemplate<String, PaymentMessage> kafkaTemplate;
    private final StripePaymentService paymentService;

    @KafkaListener(topics = TOPIC_TRIP_EVENTS, groupId = "payment-service-group")
    public void handleTripEvent(TripMessage message) {
        if ("TRIP_COMPLETED".equals(message.getEventType())) {
            log.info("Trip completed event received: {}", message.getTripId());

            paymentService.createPaymentSession(
                    message.getTripId(),
                    message.getPassengerId(),
                    message.getFare()
            );
        }
    }
}
