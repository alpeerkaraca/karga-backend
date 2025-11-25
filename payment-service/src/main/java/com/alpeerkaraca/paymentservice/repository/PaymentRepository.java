package com.alpeerkaraca.paymentservice.repository;

import com.alpeerkaraca.paymentservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
}
