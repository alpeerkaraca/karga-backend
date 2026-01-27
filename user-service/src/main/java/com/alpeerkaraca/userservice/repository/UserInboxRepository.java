package com.alpeerkaraca.userservice.repository;

import com.alpeerkaraca.userservice.model.UserInbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserInboxRepository extends JpaRepository<UserInbox, String> {
}
