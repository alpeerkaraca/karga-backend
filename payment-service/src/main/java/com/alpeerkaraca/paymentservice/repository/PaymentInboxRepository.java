package com.alpeerkaraca.paymentservice.repository;

import com.alpeerkaraca.paymentservice.model.PaymentInbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentInboxRepository extends JpaRepository<PaymentInbox, String> {
}
