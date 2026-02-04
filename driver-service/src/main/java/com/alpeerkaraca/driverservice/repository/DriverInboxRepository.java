package com.alpeerkaraca.driverservice.repository;

import com.alpeerkaraca.driverservice.model.DriverInbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverInboxRepository extends JpaRepository<DriverInbox, String> {
}
