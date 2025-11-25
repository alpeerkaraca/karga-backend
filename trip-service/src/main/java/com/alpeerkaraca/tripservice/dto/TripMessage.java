package com.alpeerkaraca.tripservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripMessage {
    private String eventType; // TRIP_CREATED, TRIP_ACCEPTED, TRIP_COMPLETED, TRIP_CANCELLED
    private UUID tripId;
    private UUID driverId;
    private UUID passengerId;
    private Timestamp timestamp;
    private BigDecimal fare;
}

