package com.alpeerkaraca.common.event;

import com.alpeerkaraca.common.model.TripEventTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMessage {
    private String paymentId;
    private UUID tripId;
    private UUID passengerId;
    private BigDecimal amount;
    private TripEventTypes eventType;
    private String failureReason;
    private Instant createdAt;
}
