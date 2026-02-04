# 🔐 Auth Service

Authentication and authorization service for the Karga ride-hailing platform. Handles user registration, login, JWT token generation, and publishes user creation events to Kafka.

## 📋 Overview

The Auth Service is responsible for:
- User registration with validation
- User authentication and login
- JWT token generation (Access & Refresh tokens)
- Publishing `user_created` events to Kafka for downstream services
- Rate limiting for security (3 registrations/hour, 5 logins/2 minutes per IP)

**Port:** `8084`  
**Database:** `auth_db` (PostgreSQL)

## 🏗️ Architecture

- **Framework:** Spring Boot 3.5.x
- **Security:** Spring Security with JWT (RSA256 asymmetric keys from Vault)
- **Database:** PostgreSQL 18
- **Message Broker:** Apache Kafka (publishes to `user_created` topic)
- **Secrets Management:** HashiCorp Vault (AppRole authentication)
- **Caching & Rate Limiting:** Redis

## 🚀 API Endpoints

### Base URL
```
http://localhost:8084/api/v1/auth
```

### 1. Register User
**POST** `/register`

Creates a new user account and publishes a user creation event to Kafka.

**Rate Limit:** 3 requests per hour per IP

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+905551234567"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Your account has been created successfully. We'll send you an email after activation.",
  "data": null
}
```

**Status Codes:**
- `201 Created` - User registered successfully
- `400 Bad Request` - Validation error
- `429 Too Many Requests` - Rate limit exceeded

### 2. Login User
**POST** `/login`

Authenticates a user and returns JWT access and refresh tokens.

**Rate Limit:** 5 requests per 2 minutes per IP

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Login successful.",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**Status Codes:**
- `200 OK` - Login successful
- `401 Unauthorized` - Invalid credentials
- `429 Too Many Requests` - Rate limit exceeded

## 🔧 Configuration

### Environment Variables

The service requires the following environment variables (configured via `.env` or Kubernetes secrets):

#### Vault Configuration
```properties
VAULT_HOST=vault-server
VAULT_PORT=8200
VAULT_SCHEME=https
VAULT_TRUSTSTORE_PASSWORD=your-truststore-password
VAULT_ROLE_ID=your-auth-service-role-id
VAULT_SECRET_ID=your-auth-service-secret-id
```

#### Database Configuration (from Vault)
Vault path: `secret/auth-service`
- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

#### JWT Configuration (from Vault)
Vault path: `secret/auth-service`
- `jwt.private.key` - RSA private key for signing tokens
- `jwt.expiration` - Access token expiration time

Vault path: `secret/application`
- `jwt.public.key` - RSA public key for token verification (shared across services)

#### Kafka Configuration (from Vault)
Vault path: `secret/application`
- `spring.kafka.bootstrap-servers`

#### Redis Configuration (from Vault)
Vault path: `secret/application`
- `spring.data.redis.host`
- `spring.data.redis.port`
- `spring.data.redis.password`

### Application Properties

Key configurations in `application.properties`:

```properties
spring.application.name=auth-service
server.port=8084

# Kafka Producer (publishes user_created events)
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

# Rate Limiting
rate-limit.enabled=true
```

## 📤 Kafka Events

### Published Topics

#### `user_created`
Published when a new user successfully registers.

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

**Consumers:** User Service

## 🐳 Running with Docker

### Using Docker Compose (Recommended)

```bash
# From project root
docker-compose up --build -d auth-service
```

### Standalone Docker

```bash
# Build the image
cd auth-service
docker build -t karga/auth-service:latest .

# Run the container
docker run -d \
  --name auth-service \
  -p 8084:8084 \
  -e VAULT_HOST=vault-server \
  -e VAULT_ROLE_ID=your-role-id \
  -e VAULT_SECRET_ID=your-secret-id \
  karga/auth-service:latest
```

## 🛠️ Local Development

### Prerequisites
- Java 21
- Maven 3.9+
- PostgreSQL 18 (running locally or via Docker)
- Kafka (running locally or via Docker)
- Redis (running locally or via Docker)
- HashiCorp Vault (configured with auth-service AppRole)

### Build the Service

```bash
# Install karga-common dependency first
cd ../karga-common
mvn clean install

# Build auth-service
cd ../auth-service
mvn clean package -DskipTests
```

### Run the Service

```bash
mvn spring-boot:run
```

Or run the JAR directly:
```bash
java -jar target/auth-service-0.0.1-SNAPSHOT.jar
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

Integration tests use Testcontainers for PostgreSQL, Kafka, and Redis.

### Manual Testing with cURL

**Register a user:**
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

**Login:**
```bash
curl -X POST http://localhost:8084/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@karga.com",
    "password": "SecurePass123!"
  }'
```

## 📊 Monitoring

### Health Check
```bash
curl http://localhost:8084/actuator/health
```

### View Logs
```bash
# Docker
docker-compose logs -f auth-service

# Kubernetes
kubectl logs -f deployment/auth-service -n app-prod
```

## 🔒 Security Features

- **JWT with RSA256:** Asymmetric cryptography for token signing (private key in Vault)
- **Rate Limiting:** Redis-backed rate limiting to prevent brute-force attacks
- **Password Encryption:** BCrypt hashing for password storage
- **Vault Integration:** All secrets retrieved from HashiCorp Vault
- **Input Validation:** Jakarta Validation (JSR-380) for all request bodies

## 🚨 Troubleshooting

### Common Issues

**1. Vault Connection Failed**
```
Error: Unable to connect to Vault server
```
**Solution:** Verify Vault is running and `VAULT_HOST`, `VAULT_ROLE_ID`, and `VAULT_SECRET_ID` are correct.

**2. Database Connection Error**
```
Error: Unable to acquire JDBC Connection
```
**Solution:** Ensure PostgreSQL is running and Vault contains the correct database credentials at `secret/auth-service`.

**3. Kafka Publishing Failed**
```
Error: Failed to publish user_created event
```
**Solution:** Verify Kafka is running and reachable at the bootstrap servers configured in Vault.

## 📚 Related Documentation

- [Main README](../README.md) - Project overview and setup
- [Vault Setup Guide](../vault/certs/README.md) - Certificate and Vault configuration
- [Karga Common Library](../karga-common/README.md) - Shared components and utilities
- [Docker Compose Setup](../docker-compose.yml) - Full environment setup
