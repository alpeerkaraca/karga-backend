# 🐦‍⬛ Karga Microservices - Ride Hailing Backend

**Karga** is a robust, event-driven microservices architecture designed for a scalable ride-hailing application (similar
to Martı Tag/Uber/Lyft). It is built with **Spring Boot 3**, **Apache Kafka (KRaft mode)**, **Redis (Geo-spatial)**, and
**PostgreSQL**, fully containerized with **Docker**.

## 🏗 Architecture & Tech Stack

The project follows a **Database-per-Service** pattern and uses **Async Communication** via Kafka for high scalability.

* **Language:** Java 21
* **Framework:** Spring Boot 3.5.x
* **Message Broker:** Apache Kafka 4.1.1 (KRaft Mode - ZooKeeper-less)
* **Caching & Geo:** Redis 7 (Alpine) - Used for caching and real-time driver location tracking.
* **Database:** PostgreSQL 18 (Alpine)
* **Containerization:** Docker & Docker Compose
* **Security:** JWT (JSON Web Tokens) & Spring Security with Docker Secrets.

---

## 📦 Services Overview

| Service             | Port   | Description                                                                                    | DB Name      |
|:--------------------|:-------|:-----------------------------------------------------------------------------------------------|:-------------|
| **Auth Service**    | `8084` | Handles Registration, Login, JWT generation, and publishes `user_created` events.              | `auth_db`    |
| **Driver Service**  | `8085` | Manages driver profiles, statuses (Online/Busy), and real-time location updates via Redis Geo. | `driver_db`  |
| **Payment Service** | `8086` | Handles payments (Stripe integration) and listens to trip completion events.                   | `payment_db` |
| **Trip Service**    | `8087` | Core logic for trip matching, pricing, and ride lifecycle management.                          | `trip_db`    |
| **User Service**    | `8088` | Manages passenger profiles and consumes `user_created` events to sync data.                    | `user_db`    |
| **Karga Common**    | -      | Shared library containing DTOs, Security Filters, Exceptions, and Utilities.                   | -            |

---

## 🚀 Getting Started

This project is designed to run with a single command using Docker Compose.

### 1. Prerequisites

* Docker & Docker Compose (or Podman)
* Java 21 (Only if running locally without Docker)

### 2. Security Setup (Crucial Step!) 🔐

This project uses **Docker Secrets** to manage sensitive data. You **must** create the secret files before starting the
containers.

#### **Required Secret Files in ./secrets/ folder:**

* auth_db_password, driver_db_password, payment_db_password, trip_db_password, user_db_password

* redis_password, redis_username

* jwt_secret (Must be 256-bit+ for HS256)

* stripe_api_key, stripe_webhook_secret

Run the following commands in the project root to set up the secrets from the examples:

#### 1. Create the secrets directory

```bash
mkdir -p secrets
```

#### 2. Copy the example secrets to the secrets directory

#### (On Windows PowerShell)

```bash
copy secrets-example\* secrets\
```

#### (On Linux/Mac)

```bash
cp secrets-example/* secrets/
```

### 3. Build & Run

Build the project and start all services. The build uses **multi-stage Dockerfiles** for optimization.

```bash
docker-compose up --build -d # or podman compose up --build -d
```

### 4. Verify Installation

Check if all containers are up and running:

```bash
docker-compose ps
```

## 📡 Event-Driven Flows (Kafka)

The system relies heavily on Kafka for decoupling services.

1. **User Registration:**
    * **Auth Service** registers a user -> Publishes to `user_created` topic.
    * **User Service** consumes `user_created` -> Creates a user profile.

2. **Driver Location:**
    * **Driver App** sends location -> **Driver Service** publishes to `driver_location_updates`.
    * **Driver Service (Consumer)** updates Redis Geo Index for "Find Nearby Drivers" feature.

3. **Trip Lifecycle:**
    * **Trip Service** publishes `trip_events` (Created, Started, Completed).
    * **Driver Service** listens to update driver status (Online -> Busy).
    * **Payment Service** listens to `TRIP_COMPLETED` to charge the user.

## 🛠 Development & Debugging

### Logs

To watch logs for a specific service (e.g., Auth Service):

```bash
docker-compose logs -f auth-service
```

### Hot Reload (Optional)

If you want to rebuild only one service after code changes (e.g., User Service) without restarting the whole cluster:

```bash
docker-compose up -d --build user-service
```

### API Endpoints

You can test the endpoints via Postman or cURL.

#### Register a User (Example)

```http
POST http://localhost:8084/api/v1/auth/register
Content-Type: application/json

{
  "email": "test@karga.com",
  "password": "SecurePassword123!",
  "firstName": "Alper",
  "lastName": "Karaca",
  "phoneNumber": "+905551234567"
}
```

#### Get User Profile

```http
GET http://localhost:8088/api/v1/users/me
Authorization: Bearer <YOUR_ACCESS_TOKEN>
```

## 🧪 Testing

The project uses Testcontainers for integration testing. This ensures tests run against real instances of PostgreSQL,
Redis, and Kafka inside Docker containers.

```bash
./mvnw clean verify
```





