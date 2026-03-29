# MyBank

A microservice-based banking application with a UI that allows users to:

- Edit their account data (last name, first name, date of birth)
- Deposit virtual money into their account and withdraw it
- Transfer virtual money to another account

## Microservices

| Service | Description | Port |
|---------|-------------|------|
| **Accounts** | User account data management (publishes events to Kafka) | 8081 |
| **Cash** | Deposit and withdrawal operations | 8083 |
| **Transfer** | Money transfers between accounts | 8084 |
| **Notifications** | User notifications | 8085 |
| **Frontend** | Web UI (Thymeleaf + Bootstrap 5) | 8082 |

## Technology Stack

- **Language**: Java 21
- **Build**: Gradle
- **Framework**: Spring Boot 3.5.12, Spring Web MVC, Spring Cloud 2025.0.0
- **Data Access**: Spring Data JPA
- **Database**: PostgreSQL 17 (each microservice has its own DB)
- **Messaging**: Apache Kafka 3.9 (KRaft mode, notification events via Transactional Outbox)
- **Auth Server**: Keycloak 26.2 (OAuth 2.0, Authorization Code Flow, JWT access tokens)
- **API Gateway**: Traefik Ingress Controller (built into Rancher Desktop k3s)
- **Service Discovery**: Kubernetes DNS (replaces Consul)
- **Externalized Config**: Kubernetes ConfigMaps and Secrets
- **Packaging**: Docker images, deployed via Helm charts
- **Orchestration**: Kubernetes (Rancher Desktop for local development)
- **Distributed Tracing**: Zipkin 3 + Micrometer Tracing (Brave)
- **Metrics**: Prometheus + Grafana (Spring Boot Actuator, Micrometer)
- **Centralized Logging**: ELK Stack (Elasticsearch 8.17, Logstash 8.17, Kibana 8.17) + Logback with LogstashEncoder

## Architecture & Patterns

- **Gateway API** — Traefik Ingress for external access (Frontend, Keycloak) and internal API gateway for inter-service routing
- **Service Discovery** — Kubernetes Services provide DNS-based service resolution
- **Circuit Breaker** — Resilient inter-service calls via Resilience4j
- **RPI (Remote Procedure Invocation)** — Synchronous inter-service communication via RestClient
- **Transactional Outbox + Kafka** — Reliable event publishing; Accounts/Cash/Transfer publish to Kafka topic, Notifications service consumes events
- **Access Token** — OAuth 2.0 Authorization Code Flow via Keycloak for user authorization
- **UI Composition** — Frontend composes UI from multiple microservice responses
- **Externalized Config** — ConfigMaps and Secrets for environment-specific configuration
- **Contract Testing** — Verified inter-service contracts via Spring Cloud Contract
- **Multi-Environment** — Namespace-based environment separation (dev, test, prod)

## Observability

### Distributed Tracing (Zipkin)

All microservices and Frontend report traces to Zipkin via Micrometer Tracing (Brave). Trace context (traceId, spanId) is automatically propagated across HTTP calls (RestClient), Kafka messages, and database queries. TraceId and spanId are included in all log entries for correlation with Kibana.

### Metrics & Alerting (Prometheus + Grafana)

Services expose metrics via Spring Boot Actuator (`/actuator/prometheus`). Prometheus scrapes all 5 services and evaluates alert rules.

**Custom business metrics:**
- `cash.withdrawal.failed` — failed withdrawal attempts (tag: `login`)
- `transfer.failed` — failed transfer attempts (tags: `senderLogin`, `receiverLogin`)
- `notification.delivery.failed` — failed notification delivery (tag: `accountId`)

**Grafana dashboards:**
- HTTP Metrics — RPS, 4xx/5xx error rates, p50/p95/p99 latency
- JVM Metrics — heap memory, GC pauses, thread count, CPU usage
- Business Metrics — failed withdrawals, transfers, notifications

**Prometheus alerts:**
- `HighErrorRate` — 5xx rate > 5% for 2 minutes
- `HighLatency` — p95 > 2 seconds for 5 minutes
- `CashWithdrawalFailures` — high rate of failed withdrawals
- `TransferFailures` — high rate of failed transfers

### Centralized Logging (ELK Stack)

Logs are sent from all services to Logstash via TCP (JSON format using LogstashEncoder), processed and forwarded to Elasticsearch, and visualized in Kibana. Each log entry includes `traceId` and `spanId` from Micrometer Tracing MDC for correlation with Zipkin traces.

**Spring profiles:**
- `default` — plain text console logging (local development)
- `k8s` — JSON console + TCP to Logstash (Kubernetes deployment)

## Project Structure

```
mybank/
├── accounts/         — Accounts service (port 8081)
├── cash/             — Cash deposit/withdrawal service (port 8083)
├── transfer/         — Money transfer service (port 8084)
├── notifications/    — Notifications service (port 8085)
├── frontend/         — Frontend UI with Thymeleaf (port 8082)
├── keycloak/         — Keycloak realm configuration
├── helm/mybank/      — Helm umbrella chart
│   ├── Chart.yaml
│   ├── values.yaml
│   ├── values-dev.yaml
│   ├── values-test.yaml
│   ├── values-prod.yaml
│   └── charts/       — Sub-charts for each component
├── build.gradle      — Root build with shared config
├── settings.gradle
└── gradle/           — Gradle wrapper
```

## Helm Chart Structure

The application is packaged as an umbrella Helm chart with sub-charts for each component:

| Sub-chart | Type | Description |
|-----------|------|-------------|
| accounts-db | StatefulSet | PostgreSQL for Accounts service |
| cash-db | StatefulSet | PostgreSQL for Cash service |
| transfer-db | StatefulSet | PostgreSQL for Transfer service |
| notifications-db | StatefulSet | PostgreSQL for Notifications service |
| keycloak-db | StatefulSet | PostgreSQL for Keycloak |
| kafka | StatefulSet | Apache Kafka message broker (KRaft mode, single-node) |
| keycloak | Deployment | Keycloak OAuth2 Authorization Server |
| gateway | Ingress + Service | API Gateway — Traefik Ingress routing for inter-service communication |
| accounts | Deployment | Accounts microservice |
| cash | Deployment | Cash microservice |
| transfer | Deployment | Transfer microservice |
| notifications | Deployment | Notifications microservice |
| frontend | Deployment | Frontend Web UI |
| zipkin | Deployment | Zipkin distributed tracing server |
| elasticsearch | StatefulSet | Elasticsearch for log storage |
| logstash | Deployment | Logstash for log processing and forwarding |
| kibana | Deployment | Kibana for log visualization |
| prometheus | StatefulSet | Prometheus metrics collection and alerting |
| grafana | Deployment | Grafana for metrics visualization |

Each sub-chart can be deployed independently or together via the umbrella chart.

## Resilience (Circuit Breaker)

Inter-service HTTP calls are protected with Resilience4j Circuit Breaker. When a downstream service is unavailable, the circuit opens after 50% failure rate (sliding window of 10 calls) and returns a fallback error for 10 seconds before retrying.

Protected calls: Frontend → Gateway → Accounts/Cash/Transfer, Cash → Gateway → Accounts, Transfer → Gateway → Accounts.

## Security (OAuth 2.0 / Keycloak)

Authentication and authorization is handled by Keycloak with the `mybank` realm (auto-imported on startup via ConfigMap).

| Flow | Used by | Description |
|------|---------|-------------|
| Authorization Code | Frontend | Users log in via Keycloak UI, frontend holds session with JWT |
| Client Credentials | Accounts, Cash, Transfer | Service-to-service calls |

**Keycloak clients:**
- `mybank-frontend` — Authorization Code Flow (secret: `frontend-secret`)
- `accounts-service` — Client Credentials (secret: `accounts-service-secret`, role: `SERVICE_ACCESS`)
- `cash-service` — Client Credentials (secret: `cash-secret`, role: `SERVICE_ACCESS`)
- `transfer-service` — Client Credentials (secret: `transfer-secret`, role: `SERVICE_ACCESS`)

**Test users** (all passwords: `password`): `user1` (Test User), `user2` (Ivan Petrov), `user3` (Anna Sidorova)

## Testing

Tests are written using JUnit 5, Spring Boot Test, Testcontainers (requires Docker running), and Spring Cloud Contract.

| Module | Unit Tests | Integration Tests | Contract Tests | Total |
|--------|-----------|-------------------|----------------|-------|
| Accounts | AccountServiceTest (8) | AccountControllerIntegrationTest (8) | BaseContractTest — producer (3) | 19 |
| Cash | CashServiceTest (3) | CashControllerIntegrationTest (2) | AccountClientContractTest — consumer (2) | 7 |
| Transfer | TransferServiceTest (2) | TransferControllerIntegrationTest (2) | AccountClientContractTest — consumer (3) | 7 |
| Notifications | NotificationServiceTest (1) | NotificationKafkaIntegrationTest (1) | — | 2 |
| Frontend | MainControllerTest (3) | — | — | 3 |

**Contract testing**: Accounts is the producer — defines API contracts (YAML DSL) and generates stubs. Cash and Transfer are consumers — verify their client code against WireMock stubs via `@AutoConfigureStubRunner`.

**Helm tests**: Each microservice sub-chart includes a health-check test pod that verifies the `/actuator/health` endpoint is accessible.

Run Java tests:
```bash
./gradlew clean test
```

Run Helm validation:
```bash
helm lint helm/mybank
helm template mybank helm/mybank
```

Run Helm tests (after deployment):
```bash
helm test mybank -n <namespace>
```

## CI/CD (Jenkins)

Jenkins pipelines are defined as Jenkinsfiles stored in Git:

| Jenkinsfile | Scope | Description |
|-------------|-------|-------------|
| `Jenkinsfile` | All services | Umbrella pipeline — validates, builds, tests, and deploys all microservices |
| `accounts/Jenkinsfile` | Accounts | Individual pipeline for the Accounts service |
| `cash/Jenkinsfile` | Cash | Individual pipeline for the Cash service |
| `transfer/Jenkinsfile` | Transfer | Individual pipeline for the Transfer service |
| `notifications/Jenkinsfile` | Notifications | Individual pipeline for the Notifications service |
| `frontend/Jenkinsfile` | Frontend | Individual pipeline for the Frontend service |

**Pipeline stages:**

1. **Validate** — compile Java sources
2. **Build** — produce JAR artifacts
3. **Test** — run unit, integration, and contract tests
4. **Docker Build & Push** — build and push Docker images to registry
5. **Deploy to Test** — deploy to `test` namespace via Helm
6. **Helm Test** — run health checks in test environment
7. **Approve Production** — manual approval gate
8. **Deploy to Production** — deploy to `prod` namespace via Helm

Per-service pipelines deploy only the changed service image; the umbrella pipeline deploys all services together.

## Prerequisites

- **Rancher Desktop** with container runtime set to **dockerd (moby)**
- **Helm** (v3+, included with Rancher Desktop)
- **Docker CLI** configured to use Rancher Desktop context

## How to Run (Rancher Desktop)

### 1. Switch Docker context to Rancher Desktop

Rancher Desktop runs its own Docker daemon inside a VM. Make sure your Docker CLI points to it:

```bash
docker context use rancher-desktop
```

Verify:
```bash
docker context ls   # rancher-desktop should have asterisk (*)
```

### 2. Build JARs and Docker images

```bash
# Build all service JARs (skip tests for faster build)
./gradlew :accounts:build :cash:build :transfer:build :notifications:build :frontend:build -x test

# Build Docker images (must be in rancher-desktop context)
docker build -t mybank/accounts ./accounts
docker build -t mybank/cash ./cash
docker build -t mybank/transfer ./transfer
docker build -t mybank/notifications ./notifications
docker build -t mybank/frontend ./frontend
```

### 3. Configure /etc/hosts

Add hostname entries for Ingress routing (requires sudo):

```bash
sudo sh -c 'echo "127.0.0.1 frontend.mybank.local" >> /etc/hosts && echo "127.0.0.1 keycloak.mybank.local" >> /etc/hosts && echo "127.0.0.1 kibana.mybank.local" >> /etc/hosts && echo "127.0.0.1 grafana.mybank.local" >> /etc/hosts'
```

### 4. Deploy with Helm

```bash
# Update chart dependencies
helm dependency update helm/mybank

# Deploy to dev namespace
helm install mybank helm/mybank -f helm/mybank/values-dev.yaml -n dev --create-namespace
```

Wait for all pods to become Ready (for my was ~2 minutes):

```bash
kubectl get pods -n dev -w
```

### 5. Seed initial account data

Keycloak users (`user1`, `user2`, `user3`) are created automatically from the realm JSON on first Keycloak startup. However, their corresponding account records in the Accounts database need to be created manually.

To add a new user: first create them in Keycloak (via admin console at http://keycloak.mybank.local), then create a matching account record using the script below.

On first deployment, create accounts for the pre-configured test users:

```bash
# Get a service token from Keycloak
TOKEN=$(kubectl exec deploy/mybank-frontend -n dev -- \
  wget -qO- --post-data='grant_type=client_credentials&client_id=accounts-service&client_secret=accounts-service-secret' \
  http://mybank-keycloak:8080/realms/mybank/protocol/openid-connect/token \
  | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

# Create test accounts
for user in '{"login":"user1","firstName":"Test","lastName":"User","birthDate":"1990-01-01"}' \
            '{"login":"user2","firstName":"Ivan","lastName":"Petrov","birthDate":"1985-05-15"}' \
            '{"login":"user3","firstName":"Anna","lastName":"Sidorova","birthDate":"1992-03-20"}'; do
  kubectl exec deploy/mybank-frontend -n dev -- \
    wget -qO- --header="Authorization: Bearer $TOKEN" \
    --header="Content-Type: application/json" \
    --post-data="$user" http://mybank-accounts:8081/accounts
  echo
done
```

### 6. Access the application

- **Frontend**: http://frontend.mybank.local
- **Keycloak Admin**: http://keycloak.mybank.local (admin / admin)
- **Grafana**: http://grafana.mybank.local (admin / admin)
- **Kibana**: http://kibana.mybank.local
- **Zipkin**: accessible inside the cluster on port 9411

Log in with test users (all passwords: `password`): `user1`, `user2`, `user3`

### Multi-environment deployment

Deploy to different namespaces for environment separation:

```bash
# Dev
helm install mybank helm/mybank -f helm/mybank/values-dev.yaml -n dev --create-namespace

# Test
helm install mybank helm/mybank -f helm/mybank/values-test.yaml -n test --create-namespace

# Prod (update secrets in values-prod.yaml first!)
helm install mybank helm/mybank -f helm/mybank/values-prod.yaml -n prod --create-namespace
```

### Upgrade an existing release

After code changes, rebuild images and upgrade:

```bash
./gradlew :accounts:build -x test
docker build -t mybank/accounts ./accounts
helm upgrade mybank helm/mybank -f helm/mybank/values-dev.yaml -n dev
kubectl rollout restart deployment/mybank-accounts -n dev
```

### Run Helm tests

```bash
helm test mybank -n dev
```

### Uninstall

```bash
helm uninstall mybank -n dev
```
