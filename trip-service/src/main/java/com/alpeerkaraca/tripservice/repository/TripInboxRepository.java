package com.alpeerkaraca.tripservice.repository;

import com.alpeerkaraca.tripservice.model.TripInbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripInboxRepository extends JpaRepository<TripInbox, String> {
}
