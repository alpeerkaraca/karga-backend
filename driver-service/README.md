# 🚗 Driver Service

Driver management service for the Karga ride-hailing platform. Handles driver profiles, real-time location tracking using Redis Geo-spatial features, and driver status management.

## 📋 Overview

The Driver Service is responsible for:
- Managing driver profiles and information
- Real-time driver location updates and tracking
- Driver status management (Online, Busy, Offline)
- Geo-spatial queries to find nearby available drivers
- Publishing driver location updates to Kafka
- Consuming trip events to update driver status

**Port:** `8085`  
**Database:** `driver_db` (PostgreSQL)  
**Cache:** Redis (Geo-spatial indexing for location data)

## 🏗️ Architecture

- **Framework:** Spring Boot 3.5.x
- **Security:** Spring Security with JWT (RSA256 public key from Vault)
- **Database:** PostgreSQL 18
- **Cache & Geo:** Redis 7 (Geo-spatial commands: GEOADD, GEORADIUS)
- **Message Broker:** Apache Kafka (publishes to `driver_location_updates`, consumes `trip_events`)
- **Secrets Management:** HashiCorp Vault (AppRole authentication)

## 🚀 API Endpoints

### Base URL
```
http://localhost:8085/api/v1/drivers
```

All endpoints require JWT authentication via `Authorization: Bearer <token>` header.

### 1. Update Driver Location
**POST** `/location`

Updates the driver's current GPS location and publishes to Kafka for geo-indexing.

**Headers:**
```
Authorization: Bearer <JWT_ACCESS_TOKEN>
Content-Type: application/json
```

**Request Body:**
```json
{
  "latitude": 41.0082,
  "longitude": 28.9784
}
```

**Response:**
```
HTTP 202 Accepted
```

**Description:**
- Extracts driver ID from JWT token
- Publishes location update to `driver_location_updates` Kafka topic
- Redis Geo index is updated by a Kafka consumer

### 2. Update Driver Status
**POST** `/status`

Updates the driver's availability status (Online, Busy, Offline) and current location.

**Headers:**
```
Authorization: Bearer <JWT_ACCESS_TOKEN>
Content-Type: application/json
```

**Request Body:**
```json
{
  "status": "ONLINE",
  "latitude": 41.0082,
  "longitude": 28.9784
}
```

**Valid Status Values:**
- `ONLINE` - Driver is available for trips
- `BUSY` - Driver is currently on a trip
- `OFFLINE` - Driver is not accepting trips

**Response:**
```json
{
  "success": true,
  "message": "Driver status updated successfully.",
  "data": null
}
```

**Status Codes:**
- `200 OK` - Status updated successfully
- `400 Bad Request` - Invalid status value
- `401 Unauthorized` - Missing or invalid JWT token

## 🗺️ Redis Geo-spatial Features

The service uses Redis Geo-spatial indexing for efficient nearby driver queries.

### Geo Index Structure

**Redis Key:** `drivers:locations`

**Stored Data:**
- Driver ID (member name)
- Longitude & Latitude (coordinates)

### Example Redis Commands

**Add driver location:**
```bash
GEOADD drivers:locations <longitude> <latitude> <driverId>
```

**Find drivers within 5km radius:**
```bash
GEORADIUS drivers:locations <longitude> <latitude> 5 km WITHDIST WITHCOORD ASC
```

**Remove offline driver:**
```bash
ZREM drivers:locations <driverId>
```

## 🔧 Configuration

### Environment Variables

#### Vault Configuration
```properties
VAULT_HOST=vault-server
VAULT_PORT=8200
VAULT_SCHEME=https
VAULT_TRUSTSTORE_PASSWORD=your-truststore-password
VAULT_ROLE_ID=your-driver-service-role-id
VAULT_SECRET_ID=your-driver-service-secret-id
```

#### Database Configuration (from Vault)
Vault path: `secret/driver-service`
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
spring.application.name=driver-service
server.port=8085

# Kafka Producer (publishes location updates)
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

# Kafka Consumer (consumes trip events)
spring.kafka.consumer.group-id=driver-service-group
spring.kafka.consumer.auto-offset-reset=earliest
```

## 📤 Kafka Events

### Published Topics

#### `driver_location_updates`
Published when a driver updates their location.

**Event Schema:**
```json
{
  "driverId": "uuid",
  "latitude": 41.0082,
  "longitude": 28.9784,
  "timestamp": "2026-02-04T12:00:00Z"
}
```

**Consumers:** Driver Service (Geo-indexing consumer)

### Consumed Topics

#### `trip_events`
Consumed to update driver status based on trip lifecycle.

**Relevant Events:**
- `TRIP_STARTED` → Set driver status to `BUSY`
- `TRIP_COMPLETED` → Set driver status to `ONLINE`
- `TRIP_CANCELLED` → Set driver status to `ONLINE`

## 🐳 Running with Docker

### Using Docker Compose (Recommended)

```bash
# From project root
docker-compose up --build -d driver-service
```

### Standalone Docker

```bash
# Build the image
cd driver-service
docker build -t karga/driver-service:latest .

# Run the container
docker run -d \
  --name driver-service \
  -p 8085:8085 \
  -e VAULT_HOST=vault-server \
  -e VAULT_ROLE_ID=your-role-id \
  -e VAULT_SECRET_ID=your-secret-id \
  karga/driver-service:latest
```

## 🛠️ Local Development

### Prerequisites
- Java 21
- Maven 3.9+
- PostgreSQL 18
- Redis 7
- Kafka
- HashiCorp Vault (configured with driver-service AppRole)

### Build the Service

```bash
# Install karga-common dependency first
cd ../karga-common
mvn clean install

# Build driver-service
cd ../driver-service
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

**Get JWT token first** (from auth-service):
```bash
TOKEN=$(curl -s -X POST http://localhost:8084/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"driver@karga.com","password":"SecurePass123!"}' \
  | jq -r '.data.accessToken')
```

**Update location:**
```bash
curl -X POST http://localhost:8085/api/v1/drivers/location \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "latitude": 41.0082,
    "longitude": 28.9784
  }'
```

**Update status:**
```bash
curl -X POST http://localhost:8085/api/v1/drivers/status \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "ONLINE",
    "latitude": 41.0082,
    "longitude": 28.9784
  }'
```

## 📊 Monitoring

### Health Check
```bash
curl http://localhost:8085/actuator/health
```

### View Logs
```bash
# Docker
docker-compose logs -f driver-service

# Kubernetes
kubectl logs -f deployment/driver-service -n app-prod
```

### Monitor Redis Geo Index
```bash
# Connect to Redis
redis-cli

# Count drivers in index
ZCARD drivers:locations

# Get all drivers (development only)
ZRANGE drivers:locations 0 -1 WITHSCORES
```

## 🔒 Security Features

- **JWT Authentication:** All endpoints require valid JWT token
- **Driver ID Extraction:** Driver ID extracted from JWT claims (prevents spoofing)
- **Vault Integration:** All secrets retrieved from HashiCorp Vault
- **Input Validation:** Jakarta Validation for all request bodies

## 🚨 Troubleshooting

### Common Issues

**1. Redis Connection Failed**
```
Error: Unable to connect to Redis server
```
**Solution:** Verify Redis is running and credentials in Vault are correct at `secret/application`.

**2. Geo Index Not Updated**
```
Issue: Location updates don't appear in Redis
```
**Solution:** 
- Check if Kafka consumer is running and consuming from `driver_location_updates`
- Verify Debezium connector is configured (if using outbox pattern)
- Check consumer logs: `docker-compose logs -f driver-service`

**3. JWT Verification Failed**
```
Error: Invalid JWT signature
```
**Solution:** Ensure the `jwt.public.key` in Vault matches the private key used by auth-service.

## 📚 Related Documentation

- [Main README](../README.md) - Project overview and setup
- [Auth Service](../auth-service/README.md) - How to obtain JWT tokens
- [Trip Service](../trip-service/README.md) - Trip lifecycle and driver assignment
- [Vault Setup Guide](../vault/certs/README.md) - Certificate and Vault configuration
