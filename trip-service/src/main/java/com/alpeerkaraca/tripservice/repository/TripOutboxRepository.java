package com.alpeerkaraca.tripservice.repository;

import com.alpeerkaraca.tripservice.model.TripOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripOutboxRepository extends JpaRepository<TripOutbox, UUID> {
    List<TripOutbox> findByProcessedFalse();
    Optional<TripOutbox> findFirstByOrderByCreatedAtDesc();
}
