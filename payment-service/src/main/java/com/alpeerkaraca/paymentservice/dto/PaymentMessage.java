package com.alpeerkaraca.paymentservice.dto;


import com.alpeerkaraca.paymentservice.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMessage {
    UUID paymentId;
    BigDecimal paymentFare;
    PaymentStatus paymentStatus;
}
