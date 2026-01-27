package com.alpeerkaraca.paymentservice.model;

import com.alpeerkaraca.common.model.BaseOutboxEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_outbox")
public class PaymentOutbox extends BaseOutboxEntity {
}
