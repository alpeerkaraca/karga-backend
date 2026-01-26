package com.alpeerkaraca.common.event;

import com.alpeerkaraca.common.model.TripEventTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripMessage {
    TripEventTypes eventType;
    UUID tripId;
    UUID driverId;
    Instant createdAt = Instant.now();
    BigDecimal fare;
    UUID passengerId;
    double currentLongitude;
    double currentLatitude;
}
