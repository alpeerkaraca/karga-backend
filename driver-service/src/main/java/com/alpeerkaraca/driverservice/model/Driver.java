package com.alpeerkaraca.driverservice.model;

import com.alpeerkaraca.common.model.BaseClass;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "drivers")
public class Driver extends BaseClass {
    @Id
    private UUID driverId;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    private boolean isApproved = false;

    private boolean isActive = false;
}
