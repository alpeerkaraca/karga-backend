package com.alpeerkaraca.driverservice.repository;

import com.alpeerkaraca.driverservice.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverRepository extends JpaRepository<Driver, UUID> {
    Optional<Driver> findDriverByDriverId(UUID driverId);
}
