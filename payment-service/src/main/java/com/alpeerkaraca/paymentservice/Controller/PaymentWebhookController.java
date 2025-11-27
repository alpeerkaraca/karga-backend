package com.alpeerkaraca.paymentservice.Controller;

import com.alpeerkaraca.common.dto.ApiResponse;
import com.alpeerkaraca.paymentservice.service.StripePaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments/")
public class PaymentWebhookController {
    private final StripePaymentService stripePaymentService;

    @Value("${stripe.webhook.secret}")
    private String stripeWebhookSecret;
    @PostMapping(value = "/webhook", consumes = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<ApiResponse<String>> handleStripeWebhookData(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
        }
        catch (SignatureVerificationException e) {
            // Invalid signature
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Invalid signature"));
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Error parsing webhook"));
        }

        stripePaymentService.processWebhookEvent(event);

        return ResponseEntity.ok(ApiResponse.success(null, "Webhook processed successfully."));
    }

}
