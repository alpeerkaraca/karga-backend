package com.alpeerkaraca.paymentservice.service;

import com.alpeerkaraca.paymentservice.AbstractIntegrationTest;
import com.alpeerkaraca.paymentservice.model.Payment;
import com.alpeerkaraca.paymentservice.model.PaymentStatus;
import com.alpeerkaraca.paymentservice.repository.PaymentRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.stripe.Stripe;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
class StripePaymentServiceWireMockTest extends AbstractIntegrationTest {

    private static WireMockServer wireMockServer;

    @MockitoBean
    private PaymentRepository paymentRepository;

    @Autowired
    private StripePaymentService stripePaymentService;

    @MockitoBean
    private RedisTemplate<String, String> redisTemplate;

    @BeforeAll
    static void setupWireMock() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        // Stripe'ın WireMock'a gitmesini sağlıyoruz
        Stripe.overrideApiBase("http://localhost:" + wireMockServer.port());
    }

    @AfterAll
    static void tearDownWireMock() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void configureStripeProperties(DynamicPropertyRegistry registry) {
        registry.add("stripe.apiKey", () -> "sk_test_mock_key");
        registry.add("stripe.webhook.secret", () -> "whsec_test_secret");
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
    }

    @Test
    @DisplayName("WireMock: Should create Stripe checkout session")
    void createPaymentSession_WithWireMock_CreatesSession() {
        // Arrange
        UUID tripId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(100.00);

        Payment mockPayment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .tripId(tripId)
                .passengerId(passengerId)
                .paymentAmount(amount)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        stubFor(post(urlEqualTo("/v1/checkout/sessions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": "cs_test_123",
                                  "object": "checkout.session",
                                  "url": "https://checkout.stripe.com/test",
                                  "payment_status": "unpaid"
                                }
                                """)));

        //(ACT) ---
        var response = stripePaymentService.createPaymentSession(tripId, passengerId, amount);

        // Assert
        verify(postRequestedFor(urlEqualTo("/v1/checkout/sessions")));
        assertThat(response.getStripeSessionUrl()).contains("checkout.stripe.com");
    }

    @Test
    @DisplayName("WireMock: Should handle Stripe API errors gracefully")
    void createPaymentSession_StripeError_HandlesGracefully() {
        // Arrange
        UUID tripId = UUID.randomUUID();
        UUID passengerId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(100.00);

        Payment mockPayment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .tripId(tripId)
                .passengerId(passengerId)
                .paymentAmount(amount)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        // Mock Stripe API error response
        stubFor(post(urlEqualTo("/v1/checkout/sessions"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "error": {
                                    "message": "Invalid request",
                                    "type": "invalid_request_error"
                                  }
                                }
                                """)));


        // ---(ACT & ASSERT) ---

        try {
            stripePaymentService.createPaymentSession(tripId, passengerId, amount);
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        verify(postRequestedFor(urlEqualTo("/v1/checkout/sessions")));
    }
}