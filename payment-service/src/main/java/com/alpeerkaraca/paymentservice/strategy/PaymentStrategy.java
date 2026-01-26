package com.alpeerkaraca.paymentservice.strategy;

import com.alpeerkaraca.paymentservice.model.Payment;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentStrategy {
    Payment pay(UUID tripId, UUID passengerId, BigDecimal paymentAmount);
}
