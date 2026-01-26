package com.alpeerkaraca.authservice.model;

import com.alpeerkaraca.common.model.BaseOutboxEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_outbox")
public class AuthOutbox extends BaseOutboxEntity {
}
