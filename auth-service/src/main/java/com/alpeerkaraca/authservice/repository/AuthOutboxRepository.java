package com.alpeerkaraca.authservice.repository;

import com.alpeerkaraca.authservice.model.AuthOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuthOutboxRepository extends JpaRepository<AuthOutbox, UUID> {
}
