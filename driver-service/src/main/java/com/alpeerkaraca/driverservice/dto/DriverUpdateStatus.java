package com.alpeerkaraca.driverservice.dto;

import com.alpeerkaraca.driverservice.model.DriverStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DriverUpdateStatus(
        DriverStatus status,
        @NotNull(message = "Longitude is required")
        @Min(value = -180, message = "Longitude must be between -180 and 180")
        @Max(value = 180, message = "Longitude must be between -180 and 180")
        Double longitude,
        @NotNull(message = "Latitude is required")
        @Min(value = -90, message = "Latitude must be between -90 and 90")
        @Max(value = 90, message = "Latitude must be between -90 and 90")
        Double latitude
) {
}
