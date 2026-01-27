package com.alpeerkaraca.tripservice.repository;

import com.alpeerkaraca.tripservice.model.TripInbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripInboxRepository extends JpaRepository<TripInbox, String> {
}
