package com.alpeerkaraca.tripservice.dto;

import jakarta.validation.constraints.NotNull;

public record NearbyDriversRequest(
        @NotNull
        double latitude,
        @NotNull
        double longitude
) {
}
