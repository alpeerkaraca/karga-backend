# 👤 User Service

User profile management service for the Karga ride-hailing platform. Handles passenger profile creation, updates, and consumes user creation events from the auth service.

## 📋 Overview

The User Service is responsible for:
- Managing passenger user profiles
- Consuming `user_created` events from Kafka (published by auth-service)
- Providing user profile retrieval and update endpoints
- Syncing user data across services via event-driven architecture

**Port:** `8088`  
**Database:** `user_db` (PostgreSQL)

## 🏗️ Architecture

- **Framework:** Spring Boot 3.5.x
- **Security:** Spring Security with JWT (RSA256 public key from Vault)
- **Database:** PostgreSQL 18
- **Message Broker:** Apache Kafka (consumes `user_created` topic)
- **Secrets Management:** HashiCorp Vault (AppRole authentication)
- **Pattern:** Event-driven architecture (Saga pattern for user creation)

## 🚀 API Endpoints

### Base URL
```
http://localhost:8088/api/v1/users
```

All endpoints require JWT authentication via `Authorization: Bearer <token>` header.

### 1. Get My Profile
**GET** `/me`

Retrieves the authenticated user's profile information.

**Headers:**
```
Authorization: Bearer <JWT_ACCESS_TOKEN>
```

**Response:**
```json
{
  "success": true,
  "message": "User profile retrieved successfully.",
  "data": {
    "userId": "uuid",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+905551234567",
    "createdAt": "2026-02-04T12:00:00Z",
    "updatedAt": "2026-02-04T12:00:00Z"
  }
}
```

**Status Codes:**
- `200 OK` - Profile retrieved successfully
- `401 Unauthorized` - Invalid or missing JWT token
- `404 Not Found` - User profile not found

### 2. Update My Profile
**PUT** `/me`

Updates the authenticated user's profile information.

**Headers:**
```
Authorization: Bearer <JWT_ACCESS_TOKEN>
Content-Type: application/json
```

**Request Body:**
```json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "phoneNumber": "+905559876543"
}
```

**Note:** All fields are optional. Only provide fields you want to update.

**Response:**
```json
{
  "success": true,
  "message": "User profile updated successfully.",
  "data": {
    "userId": "uuid",
    "email": "user@example.com",
    "firstName": "Jane",
    "lastName": "Smith",
    "phoneNumber": "+905559876543",
    "updatedAt": "2026-02-04T13:30:00Z"
  }
}
```

**Status Codes:**
- `200 OK` - Profile updated successfully
- `400 Bad Request` - Validation error or empty request body
- `401 Unauthorized` - Invalid or missing JWT token

## 🔧 Configuration

### Environment Variables

#### Vault Configuration
```properties
VAULT_HOST=vault-server
VAULT_PORT=8200
VAULT_SCHEME=https
VAULT_TRUSTSTORE_PASSWORD=your-truststore-password
VAULT_ROLE_ID=your-user-service-role-id
VAULT_SECRET_ID=your-user-service-secret-id
```

#### Database Configuration (from Vault)
Vault path: `secret/user-service`
- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

#### JWT Configuration (from Vault)
Vault path: `secret/application`
- `jwt.public.key` - RSA public key for token verification

#### Kafka Configuration (from Vault)
Vault path: `secret/application`
- `spring.kafka.bootstrap-servers`

### Application Properties

Key configurations in `application.properties`:

```properties
spring.application.name=user-service
server.port=8088

# Kafka Consumer (consumes user_created events)
spring.kafka.consumer.group-id=user-service-group
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*
```

## 📥 Kafka Events

### Consumed Topics

#### `user_created`
Consumed to create user profiles when new users register via auth-service.

**Event Schema:**
```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+905551234567",
  "createdAt": "2026-02-04T12:00:00Z"
}
```

**Processing Flow:**
1. Auth Service creates user account and publishes `user_created` event
2. User Service consumes event from Kafka
3. Creates user profile in `user_db` with the same `userId`
4. User can now access their profile via `/me` endpoint

**Why Event-Driven?**
- **Decoupling:** Auth service doesn't need to directly call user service
- **Resilience:** If user service is down, event remains in Kafka
- **Scalability:** Multiple services can consume the same event
- **Saga Pattern:** Distributed transaction across services

## 🐳 Running with Docker

### Using Docker Compose (Recommended)

```bash
# From project root
docker-compose up --build -d user-service
```

### Standalone Docker

```bash
# Build the image
cd user-service
docker build -t karga/user-service:latest .

# Run the container
docker run -d \
  --name user-service \
  -p 8088:8088 \
  -e VAULT_HOST=vault-server \
  -e VAULT_ROLE_ID=your-role-id \
  -e VAULT_SECRET_ID=your-secret-id \
  karga/user-service:latest
```

## 🛠️ Local Development

### Prerequisites
- Java 21
- Maven 3.9+
- PostgreSQL 18
- Kafka
- HashiCorp Vault (configured with user-service AppRole)

### Build the Service

```bash
# Install karga-common dependency first
cd ../karga-common
mvn clean install

# Build user-service
cd ../user-service
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

Integration tests use Testcontainers for PostgreSQL and Kafka.

### Manual Testing with cURL

**1. Register a new user** (via auth-service):
```bash
curl -X POST http://localhost:8084/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@karga.com",
    "password": "SecurePass123!",
    "firstName": "Test",
    "lastName": "User",
    "phoneNumber": "+905551234567"
  }'
```

**2. Login to get JWT token:**
```bash
TOKEN=$(curl -s -X POST http://localhost:8084/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@karga.com","password":"SecurePass123!"}' \
  | jq -r '.data.accessToken')
```

**3. Get user profile:**
```bash
curl -X GET http://localhost:8088/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN"
```

**4. Update user profile:**
```bash
curl -X PUT http://localhost:8088/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Updated",
    "phoneNumber": "+905559999999"
  }'
```

## 📊 Monitoring

### Health Check
```bash
curl http://localhost:8088/actuator/health
```

### View Logs
```bash
# Docker
docker-compose logs -f user-service

# Kubernetes
kubectl logs -f deployment/user-service -n app-prod
```

### Check Kafka Consumer Status
```bash
# Check consumer group offset
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group user-service-group

# View user_created topic messages
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic user_created --from-beginning
```

## 🔒 Security Features

- **JWT Authentication:** All endpoints require valid JWT token
- **User ID Extraction:** User ID extracted from JWT claims (prevents accessing other users' data)
- **Vault Integration:** All secrets retrieved from HashiCorp Vault
- **Input Validation:** Jakarta Validation for all request bodies
- **Read Your Own Data:** Users can only access/update their own profile

## 🚨 Troubleshooting

### Common Issues

**1. User Profile Not Found After Registration**
```
Error: 404 Not Found when calling /me
```
**Solution:**
- Check if Kafka consumer is running: `docker-compose logs -f user-service | grep "Kafka"`
- Verify `user_created` event was published: Check auth-service logs
- Check consumer group offset is advancing: `kafka-consumer-groups --describe --group user-service-group`
- Ensure Debezium connector is configured (if using outbox pattern)

**2. Kafka Consumer Not Starting**
```
Error: Failed to connect to Kafka broker
```
**Solution:**
- Verify Kafka is running: `docker-compose ps kafka`
- Check bootstrap servers configuration in Vault: `secret/application`
- Ensure Kafka is reachable from the container network

**3. Empty Request Body Error**
```
Error: At least one field must be provided
```
**Solution:** When updating profile, you must provide at least one field (firstName, lastName, or phoneNumber).

**4. Database Connection Error**
```
Error: Unable to acquire JDBC Connection
```
**Solution:**
- Ensure PostgreSQL is running: `docker-compose ps user-db`
- Verify database credentials in Vault at `secret/user-service`
- Check database URL and port configuration

## 🔄 Event-Driven Flow

```
┌─────────────┐      user_created      ┌──────────────┐
│Auth Service │ ─────────────────────> │ Kafka Topic  │
└─────────────┘                        └──────────────┘
                                              │
                                              │ consumes
                                              ▼
                                       ┌──────────────┐
                                       │User Service  │
                                       │(creates      │
                                       │ profile)     │
                                       └──────────────┘
```

**Benefits:**
- **Eventual Consistency:** User profile eventually created after registration
- **Fault Tolerance:** Event persisted in Kafka if consumer is down
- **Decoupling:** Services communicate asynchronously
- **Auditability:** All user creation events logged in Kafka

## 📚 Related Documentation

- [Main README](../README.md) - Project overview and setup
- [Auth Service](../auth-service/README.md) - User registration and authentication
- [Vault Setup Guide](../vault/certs/README.md) - Certificate and Vault configuration
- [Karga Common Library](../karga-common/README.md) - Shared components and utilities
- [Debezium Configuration](../debezium-connectors/README.md) - CDC configuration for outbox pattern
