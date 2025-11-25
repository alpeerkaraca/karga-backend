package com.alpeerkaraca.tripservice.dto;

import java.util.UUID;

public record NearbyDriversResponse(
        UUID driverId,
        double latitude,
        double longitude,
        double distanceKm
) {
}
