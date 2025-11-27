package com.alpeerkaraca.driverservice.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

public record TripMessage(
        String eventType,
        UUID tripId,
        UUID driverId,
        Timestamp timestamp,
        BigDecimal fare,
        UUID passengerId,
        double currentLongitude,
        double currentLatitude
) {
}
