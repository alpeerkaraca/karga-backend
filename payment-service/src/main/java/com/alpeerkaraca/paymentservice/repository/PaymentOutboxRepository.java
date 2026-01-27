package com.alpeerkaraca.paymentservice.repository;

import com.alpeerkaraca.paymentservice.model.PaymentOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, UUID> {
}
