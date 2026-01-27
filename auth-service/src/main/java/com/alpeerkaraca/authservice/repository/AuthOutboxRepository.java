package com.alpeerkaraca.authservice.repository;

import com.alpeerkaraca.authservice.model.AuthOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuthOutboxRepository extends JpaRepository<AuthOutbox, UUID> {
}
