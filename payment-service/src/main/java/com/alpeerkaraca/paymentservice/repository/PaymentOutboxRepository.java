package com.alpeerkaraca.paymentservice.repository;

import com.alpeerkaraca.paymentservice.model.PaymentOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, UUID> {
}
