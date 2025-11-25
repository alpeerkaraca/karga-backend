package com.alpeerkaraca.driverservice.dto;

import com.alpeerkaraca.driverservice.model.DriverStatus;

public record DriverUpdateStatus(
        DriverStatus status,
        double longitude,
        double latitude
) {
}
