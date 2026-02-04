package com.alpeerkaraca.driverservice.repository;

import com.alpeerkaraca.driverservice.model.DriverOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DriverOutboxRepository extends JpaRepository<DriverOutbox, UUID> {
}
