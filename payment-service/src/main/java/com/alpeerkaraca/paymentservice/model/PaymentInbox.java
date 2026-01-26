package com.alpeerkaraca.paymentservice.model;

import com.alpeerkaraca.common.model.BaseInboxEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_inbox")
public class PaymentInbox extends BaseInboxEntity {
}
