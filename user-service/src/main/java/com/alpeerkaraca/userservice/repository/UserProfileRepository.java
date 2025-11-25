package com.alpeerkaraca.userservice.repository;

import com.alpeerkaraca.userservice.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByEmail(String email);

    @Query("SELECT u FROM UserProfile u WHERE u.userId = :id AND u.deletedAt IS NULL ")
    Optional<UserProfile> findDeletedById(@Param("id") UUID id);

}
