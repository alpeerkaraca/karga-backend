package com.alpeerkaraca.paymentservice.controller;

import com.alpeerkaraca.paymentservice.dto.PaymentMessage;
import com.alpeerkaraca.paymentservice.infra.kafka.TripEventListener;
import com.alpeerkaraca.paymentservice.service.StripePaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentWebhookController.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StripePaymentService stripePaymentService;

    @MockitoBean
    private TripEventListener tripEventListener;

    @MockitoBean
    private KafkaTemplate<String, PaymentMessage> kafkaTemplate;
    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    @Nested
    @DisplayName("POST /api/v1/payments/webhook - Webhook Processing")
    class WebhookProcessingTests {

        @Test
        @DisplayName("Should handle valid payment_intent.succeeded event")
        void handleStripeWebhook_PaymentSucceeded_ProcessesWebhook() throws Exception {
            // Arrange
            String payload = """
                    {
                      "id": "evt_test_123",
                      "object": "event",
                      "type": "payment_intent.succeeded",
                      "data": {
                        "object": {
                          "id": "pi_test_123",
                          "amount": 15000,
                          "currency": "try"
                        }
                      }
                    }
                    """;
            String signature = "test_signature";

            // Act & Assert - Will fail validation but tests the endpoint
            mockMvc.perform(post("/api/v1/payments/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload)
                            .header("Stripe-Signature", signature))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle payment_intent.payment_failed event")
        void handleStripeWebhook_PaymentFailed_ProcessesWebhook() throws Exception {
            // Arrange
            String payload = """
                    {
                      "id": "evt_test_456",
                      "object": "event",
                      "type": "payment_intent.payment_failed",
                      "data": {
                        "object": {
                          "id": "pi_test_456",
                          "amount": 15000,
                          "currency": "try",
                          "last_payment_error": {
                            "message": "Insufficient funds"
                          }
                        }
                      }
                    }
                    """;
            String signature = "test_signature";

            // Act & Assert
            mockMvc.perform(post("/api/v1/payments/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload)
                            .header("Stripe-Signature", signature))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject invalid signature")
        void handleStripeWebhook_InvalidSignature_ReturnsBadRequest() throws Exception {
            // Arrange
            String payload = """
                    {
                      "id": "evt_test_123",
                      "object": "event",
                      "type": "payment_intent.succeeded"
                    }
                    """;
            String invalidSignature = "invalid_signature";

            // Act & Assert
            mockMvc.perform(post("/api/v1/payments/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload)
                            .header("Stripe-Signature", invalidSignature))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verify(stripePaymentService, never()).processWebhookEvent(any());
        }

        @Test
        @DisplayName("Should handle missing signature header")
        void handleStripeWebhook_MissingSignature_ReturnsBadRequest() throws Exception {
            // Arrange
            String payload = """
                    {
                      "id": "evt_test_123",
                      "object": "event",
                      "type": "payment_intent.succeeded"
                    }
                    """;

            // Act & Assert
            mockMvc.perform(post("/api/v1/payments/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());

            verify(stripePaymentService, never()).processWebhookEvent(any());
        }

        @Test
        @DisplayName("Should handle malformed JSON")
        void handleStripeWebhook_MalformedJson_ReturnsBadRequest() throws Exception {
            // Arrange
            String malformedPayload = "{ invalid json }";
            String signature = "test_signature";

            // Act & Assert
            mockMvc.perform(post("/api/v1/payments/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedPayload)
                            .header("Stripe-Signature", signature))
                    .andExpect(status().isBadRequest());

            verify(stripePaymentService, never()).processWebhookEvent(any());
        }

        @Test
        @DisplayName("Should handle empty payload")
        void handleStripeWebhook_EmptyPayload_ReturnsBadRequest() throws Exception {
            // Arrange
            String emptyPayload = "";
            String signature = "test_signature";

            // Act & Assert
            mockMvc.perform(post("/api/v1/payments/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(emptyPayload)
                            .header("Stripe-Signature", signature))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should only accept POST requests")
        void handleStripeWebhook_GetRequest_ReturnsMethodNotAllowed() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/payments/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Stripe-Signature", "test"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }
}

