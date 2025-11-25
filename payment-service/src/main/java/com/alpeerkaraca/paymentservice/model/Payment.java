package com.alpeerkaraca.paymentservice.model;

import com.alpeerkaraca.common.model.BaseClass;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Payment extends BaseClass {
    @Id
    @GeneratedValue
    private UUID paymentId;

    private UUID tripId;
    private UUID passengerId;

    private BigDecimal paymentAmount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private Timestamp paidAt;

    private String stripeSessionId;
    @Column(columnDefinition = "TEXT")
    private String stripeSessionUrl;

}
