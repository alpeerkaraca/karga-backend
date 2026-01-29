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
* **Security:** JWT (RSA256 asymmetric keys) & Spring Security with Docker Secrets.
* **Secrets Management:** HashiCorp Vault with AppRole authentication & Spring Cloud Vault.

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

## 🔐 HashiCorp Vault Integration

All microservices are integrated with **HashiCorp Vault** for centralized secrets management using **Spring Cloud Vault
** with **AppRole authentication**.

### Why Vault?

- **Centralized Secrets Management:** All sensitive data (database passwords, API keys, JWT keys) are stored in Vault.
- **Dynamic Secrets:** Secrets can be rotated without redeploying services.
- **Audit Logging:** Every secret access is logged for compliance.
- **Encryption as a Service:** Vault handles encryption/decryption operations.

### Vault Configuration Overview

Each microservice connects to Vault using the following configuration in `application.properties`:

```properties
spring.cloud.vault.host=${VAULT_HOST:localhost}
spring.cloud.vault.port=${VAULT_PORT:8200}
spring.cloud.vault.scheme=${VAULT_SCHEME:https}
spring.cloud.vault.ssl.trust-store=classpath:vault.jks
spring.cloud.vault.ssl.trust-store-password=${VAULT_TRUSTSTORE_PASSWORD}
spring.cloud.vault.authentication=APPROLE
spring.cloud.vault.app-role.role-id=${VAULT_ROLE_ID}
spring.cloud.vault.app-role.secret-id=${VAULT_SECRET_ID}
spring.cloud.vault.kv.enabled=true
spring.cloud.vault.kv.backend=${VAULT_KV_BACKEND:secret}
spring.config.import=vault://
```

### SSL/TLS Configuration

Vault communication is secured via TLS. Each service requires a **truststore** containing the Vault server certificate.

#### Creating Vault Certificates (Development)

1. **Generate self-signed certificate:**

```bash
openssl req -x509 -newkey rsa:4096 -days 365 -nodes \
  -keyout vault/certs/vault.key \
  -out vault/certs/vault.crt \
  -subj "/CN=vault-server" \
  -addext "subjectAltName = DNS:localhost,DNS:vault-server,IP:127.0.0.1,IP:0.0.0.0"
```

2. **Import certificate into Java Keystore:**

```bash
keytool -import -alias vault -keystore vault.jks -file vault/certs/vault.crt
```

3. **Copy the truststore** to each service's `src/main/resources/` directory.

For detailed certificate instructions, see [vault/certs/README.md](vault/certs/README.md).

### Vault AppRole Setup

Each microservice has its own AppRole for fine-grained access control:

1. **Enable AppRole authentication:**

```bash
vault auth enable approle
```

2. **Create policy for a service:**

```bash
vault policy write auth-service-policy - <<EOF
path "secret/data/auth-service/*" {
  capabilities = ["read"]
}
EOF
```

3. **Create AppRole for the service:**

```bash
vault write auth/approle/role/auth-service \
  token_policies="auth-service-policy" \
  token_ttl=1h \
  token_max_ttl=4h
```

4. **Retrieve Role ID and Secret ID:**

```bash
# Get Role ID (static)
vault read auth/approle/role/auth-service/role-id

# Generate Secret ID (rotatable)
vault write -f auth/approle/role/auth-service/secret-id
```

### Secrets Stored in Vault

The following secrets are managed via Vault's KV v2 secrets engine:

| Secret Path              | Description                           |
|--------------------------|---------------------------------------|
| `secret/auth-service`    | Database credentials, JWT private key |
| `secret/driver-service`  | Database credentials                  |
| `secret/payment-service` | Database credentials, Stripe API keys |
| `secret/trip-service`    | Database credentials                  |
| `secret/user-service`    | Database credentials                  |
| `secret/application`     | JWT public key, Redis and common keys |

---

## 🚀 Getting Started

This project is designed to run with a single command using Docker Compose.

### 1. Prerequisites

* Docker & Docker Compose (or Podman)
* Java 21 (Only if running locally without Docker)

### 2. Security Setup (Crucial Step!) 🔐

This project uses **HashiCorp Vault** for centralized secrets management and **Docker Secrets** for local development.

#### **Step 2.1: Vault Environment Configuration**

Create a `.env` file from the example template:

```bash
cp .env.example .env
```

Edit the `.env` file with your Vault configuration:

```env
# Vault Server Configuration
VAULT_HOST=your-vault-host       # e.g., vault-server or IP address
VAULT_PORT=8200
VAULT_SCHEME=https
VAULT_TRUSTSTORE_PASSWORD=your-truststore-password

# Service-specific AppRole credentials (from Vault)
AUTH_VAULT_ROLE_ID=your_auth_role_id
AUTH_VAULT_SECRET_ID=your_auth_secret_id

DRIVER_VAULT_ROLE_ID=your_driver_role_id
DRIVER_VAULT_SECRET_ID=your_driver_secret_id

PAYMENT_VAULT_ROLE_ID=your_payment_role_id
PAYMENT_VAULT_SECRET_ID=your_payment_secret_id

TRIP_VAULT_ROLE_ID=your_trip_role_id
TRIP_VAULT_SECRET_ID=your_trip_secret_id

USER_VAULT_ROLE_ID=your_user_role_id
USER_VAULT_SECRET_ID=your_user_secret_id
```

#### **Step 2.2: Docker Secrets Setup (Local Development)**

Docker Secrets are still used for local development fallback. Create the required secret files:

**Required Secret Files in ./secrets/ folder:**

* auth_db_password, driver_db_password, payment_db_password, trip_db_password, user_db_password

* redis_password, redis_username

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

### 3. Configure Debezium

In order to communicate through Kafka you need to configure Debezium. All .json files are located
at [/debezium-connectors](/debezium-connectors).

To configure your connectors you can refer to [Debezium Configuration Guide](debezium-connectors/README.md).

### 4. Build & Run

Build the project and start all services. The build uses **multi-stage Dockerfiles** for optimization.

```bash
docker-compose up --build -d # or podman compose up --build -d
```

### 5. Verify Installation

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

---

## 🗂 Project Structure

```
karga-microservices/
├── auth-service/              # Authentication & JWT generation
├── driver-service/            # Driver management & geo-location
├── payment-service/           # Stripe payment processing
├── trip-service/              # Trip lifecycle management
├── user-service/              # User profile management
├── karga-common/              # Shared library (DTOs, Security, Vault config)
├── nginx/                     # Reverse proxy configuration
├── k8s/                       # Kubernetes deployment manifests
│   ├── *-deployment.yaml     # Service deployments
│   ├── *-db.yaml             # Database deployments
│   ├── vault-store.yaml      # External Secrets Operator config
│   └── external-secrets/     # ExternalSecret resources for Vault
│       └── data-prod/        # Database credentials from Vault
├── vault/                     # Vault server configuration
│   ├── certs/                # SSL certificates & README
│   ├── config/               # Vault HCL configuration
│   └── nginx.conf            # Nginx reverse proxy for Vault UI
├── debezium-connectors/       # Debezium CDC connector configurations
├── secrets/                   # Docker secrets (gitignored)
├── secrets-example/           # Secret file templates
├── docker-compose.yml         # Docker Compose orchestration
├── build-all.ps1             # PowerShell build & deploy script
├── jenkinsfile               # Jenkins CI/CD pipeline
├── .env.example              # Environment variable template
└── .tool-versions            # ASDF version manager config
```

---

## 🔄 CI/CD Pipeline

The project includes a **Jenkins pipeline** (`jenkinsfile`) for automated builds and deployments.

### Pipeline Features

- **Selective Service Deployment:** Choose to build/deploy a single service or all services.
- **Multiple Deployment Targets:** Deploy to Kubernetes (Minikube) or build locally with Podman.
- **Parallel Builds:** Services are built in parallel for faster CI/CD.
- **Health Checks:** Validates Podman and Minikube availability before builds.

### Jenkins Pipeline Parameters

| Parameter           | Options                                                        | Description                   |
|---------------------|----------------------------------------------------------------|-------------------------------|
| `SERVICE_TO_DEPLOY` | `all`, `auth-service`, `user-service`, `payment-service`, etc. | Which service to build/deploy |
| `DEPLOY_TARGET`     | `kubernetes`, `podman_local`                                   | Target deployment environment |
| `SKIP_TESTS`        | `true`, `false`                                                | Skip tests during Maven build |

### Manual Build Script (PowerShell)

For local development without Jenkins, use the `build-all.ps1` script:

```powershell
# Build all services and deploy to Minikube
.\build-all.ps1
```

This script:

1. Builds `karga-common` shared library
2. Creates Podman images for each service
3. Loads images into Minikube using TAR export

---

## ☸️ Kubernetes Deployment

Kubernetes manifests are available in the [k8s/](k8s/) directory with Vault integration.

### Prerequisites

1. **Create namespaces:**

```bash
kubectl create namespace app-prod
kubectl create namespace data-prod
```

2. **Create Vault CA certificate secret:**

```bash
kubectl create secret generic vault-ca \
  --from-file=vault.crt=vault/certs/vault.crt \
  -n data-prod
```

3. **Create Vault truststore secret (for services):**

```bash
kubectl create secret generic vault-truststore \
  --from-file=vault.jks=path/to/vault.jks \
  -n app-prod
```

4. **Create Vault AppRole credentials secret:**

```bash
kubectl create secret generic vault-approle-creds \
  --from-literal=AUTH_VAULT_ROLE_ID=your_auth_role_id \
  --from-literal=AUTH_VAULT_SECRET_ID=your_auth_secret_id \
  --from-literal=DRIVER_VAULT_ROLE_ID=your_driver_role_id \
  --from-literal=DRIVER_VAULT_SECRET_ID=your_driver_secret_id \
  --from-literal=PAYMENT_VAULT_ROLE_ID=your_payment_role_id \
  --from-literal=PAYMENT_VAULT_SECRET_ID=your_payment_secret_id \
  --from-literal=TRIP_VAULT_ROLE_ID=your_trip_role_id \
  --from-literal=TRIP_VAULT_SECRET_ID=your_trip_secret_id \
  --from-literal=USER_VAULT_ROLE_ID=your_user_role_id \
  --from-literal=USER_VAULT_SECRET_ID=your_user_secret_id \
  --from-literal=VAULT_TRUSTSTORE_PASSWORD=your_truststore_password \
  -n app-prod
```

5. **Create Vault token secret (for External Secrets Operator):**

```bash
kubectl create secret generic vault-token \
  --from-literal=token=your_vault_token \
  -n data-prod
```

### External Secrets Operator Integration

The project uses [External Secrets Operator](https://external-secrets.io/) to sync secrets from Vault to Kubernetes.

1. **Install External Secrets Operator:**

```bash
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets external-secrets/external-secrets -n external-secrets --create-namespace
```

2. **Deploy SecretStore (connects to Vault):**

```bash
kubectl apply -f k8s/vault-store.yaml
```

3. **Deploy ExternalSecrets (syncs database credentials):**

```bash
kubectl apply -f k8s/external-secrets/data-prod/
```

This creates Kubernetes secrets automatically from Vault paths like `secret/auth-service/k8s`.

### Deploy Services

```bash
kubectl apply -f k8s/auth-deployment.yaml
kubectl apply -f k8s/driver-deployment.yaml
kubectl apply -f k8s/payment-deployment.yaml
kubectl apply -f k8s/trip-deployment.yaml
kubectl apply -f k8s/user-deployment.yaml
```

---

## 🔒 Security Best Practices

- **Vault TLS:** Always use HTTPS (`spring.cloud.vault.scheme=https`) for Vault communication in production.
- **Secret Rotation:** Regularly rotate Vault AppRole Secret IDs using
  `vault write -f auth/approle/role/<service>/secret-id`.
- **Least Privilege:** Each service has its own AppRole with access only to its specific secret path.
- **Audit Logging:** Enable Vault audit logging to track all secret access.
- **RSA Keys:** JWT signing uses RSA256 asymmetric keys stored in Vault, not symmetric secrets.
- **Network Segmentation:** Use `extra_hosts` in Docker Compose to allow containers to reach external Vault server.

---

## 📚 Additional Documentation

- [Vault Certificate Setup Guide](vault/certs/README.md) - How to create and configure SSL certificates for Vault
- [Vault Server Configuration](vault/config/config.hcl) - HCL configuration for Vault server
- [Debezium Configuration Guide](debezium-connectors/README.md) - How to configure Debezium CDC connectors
- [External Secrets Examples](k8s/external-secrets/) - ExternalSecret resources for Vault integration

---

## 🛠 Troubleshooting

### Vault Connection Issues

If services fail to connect to Vault:

1. **Verify Vault is accessible:**
   ```bash
   curl -k https://<VAULT_HOST>:8200/v1/sys/health
   ```

2. **Check truststore configuration:**
   - Ensure `vault.jks` contains the Vault server certificate
   - Verify `VAULT_TRUSTSTORE_PASSWORD` is correct

3. **For Kubernetes deployments:**
   - Ensure `vault-truststore` secret exists in `app-prod` namespace
   - Verify the service can reach Vault from within the cluster

### Nginx Reverse Proxy for Vault UI

For development, you can use the Nginx configuration in `vault/nginx.conf` to proxy HTTP requests to Vault's HTTPS
endpoint:

```bash
docker run -d -p 9000:80 -v $(pwd)/vault/nginx.conf:/etc/nginx/nginx.conf:ro nginx:alpine
```

Access Vault UI at `http://localhost:9000` (proxied to HTTPS Vault server).

