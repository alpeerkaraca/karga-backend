package com.alpeerkaraca.authservice.repository;

import com.alpeerkaraca.authservice.model.AuthInbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthInboxRepository extends JpaRepository<AuthInbox, String> {
}
