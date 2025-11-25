package com.alpeerkaraca.tripservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "trips")
public class Trip {
    @Id
    @GeneratedValue
    private UUID tripId;
    private double startLatitude;
    private double startLongitude;
    private String startAddress;
    private double endLatitude;
    private double endLongitude;
    private String endAddress;
    private Timestamp requestedAt;
    private Timestamp startedAt;
    private Timestamp endedAt;
    @Enumerated(EnumType.STRING)
    private TripStatus tripStatus;
    private BigDecimal fare;
    private UUID passengerId;
    private UUID driverId;
}
