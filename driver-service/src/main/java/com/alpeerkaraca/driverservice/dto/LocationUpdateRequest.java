package com.alpeerkaraca.driverservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record LocationUpdateRequest(
        @Min(-90) @Max(90)
        double latitude,
        @Min(-180) @Max(180)
        double longitude
) {
}
