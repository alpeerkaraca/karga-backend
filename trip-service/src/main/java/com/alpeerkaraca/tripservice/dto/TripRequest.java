package com.alpeerkaraca.tripservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TripRequest(
        @NotNull @Min(-90) @Max(90)
        double startLatitude,

        @NotNull @Min(-180) @Max(180)
        double startLongitude,

        @NotNull @Min(-90) @Max(90)
        double endLatitude,

        @NotNull @Min(-180) @Max(180)
        double endLongitude
) {
}
