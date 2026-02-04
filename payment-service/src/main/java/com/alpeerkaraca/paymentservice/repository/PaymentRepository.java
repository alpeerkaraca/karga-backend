package com.alpeerkaraca.paymentservice.repository;

import com.alpeerkaraca.paymentservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByStripeSessionId(String stripeSessionId);

    Optional<Payment> findByTripId(UUID tripId);
}
