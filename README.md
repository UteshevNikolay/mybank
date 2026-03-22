# MyBank

A microservice-based banking application with a UI that allows users to:

- Edit their account data (last name, first name, date of birth)
- Deposit virtual money into their account and withdraw it
- Transfer virtual money to another account

## Microservices

| Service | Description |
|---------|-------------|
| **Frontend** | Front UI |
| **Accounts** | User account data management (with Transactional Outbox for notifications) |
| **Cash** | Deposit and withdrawal operations |
| **Transfer** | Money transfers between accounts |
| **Notifications** | User notifications |

## Technology Stack

- **Language**: Java 21
- **Build**: Gradle
- **Framework**: Spring Web MVC
- **Data Access**: Spring Data JPA and JDBC (both dependencies included; choice per service TBD)
- **Database**: PostgreSQL (each microservice has its own DB)
- **Auth Server**: Keycloak (OAuth 2.0, Authorization Code Flow, JWT access tokens)
- **Gateway API**: Custom service with Spring Cloud Gateway
- **Service Discovery**: Consul (integration via Spring Cloud Consul)
- **Externalized/Distributed Config**: Consul
- **Packaging**: Executable JAR, running on embedded Tomcat
- **Containers**: Docker (Single Service per Host), orchestrated via Docker Compose

## Architecture & Patterns

- **Gateway API** — Frontend routes all requests through the gateway
- **Service Discovery** — Microservices register in Consul; direct service-to-service communication
- **Circuit Breaker** — Resilient inter-service calls
- **RPI (Remote Procedure Invocation)** — Synchronous inter-service communication
- **Transactional Outbox** — Reliable event publishing; used by Cash/Transfer services to notify the Notifications service
- **Access Token** — OAuth 2.0 Authorization Code Flow via Keycloak for user authorization
- **UI Composition** — Frontend composes UI from multiple microservice responses
- **Externalized/Distributed Config** — Common configs served centrally
- **Contract Testing** — Verified inter-service contracts
- **Single Service per Host** — Each service runs in its own Docker container

## Testing

- **Unit tests** — JUnit 5
- **Integration tests** — Spring Boot Test, TestContext Framework (with context caching)
- **Contract tests** — Spring Cloud Contract

## Project Structure

```
mybank/
├── gateway/          — Spring Cloud Gateway (port 8090)
├── accounts/         — Accounts service (port 8081)
├── cash/             — Cash deposit/withdrawal service (port 8083)
├── transfer/         — Money transfer service (port 8084)
├── notifications/    — Notifications service (port 8085)
├── frontend/         — Frontend UI with Thymeleaf (port 8082)
├── docker-compose.yml
├── build.gradle      — root build with shared config
├── settings.gradle
└── gradle/           — Gradle wrapper
```

## Infrastructure (Docker Compose)

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| Consul | hashicorp/consul:1.21 | 8500 | Service Discovery + Distributed Config |
| Keycloak | keycloak/keycloak:26.2 | 8080 | OAuth 2.0 Authorization Server |
| Gateway | custom (Spring Cloud Gateway) | 8090 | API Gateway |
| Accounts | custom (Spring Boot) | 8081 | Account data management |
| Cash | custom (Spring Boot) | 8083 | Deposit and withdrawal operations |
| Transfer | custom (Spring Boot) | 8084 | Money transfers between accounts |
| Notifications | custom (Spring Boot) | 8085 | Event notifications (Transactional Outbox consumer) |
| Frontend | custom (Spring Boot + Thymeleaf) | 8082 | Web UI |
| accounts-db | postgres:17 | 5433 | Accounts service database |
| cash-db | postgres:17 | 5434 | Cash service database |
| transfer-db | postgres:17 | 5435 | Transfer service database |
| notifications-db | postgres:17 | 5436 | Notifications service database |

All services run on a shared `mybank-network` bridge network.

## Resilience (Circuit Breaker)

Inter-service HTTP calls are protected with Resilience4j Circuit Breaker. When a downstream service is unavailable, the circuit opens after 50% failure rate (sliding window of 10 calls) and returns a fallback error for 10 seconds before retrying.

Protected calls: Frontend → Accounts/Cash/Transfer, Cash → Accounts, Transfer → Accounts.

## Security (OAuth 2.0 / Keycloak)

Authentication and authorization is handled by Keycloak with the `mybank` realm (auto-imported on startup).

| Flow | Used by | Description |
|------|---------|-------------|
| Authorization Code | Frontend | Users log in via Keycloak UI, frontend holds session with JWT |
| Client Credentials | Cash, Transfer | Service-to-service calls to Accounts and Notifications |

**Keycloak clients:**
- `mybank-frontend` — Authorization Code Flow (secret: `frontend-secret`)
- `cash-service` — Client Credentials (secret: `cash-secret`, role: `SERVICE_ACCESS`)
- `transfer-service` — Client Credentials (secret: `transfer-secret`, role: `SERVICE_ACCESS`)

**Test user:** `user1` / `password`

**Keycloak admin:** http://localhost:8080 (admin / admin)

## Testing

Tests are written using JUnit 5, Spring Boot Test, and Testcontainers (requires Docker running).

| Module | Unit Tests | Integration Tests | Total |
|--------|-----------|-------------------|-------|
| Accounts | AccountServiceTest (8) | AccountControllerIntegrationTest (8) | 16 |
| Cash | CashServiceTest (3) | CashControllerIntegrationTest (2) | 5 |
| Transfer | TransferServiceTest (2) | TransferControllerIntegrationTest (2) | 4 |
| Notifications | NotificationServiceTest (1) | NotificationControllerIntegrationTest (1) | 2 |
| Frontend | MainControllerTest (3) | — | 3 |

Run all tests:
```bash
./gradlew clean test
```

## How to Run

```bash
# Start infrastructure
docker compose up -d consul keycloak accounts-db cash-db transfer-db notifications-db

# Build services
./gradlew :gateway:build :accounts:build :cash:build :transfer:build :notifications:build :frontend:build

# Start everything
docker compose up -d
```

Open http://localhost:8082 — you will be redirected to Keycloak login. Use `user1` / `password`.
