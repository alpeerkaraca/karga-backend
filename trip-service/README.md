# 🚕 Trip Service

Core trip management service for the Karga ride-hailing platform. Handles trip requests, driver matching, pricing calculations, and trip lifecycle management (request, accept, start, complete, cancel).

## 📋 Overview

The Trip Service is responsible for:
- Finding nearby available drivers using Redis Geo-spatial queries
- Creating trip requests from passengers
- Managing trip lifecycle (Requested → Accepted → Started → Completed/Cancelled)
- Calculating trip pricing based on distance and duration
- Publishing trip events to Kafka for other services
- Driver-trip matching and assignment

**Port:** `8087`  
**Database:** `trip_db` (PostgreSQL)  
**Cache:** Redis (Geo-spatial queries for driver matching)

## 🏗️ Architecture

- **Framework:** Spring Boot 3.5.x
- **Security:** Spring Security with JWT (RSA256 public key from Vault)
- **Database:** PostgreSQL 18
- **Cache & Geo:** Redis 7 (GEORADIUS queries for nearby drivers)
- **Message Broker:** Apache Kafka (publishes `trip_events`)
- **Secrets Management:** HashiCorp Vault (AppRole authentication)

## 🚀 API Endpoints

### Base URL
```
http://localhost:8087/api/v1/trips
```

Most endpoints require JWT authentication via `Authorization: Bearer <token>` header.

### 1. Find Nearby Drivers
**GET** `/nearby-drivers`

Finds available drivers within a 5km radius of the given location using Redis Geo-spatial queries.

**Query Parameters:**
- `latitude` (required) - Pickup latitude
- `longitude` (required) - Pickup longitude

**Example:**
```bash
GET /api/v1/trips/nearby-drivers?latitude=41.0082&longitude=28.9784
```

**Response:**
```json
{
  "success": true,
  "message": "Nearby drivers listed.",
  "data": [
    {
      "driverId": "uuid",
      "distance": 1.24,
      "latitude": 41.0095,
      "longitude": 28.9801
    },
    {
      "driverId": "uuid",
      "distance": 3.47,
      "latitude": 41.0213,
      "longitude": 28.9652
    }
  ]
}
```

**Status Codes:**
- `200 OK` - Drivers found (may return empty array)
- `400 Bad Request` - Invalid latitude/longitude

### 2. Request Trip (Passenger)
**POST** `/request`

Creates a new trip request from a passenger.

**Headers:**
```
Authorization: Bearer <PASSENGER_JWT_TOKEN>
Content-Type: application/json
```

**Request Body:**
```json
{
  "pickupLatitude": 41.0082,
  "pickupLongitude": 28.9784,
  "dropoffLatitude": 41.0251,
  "dropoffLongitude": 28.9741,
  "pickupAddress": "Taksim Square, Istanbul",
  "dropoffAddress": "Galata Tower, Istanbul"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Trip request received.",
  "data": {
    "tripId": "uuid",
    "passengerId": "uuid",
    "pickupLatitude": 41.0082,
    "pickupLongitude": 28.9784,
    "dropoffLatitude": 41.0251,
    "dropoffLongitude": 28.9741,
    "status": "REQUESTED",
    "estimatedPrice": 45.50,
    "createdAt": "2026-02-04T12:00:00Z"
  }
}
```

**Status Codes:**
- `200 OK` - Trip created successfully
- `401 Unauthorized` - Invalid or missing JWT token

### 3. Get Available Trips (Driver)
**GET** `/available`

Lists all trips with status `REQUESTED` that drivers can accept.

**Headers:**
```
Authorization: Bearer <DRIVER_JWT_TOKEN>
```

**Response:**
```json
{
  "success": true,
  "message": "Available trips listed.",
  "data": [
    {
      "tripId": "uuid",
      "passengerId": "uuid",
      "pickupAddress": "Taksim Square, Istanbul",
      "dropoffAddress": "Galata Tower, Istanbul",
      "estimatedPrice": 45.50,
      "createdAt": "2026-02-04T12:00:00Z"
    }
  ]
}
```

### 4. Accept Trip (Driver)
**POST** `/{tripId}/accept`

Driver accepts a trip request.

**Headers:**
```
Authorization: Bearer <DRIVER_JWT_TOKEN>
```

**Response:**
```json
{
  "success": true,
  "message": "Trip accepted.",
  "data": {
    "tripId": "uuid",
    "driverId": "uuid",
    "status": "ACCEPTED"
  }
}
```

**Status Codes:**
- `200 OK` - Trip accepted
- `400 Bad Request` - Trip already accepted by another driver
- `401 Unauthorized` - Invalid JWT token
- `404 Not Found` - Trip not found

### 5. Start Trip (Driver)
**POST** `/{tripId}/start`

Driver starts the trip (picks up passenger).

**Headers:**
```
Authorization: Bearer <DRIVER_JWT_TOKEN>
```

**Response:**
```json
{
  "success": true,
  "message": "Trip started.",
  "data": null
}
```

**Events Published:**
- `TRIP_STARTED` event to Kafka (driver status → BUSY)

### 6. Complete Trip (Driver)
**POST** `/{tripId}/complete`

Driver completes the trip (drops off passenger).

**Headers:**
```
Authorization: Bearer <DRIVER_JWT_TOKEN>
```

**Response:**
```json
{
  "success": true,
  "message": "Trip completed.",
  "data": null
}
```

**Events Published:**
- `TRIP_COMPLETED` event to Kafka (triggers payment processing, driver status → ONLINE)

### 7. Cancel Trip
**POST** `/{tripId}/cancel`

Cancels a trip (can be initiated by passenger or driver).

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "success": true,
  "message": "Trip cancelled.",
  "data": null
}
```

**Events Published:**
- `TRIP_CANCELLED` event to Kafka (driver status → ONLINE if assigned)

## 🔧 Configuration

### Environment Variables

#### Vault Configuration
```properties
VAULT_HOST=vault-server
VAULT_PORT=8200
VAULT_SCHEME=https
VAULT_TRUSTSTORE_PASSWORD=your-truststore-password
VAULT_ROLE_ID=your-trip-service-role-id
VAULT_SECRET_ID=your-trip-service-secret-id
```

#### Database Configuration (from Vault)
Vault path: `secret/trip-service`
- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

#### JWT Configuration (from Vault)
Vault path: `secret/application`
- `jwt.public.key` - RSA public key for token verification

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
spring.application.name=trip-service
server.port=8087

# Kafka Producer (publishes trip events)
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
```

## 📤 Kafka Events

### Published Topics

#### `trip_events`
Published when trip status changes.

**Event Types:**
- `TRIP_CREATED` - New trip requested
- `TRIP_ACCEPTED` - Driver accepted trip
- `TRIP_STARTED` - Driver picked up passenger
- `TRIP_COMPLETED` - Trip finished successfully
- `TRIP_CANCELLED` - Trip was cancelled

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

**Consumers:**
- **Driver Service:** Updates driver status based on trip events
- **Payment Service:** Processes payment when `TRIP_COMPLETED`

## 🗺️ Trip Lifecycle

```
REQUESTED → ACCEPTED → STARTED → COMPLETED
    ↓           ↓          ↓
 CANCELLED   CANCELLED  CANCELLED
```

**Status Descriptions:**
- `REQUESTED` - Trip created by passenger, waiting for driver
- `ACCEPTED` - Driver assigned, heading to pickup location
- `STARTED` - Passenger picked up, trip in progress
- `COMPLETED` - Trip finished, payment processed
- `CANCELLED` - Trip cancelled by passenger or driver

## 🐳 Running with Docker

### Using Docker Compose (Recommended)

```bash
# From project root
docker-compose up --build -d trip-service
```

### Standalone Docker

```bash
# Build the image
cd trip-service
docker build -t karga/trip-service:latest .

# Run the container
docker run -d \
  --name trip-service \
  -p 8087:8087 \
  -e VAULT_HOST=vault-server \
  -e VAULT_ROLE_ID=your-role-id \
  -e VAULT_SECRET_ID=your-secret-id \
  karga/trip-service:latest
```

## 🛠️ Local Development

### Prerequisites
- Java 21
- Maven 3.9+
- PostgreSQL 18
- Redis 7 (with geo-spatial support)
- Kafka
- HashiCorp Vault (configured with trip-service AppRole)

### Build the Service

```bash
# Install karga-common dependency first
cd ../karga-common
mvn clean install

# Build trip-service
cd ../trip-service
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

### Manual Testing with cURL

**Get passenger JWT token:**
```bash
PASSENGER_TOKEN=$(curl -s -X POST http://localhost:8084/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"passenger@karga.com","password":"SecurePass123!"}' \
  | jq -r '.data.accessToken')
```

**Find nearby drivers:**
```bash
curl -X GET "http://localhost:8087/api/v1/trips/nearby-drivers?latitude=41.0082&longitude=28.9784"
```

**Request a trip:**
```bash
curl -X POST http://localhost:8087/api/v1/trips/request \
  -H "Authorization: Bearer $PASSENGER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "pickupLatitude": 41.0082,
    "pickupLongitude": 28.9784,
    "dropoffLatitude": 41.0251,
    "dropoffLongitude": 28.9741,
    "pickupAddress": "Taksim Square, Istanbul",
    "dropoffAddress": "Galata Tower, Istanbul"
  }'
```

**Get driver JWT token:**
```bash
DRIVER_TOKEN=$(curl -s -X POST http://localhost:8084/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"driver@karga.com","password":"SecurePass123!"}' \
  | jq -r '.data.accessToken')
```

**Get available trips:**
```bash
curl -X GET http://localhost:8087/api/v1/trips/available \
  -H "Authorization: Bearer $DRIVER_TOKEN"
```

**Accept a trip:**
```bash
curl -X POST http://localhost:8087/api/v1/trips/{tripId}/accept \
  -H "Authorization: Bearer $DRIVER_TOKEN"
```

## 📊 Monitoring

### Health Check
```bash
curl http://localhost:8087/actuator/health
```

### View Logs
```bash
# Docker
docker-compose logs -f trip-service

# Kubernetes
kubectl logs -f deployment/trip-service -n app-prod
```

## 🔒 Security Features

- **JWT Authentication:** All authenticated endpoints require valid JWT token
- **Role-based Access:** Passengers can request trips, drivers can accept trips
- **User ID Extraction:** User ID extracted from JWT claims
- **Vault Integration:** All secrets retrieved from HashiCorp Vault
- **Input Validation:** Jakarta Validation for all request bodies

## 🚨 Troubleshooting

### Common Issues

**1. No Nearby Drivers Found**
```
Issue: Empty array returned from /nearby-drivers
```
**Solution:**
- Ensure drivers are online and have updated their location
- Check Redis Geo index: `ZRANGE drivers:locations 0 -1`
- Verify driver-service is publishing location updates to Kafka

**2. Trip Already Accepted**
```
Error: Trip has already been accepted by another driver
```
**Solution:** This is expected behavior. Trip can only be accepted by one driver (first-come, first-served).

**3. Payment Not Triggered**
```
Issue: Trip completed but payment not processed
```
**Solution:**
- Verify payment-service is running and consuming `trip_events` topic
- Check Kafka consumer logs in payment-service
- Ensure Debezium connector is configured for outbox pattern

## 📚 Related Documentation

- [Main README](../README.md) - Project overview and setup
- [Driver Service](../driver-service/README.md) - Driver location and status management
- [Payment Service](../payment-service/README.md) - Payment processing
- [Auth Service](../auth-service/README.md) - JWT token generation
- [Vault Setup Guide](../vault/certs/README.md) - Certificate and Vault configuration
