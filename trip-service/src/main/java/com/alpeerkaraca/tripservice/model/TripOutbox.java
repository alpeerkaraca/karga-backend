package com.alpeerkaraca.tripservice.model;

import com.alpeerkaraca.common.model.BaseOutboxEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "trip_outbox")
public class TripOutbox extends BaseOutboxEntity {
}
