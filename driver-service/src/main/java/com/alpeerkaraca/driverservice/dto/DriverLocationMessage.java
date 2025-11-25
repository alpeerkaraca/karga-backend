package com.alpeerkaraca.driverservice.dto;

import java.sql.Timestamp;
import java.util.UUID;

public record DriverLocationMessage(
        UUID driverId,
        double latitude,
        double longitude,
        Timestamp timestamp
) {
}
