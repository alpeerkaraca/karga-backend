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
//@SQLDelete(sql = "UPDATE drivers SET deleted_at = NOW() WHERE driver_id = ?")
public class Driver extends BaseClass {
    @Id
    private UUID driverId;

    @Column(name = "user_id", unique = true)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    private boolean isApproved = false;

    private boolean isActive = false;
}
