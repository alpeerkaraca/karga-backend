# 💳 Payment Service

Payment processing service for the Karga ride-hailing platform. Handles Stripe payment integration, processes trip payments, and manages Stripe webhook events.

## 📋 Overview

The Payment Service is responsible for:
- Processing payments for completed trips via Stripe
- Handling Stripe webhook events (payment confirmations, failures)
- Consuming trip completion events from Kafka
- Creating payment records and tracking payment status
- Refund processing for cancelled trips (if applicable)

**Port:** `8086`  
**Database:** `payment_db` (PostgreSQL)  
**Payment Gateway:** Stripe API

## 🏗️ Architecture

- **Framework:** Spring Boot 3.5.x
- **Security:** Spring Security with JWT (RSA256 public key from Vault)
- **Database:** PostgreSQL 18
- **Payment Gateway:** Stripe API (for payment processing)
- **Message Broker:** Apache Kafka (consumes `trip_events`)
- **Secrets Management:** HashiCorp Vault (AppRole authentication)

## 🚀 API Endpoints

### Base URL
```
http://localhost:8086/api/v1/payments
```

### 1. Stripe Webhook Handler
**POST** `/webhook`

Handles webhook events from Stripe for payment status updates.

**Headers:**
```
Content-Type: application/json
Stripe-Signature: <signature>
```

**Request Body:**
Stripe sends JSON payload with event data.

**Response:**
```json
{
  "success": true,
  "message": "Webhook processed successfully.",
  "data": null
}
```

**Status Codes:**
- `200 OK` - Webhook processed successfully
- `400 Bad Request` - Invalid signature or malformed payload

**Supported Webhook Events:**
- `payment_intent.succeeded` - Payment completed successfully
- `payment_intent.failed` - Payment failed
- `charge.refunded` - Refund processed

**Important:** This endpoint must be publicly accessible for Stripe to send webhooks. Configure your firewall/reverse proxy accordingly.

## 🔧 Configuration

### Environment Variables

#### Vault Configuration
```properties
VAULT_HOST=vault-server
VAULT_PORT=8200
VAULT_SCHEME=https
VAULT_TRUSTSTORE_PASSWORD=your-truststore-password
VAULT_ROLE_ID=your-payment-service-role-id
VAULT_SECRET_ID=your-payment-service-secret-id
```

#### Database Configuration (from Vault)
Vault path: `secret/payment-service`
- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

#### Stripe Configuration (from Vault)
Vault path: `secret/payment-service`
- `stripe.api.key` - Stripe API key (secret key, not publishable key)
- `stripe.webhook.secret` - Stripe webhook signing secret

#### JWT Configuration (from Vault)
Vault path: `secret/application`
- `jwt.public.key` - RSA public key for token verification

#### Kafka Configuration (from Vault)
Vault path: `secret/application`
- `spring.kafka.bootstrap-servers`

### Application Properties

Key configurations in `application.properties`:

```properties
spring.application.name=payment-service
server.port=8086

# Kafka Consumer (consumes trip events)
spring.kafka.consumer.group-id=payment-service-group
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
```

## 📥 Kafka Events

### Consumed Topics

#### `trip_events`
Consumed to process payments when trips are completed.

**Relevant Event:**
- `TRIP_COMPLETED` → Creates Stripe payment intent and charges passenger

**Event Schema:**
```json
{
  "tripId": "uuid",
  "eventType": "TRIP_COMPLETED",
  "passengerId": "uuid",
  "driverId": "uuid",
  "amount": 45.50,
  "timestamp": "2026-02-04T12:00:00Z"
}
```

**Processing Flow:**
1. Consume `TRIP_COMPLETED` event from Kafka
2. Create Stripe Payment Intent with trip amount
3. Charge passenger's payment method
4. Store payment record in `payment_db`
5. Wait for Stripe webhook confirmation

## 💳 Stripe Integration

### Setup Stripe

1. **Create a Stripe account** at [stripe.com](https://stripe.com)

2. **Get your API keys:**
   - Go to [Stripe Dashboard → Developers → API keys](https://dashboard.stripe.com/test/apikeys)
   - Copy your **Secret Key** (starts with `sk_test_` or `sk_live_`)

3. **Set up webhook endpoint:**
   - Go to [Stripe Dashboard → Developers → Webhooks](https://dashboard.stripe.com/test/webhooks)
   - Click "Add endpoint"
   - URL: `https://your-domain.com/api/v1/payments/webhook`
   - Select events to listen for:
     - `payment_intent.succeeded`
     - `payment_intent.payment_failed`
     - `charge.refunded`
   - Copy the **Webhook Signing Secret** (starts with `whsec_`)

4. **Store secrets in Vault:**
   ```bash
   vault kv put secret/payment-service \
     stripe.api.key="sk_test_your_secret_key" \
     stripe.webhook.secret="whsec_your_webhook_secret"
   ```

### Testing with Stripe Test Mode

Stripe provides test cards for development:

**Successful Payment:**
```
Card Number: 4242 4242 4242 4242
Expiry: Any future date (e.g., 12/34)
CVC: Any 3 digits (e.g., 123)
ZIP: Any 5 digits (e.g., 12345)
```

**Payment Declined:**
```
Card Number: 4000 0000 0000 0002
```

**For more test cards:** [Stripe Testing Documentation](https://stripe.com/docs/testing)

### Webhook Testing Locally

Use Stripe CLI to forward webhooks to your local development environment:

```bash
# Install Stripe CLI
brew install stripe/stripe-cli/stripe  # macOS
# or download from https://stripe.com/docs/stripe-cli

# Login to Stripe
stripe login

# Forward webhooks to local service
stripe listen --forward-to localhost:8086/api/v1/payments/webhook
```

The CLI will output a webhook signing secret like `whsec_...`. Use this for local development.

## 🐳 Running with Docker

### Using Docker Compose (Recommended)

```bash
# From project root
docker-compose up --build -d payment-service
```

### Standalone Docker

```bash
# Build the image
cd payment-service
docker build -t karga/payment-service:latest .

# Run the container
docker run -d \
  --name payment-service \
  -p 8086:8086 \
  -e VAULT_HOST=vault-server \
  -e VAULT_ROLE_ID=your-role-id \
  -e VAULT_SECRET_ID=your-secret-id \
  karga/payment-service:latest
```

## 🛠️ Local Development

### Prerequisites
- Java 21
- Maven 3.9+
- PostgreSQL 18
- Kafka
- HashiCorp Vault (configured with payment-service AppRole)
- Stripe account with test API keys

### Build the Service

```bash
# Install karga-common dependency first
cd ../karga-common
mvn clean install

# Build payment-service
cd ../payment-service
mvn clean package -DskipTests
```

### Run the Service

```bash
mvn spring-boot:run
```

## 🧪 Testing

### Run Unit Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn verify
```

Integration tests use Testcontainers and Stripe test mode.

### Manual Testing

**1. Simulate trip completion event** (publish to Kafka):
```bash
# Using Kafka console producer
kafka-console-producer --bootstrap-server localhost:9092 --topic trip_events

# Paste this JSON:
{
  "tripId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "TRIP_COMPLETED",
  "passengerId": "123e4567-e89b-12d3-a456-426614174000",
  "driverId": "789e0123-e89b-12d3-a456-426614174000",
  "amount": 45.50,
  "timestamp": "2026-02-04T12:00:00Z"
}
```

**2. Verify payment created in database:**
```sql
SELECT * FROM payments WHERE trip_id = '550e8400-e29b-41d4-a716-446655440000';
```

**3. Test webhook with Stripe CLI:**
```bash
stripe trigger payment_intent.succeeded
```

## 📊 Monitoring

### Health Check
```bash
curl http://localhost:8086/actuator/health
```

### View Logs
```bash
# Docker
docker-compose logs -f payment-service

# Kubernetes
kubectl logs -f deployment/payment-service -n app-prod
```

### Check Payment Records
```sql
-- Connect to payment_db
psql -h localhost -U payment_user -d payment_db

-- View recent payments
SELECT trip_id, amount, status, created_at 
FROM payments 
ORDER BY created_at DESC 
LIMIT 10;
```

## 🔒 Security Features

- **Webhook Signature Verification:** All Stripe webhooks are verified using `Stripe-Signature` header
- **Vault Integration:** Stripe API keys and webhook secrets stored in Vault
- **PCI Compliance:** No credit card data stored in the application (handled by Stripe)
- **Idempotency:** Payment processing is idempotent to prevent duplicate charges

## 🚨 Troubleshooting

### Common Issues

**1. Webhook Signature Verification Failed**
```
Error: Invalid signature
```
**Solution:**
- Verify `stripe.webhook.secret` in Vault matches the secret from Stripe Dashboard
- Ensure the webhook payload is not modified (e.g., by a proxy)
- Check that the `Stripe-Signature` header is being forwarded correctly

**2. Payment Intent Creation Failed**
```
Error: Invalid API Key provided
```
**Solution:**
- Verify `stripe.api.key` in Vault is correct
- Ensure you're using the **Secret Key** (starts with `sk_`), not the Publishable Key
- Check if you're in test mode vs. production mode

**3. Trip Completed Event Not Consumed**
```
Issue: Payment not created after trip completion
```
**Solution:**
- Verify Kafka consumer is running: Check logs for "Kafka consumer started"
- Ensure `trip_events` topic exists: `kafka-topics --list --bootstrap-server localhost:9092`
- Check if Debezium connector is configured for outbox pattern
- Verify consumer group offset: `kafka-consumer-groups --describe --group payment-service-group`

**4. Webhook Endpoint Not Reachable**
```
Issue: Stripe shows webhook delivery failures
```
**Solution:**
- Ensure the service is publicly accessible (use ngrok for local testing)
- Verify firewall rules allow incoming HTTPS traffic on port 8086
- Check nginx/reverse proxy configuration
- For local development, use Stripe CLI: `stripe listen --forward-to localhost:8086/api/v1/payments/webhook`

## 📚 Related Documentation

- [Main README](../README.md) - Project overview and setup
- [Trip Service](../trip-service/README.md) - Trip lifecycle and completion events
- [Vault Setup Guide](../vault/certs/README.md) - Certificate and Vault configuration
- [Stripe API Documentation](https://stripe.com/docs/api) - Official Stripe API docs
- [Stripe Webhooks Guide](https://stripe.com/docs/webhooks) - How to handle webhooks
