package com.alpeerkaraca.tripservice.repository;

import com.alpeerkaraca.tripservice.model.Trip;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {
    @Query("SELECT t FROM Trip t WHERE t.tripStatus = 'REQUESTED'")
    List<Trip> findAvailableTrips();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trip t WHERE t.tripId = :tripId")
    Optional<Trip> findByIdForUpdate(UUID tripId);
}
