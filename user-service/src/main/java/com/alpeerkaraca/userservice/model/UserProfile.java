package com.alpeerkaraca.userservice.model;

import com.alpeerkaraca.common.model.BaseClass;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_profiles")
@SQLRestriction("deleted_at IS NULL")
public class UserProfile extends BaseClass {
    @Id
    private UUID userId;
    private String firstName;
    private String lastName;
    @Column(unique = true, nullable = false)
    @Email
    private String email;
    @Column(unique = true, nullable = false)
    private String phoneNumber;

    private double rating;
}
