package com.alpeerkaraca.authservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import java.util.UUID;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @Email
    @Column(unique = true)
    private String email;

    @Length(min = 8, message = "Password must be longer than 8 characters.")
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean isActive = false;
}
