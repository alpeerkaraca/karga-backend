package com.alpeerkaraca.userservice.model;

import com.alpeerkaraca.common.model.BaseInboxEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_inbox")
public class UserInbox extends BaseInboxEntity {
}
