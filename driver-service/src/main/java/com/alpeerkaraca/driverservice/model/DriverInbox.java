package com.alpeerkaraca.driverservice.model;

import com.alpeerkaraca.common.model.BaseInboxEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "driver_inbox")
public class DriverInbox extends BaseInboxEntity {
}
