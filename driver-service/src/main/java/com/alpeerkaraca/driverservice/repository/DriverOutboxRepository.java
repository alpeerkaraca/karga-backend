package com.alpeerkaraca.driverservice.repository;

import com.alpeerkaraca.driverservice.model.DriverOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DriverOutboxRepository extends JpaRepository<DriverOutbox, UUID> {
}
