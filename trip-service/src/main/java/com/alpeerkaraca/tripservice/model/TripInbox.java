package com.alpeerkaraca.tripservice.model;

import com.alpeerkaraca.common.model.BaseInboxEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "trip_inbox")
public class TripInbox extends BaseInboxEntity {
}
