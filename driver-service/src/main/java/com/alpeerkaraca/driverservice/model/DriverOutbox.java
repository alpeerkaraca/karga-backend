package com.alpeerkaraca.driverservice.model;

import com.alpeerkaraca.common.model.BaseOutboxEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "driver_outbox")
public class DriverOutbox extends BaseOutboxEntity {
}
