package com.alpeerkaraca.userservice.repository;

import com.alpeerkaraca.userservice.model.UserOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserOutboxRepository extends JpaRepository<UserOutbox, UUID> {
}
