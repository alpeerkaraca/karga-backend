package com.alpeerkaraca.userservice.model;

import com.alpeerkaraca.common.model.BaseOutboxEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_outbox")
public class UserOutbox extends BaseOutboxEntity {
}
