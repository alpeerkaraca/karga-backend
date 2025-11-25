package com.alpeerkaraca.paymentservice.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripeResponse {
    String status;
    String message;
    String sessionId;
    String sessionUrl;
}
