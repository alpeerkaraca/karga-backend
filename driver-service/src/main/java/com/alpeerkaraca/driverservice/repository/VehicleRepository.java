package com.alpeerkaraca.driverservice.repository;

import com.alpeerkaraca.driverservice.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
}
