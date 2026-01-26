package com.alpeerkaraca.authservice.model;

import com.alpeerkaraca.common.model.BaseInboxEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_inbox")
public class AuthInbox extends BaseInboxEntity {
}
