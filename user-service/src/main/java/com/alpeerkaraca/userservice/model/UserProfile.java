package com.alpeerkaraca.userservice.model;

import com.alpeerkaraca.common.model.BaseClass;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SQLUpdate;

import java.util.UUID;

@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_profiles")
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE user_id = ?")
@SQLUpdate(sql = "UPDATE users SET updated_at = NOW() WHERE user_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class UserProfile extends BaseClass {
    @Id
    private UUID userId;
    private String firstName;
    private String lastName;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(unique = true, nullable = false)
    private String phoneNumber;
    private double rating;
}
