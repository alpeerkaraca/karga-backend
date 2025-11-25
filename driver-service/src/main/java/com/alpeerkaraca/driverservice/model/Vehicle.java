package com.alpeerkaraca.driverservice.model;

import com.alpeerkaraca.common.model.BaseClass;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Vehicle extends BaseClass {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID vehicleId;

    private String brand;
    private String model;
    @Column(unique = true, nullable = false)
    private String plate;
    private String color;
    private String year;

}
