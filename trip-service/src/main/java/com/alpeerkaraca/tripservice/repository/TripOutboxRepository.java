package com.alpeerkaraca.tripservice.repository;

import com.alpeerkaraca.tripservice.model.TripOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TripOutboxRepository extends JpaRepository<TripOutbox, UUID> {
    List<TripOutbox> findByProcessedFalse();
}
