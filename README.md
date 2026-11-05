# ReleaseHub

> A modern software distribution, cataloging, search, and release analytics platform built with **Spring Boot**, **MySQL 8**, and **Elasticsearch 8**.

ReleaseHub mirrors a real-world software downloads and versioning hub: publishers upload and manage software releases, users discover and download specific versions, and feedback/download events drive real-time analytics.

---

## Tech Stack

| Layer | Technologies |
|---|---|
| **Backend** | Java 21, Spring Boot 4.x, Spring Data JPA, Hibernate, MapStruct |
| **Database & Migrations** | **MySQL 8**, Flyway |
| **Search (Phase 2)** | Elasticsearch 8, Elasticsearch Java API Client |
| **Messaging (Phase 3)** | Apache Kafka, transactional outbox, dead-letter topics |
| **API Gateway (Phase 4)** | Kong Gateway |
| **Testing** | JUnit 5, Mockito, Testcontainers (MySQL, Elasticsearch), JaCoCo |
| **Observability (Phase 5)**| Spring Boot Actuator, Micrometer, Prometheus, Grafana |
| **Containerization** | Docker, Docker Compose, Kubernetes |

---

## System Architecture

```
                  ┌──────────────────────┐
                  │     Client / UI      │
                  └──────────┬───────────┘
                             │
                             ▼
                  ┌──────────────────────┐
                  │  Kong API Gateway    │
                  └──────────┬───────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         ▼                   ▼                   ▼
┌──────────────────┐┌──────────────────┐┌──────────────────┐
│ Catalog Service  ││  Search Service  ││ Feedback Service │
│ (Software, Ver,  ││ (Fuzzy Search,   ││ (Outbox Pattern) │
│  Downloads)      ││  Facets, Index)  ││                  │
└────────┬─────────┘└────────┬─────────┘└────────┬─────────┘
         │                   │                   │
         ▼                   ▼                   ▼
    ┌─────────┐         ┌──────────────┐    ┌─────────┐
    │ MySQL 8 │         │Elasticsearch │    │  Kafka  │
    └────┬────┘         └──────────────┘    └────┬────┘
         │                                        │
         └──────────────┐           ┌────────────┘
                        ▼           ▼
                 ┌────────────┐ ┌───────────────┐
                 │ Feedback   │ │ Analytics      │
                 │ Service    │ │ Service        │
                 └────────────┘ └───────────────┘
```

## Service Package Structure

`catalog-service` is organized using a **module-based (package-by-feature / vertical slice)** architecture:

```
catalog-service/
├── src/main/java/com/yourapp/catalog/
│   ├── CatalogServiceApplication.java
│   │
│   ├── software/                # Software Module (CRUD, filtering, search specs)
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   ├── mapper/
│   │   └── specification/
│   │
│   ├── version/                 # Version Module (releases, OS, notes)
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   └── mapper/
│   │
│   ├── download/                # Download Module (events, download URL resolution)
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   └── dto/
│   │
│   └── common/                  # Cross-Cutting Shared Modules
│       ├── config/              # Web MVC, CORS
│       ├── dto/                 # Generic ErrorResponse, PageResponse
│       ├── exception/           # GlobalExceptionHandler & custom exceptions
│       └── security/            # RoleCheckFilter (Publisher/Admin guard)
```

The separate `search-service` uses the same feature-oriented approach:

```
search-service/src/main/java/com/search_service/
├── catalog/      # Read-only MySQL catalog entities and repositories
├── config/       # Elasticsearch client and index configuration
├── index/        # Search document, mapping/index management, bootstrap
├── indexer/      # Event-driven indexing and full reindex job
├── search/       # Fuzzy query, facets, and response models
└── web/          # Search, internal indexing, and reindex endpoints
```

`feedback-service` and `analytics-service` follow feature-oriented packages as well:

```
feedback-service/src/main/java/com/releasehub/feedback/
├── catalog/      # Read-only lookup of catalog versions in shared MySQL
├── feedback/     # Feedback API, persistence, validation, and event model
├── outbox/       # Transactional event storage and scheduled Kafka relay
└── web/          # API error mapping

analytics-service/src/main/java/com/analytics_service/
├── config/       # Kafka topics and retry/DLQ handling
├── consumer/     # Kafka listeners and idempotent event processing
├── event/        # Consumed event contracts
└── report/       # Aggregated report query API
```

---

## Quick Start

### 1. Prerequisites
- **JDK 21** or higher
- **Docker** and **Docker Compose**

### 2. Start the complete stack
From the repository root, build and start MySQL, Elasticsearch, Kafka, all four services, Kong, and the monitoring stack:
```bash
docker compose -f infra/docker-compose.yml up --build -d
```
Catalog applies the Flyway migrations and provisions Kafka topics during startup. The services are available directly for manual tests on ports `8083`–`8086`; Kong is available on port `8000`. Infrastructure ports are MySQL `3307`, Elasticsearch `9200`, Kafka `9092`, Prometheus `9090`, Grafana `3000`, Loki `3100`, and Kafka Exporter `9308`.

Check startup with `docker compose -f infra/docker-compose.yml ps` and service logs with `docker compose -f infra/docker-compose.yml logs -f catalog-service`.

### 3. Run all API tests
After the containers report healthy/running:
```bash
./api-tests/run-tests.sh all
```
The script tests the catalog, search, feedback/analytics, version, download, and role/error APIs against the direct service ports. To run one suite, replace `all` with `software`, `search`, `feedback`, `versions`, `downloads`, or `auth`. See [`docs/api-testing.md`](docs/api-testing.md) for the suite data/setup and request details.

### 4. Configure local gateway JWT
Kong is configured DB-less, so its public-key credential must be part of its declarative configuration. Generate a local RS256 key pair, render the local Kong config, and restart Kong with that config:
```bash
./gateway/generate-local-jwt-key.sh
./gateway/register-local-jwt.sh
KONG_DECLARATIVE_CONFIG=/kong/keys/kong.local.yml docker compose -f infra/docker-compose.yml up -d --force-recreate kong
TOKEN="$(./gateway/create-local-token.sh)"
curl -H "Authorization: Bearer $TOKEN" http://localhost:8000/api/search?q=release
```
The private key is ignored by Git. Direct ports `8083`–`8086` remain available for the API test scripts; external clients should use Kong. Do not use the example development credentials or expose Kong's Admin API outside localhost.

### 5. Observability
- Grafana: `http://localhost:3000` (local development login: `admin` / `admin`)
- Prometheus: `http://localhost:9090`
- Springdoc UI: `http://localhost:8083/swagger-ui/index.html` (also available on the other service ports)
- Metrics: `http://localhost:8083/actuator/prometheus` (change the port for the other services)

The provisioned Grafana dashboard is named **ReleaseHub Overview**. Promtail sends container logs to Loki; use the Grafana Loki data source to query `{service="catalog-service"}` or search for a `correlationId` field.

### 6. Kubernetes deployment
The manifests deploy the four services and Kong. MySQL, Elasticsearch, and Kafka are external prerequisites; update the `releasehub-config` ConfigMap endpoints for your cluster and ensure the GHCR images are accessible. Create the namespace and credential Secret before applying the remaining manifests:
```bash
kubectl apply -f infra/k8s/00-namespace.yaml
kubectl -n releasehub create secret generic releasehub-secrets \
  --from-literal=DB_PASSWORD='<mysql-password>'
kubectl apply -f infra/k8s/10-services.yaml
kubectl apply -f infra/k8s/20-gateway.yaml
kubectl apply -f infra/k8s/30-autoscaling.yaml
```
Install the Kong Ingress Controller and Kubernetes Metrics Server first, and configure the controller to use the internal Admin API at `http://kong-admin.releasehub.svc.cluster.local:8001`. Add the JWT consumer and its public key to the Kong declarative configuration before relying on authenticated gateway traffic; the local key scripts are for Compose development only. No private key or production password belongs in the committed manifests.

---

## API Reference & Sample Requests

> For the complete step-by-step manual testing guide with sample responses and error checks, see [`docs/api-testing.md`](docs/api-testing.md).

### Software Endpoints

- **Create Software** *(Requires `X-Role: PUBLISHER` or `ADMIN`)*
  ```bash
  curl -X POST http://localhost:8083/software \
    -H "Content-Type: application/json" \
    -H "X-Role: PUBLISHER" \
    -d '{
      "name": "Postman",
      "description": "API platform for building and using APIs",
      "category": "developer-tools",
      "tags": ["api", "testing", "http"],
      "publisherId": "pub-001"
    }'
  ```

- **List / Filter Software** *(Paginated)*
  ```bash
  curl "http://localhost:8083/software?category=developer-tools&page=0&size=10"
  ```

- **Get Software by ID**
  ```bash
  curl http://localhost:8083/software/{softwareId}
  ```

- **Update Software** *(Requires `X-Role: PUBLISHER` or `ADMIN`)*
  ```bash
  curl -X PUT http://localhost:8083/software/{softwareId} \
    -H "Content-Type: application/json" \
    -H "X-Role: PUBLISHER" \
    -d '{
      "description": "Updated description",
      "tags": ["api", "testing", "rest"]
    }'
  ```

- **Delete Software** *(Requires `X-Role: PUBLISHER` or `ADMIN`)*
  ```bash
  curl -X DELETE http://localhost:8083/software/{softwareId} \
    -H "X-Role: ADMIN"
  ```

### Version Endpoints

- **Create New Version** *(Requires `X-Role: PUBLISHER` or `ADMIN`)*
  ```bash
  curl -X POST http://localhost:8083/software/{softwareId}/versions \
    -H "Content-Type: application/json" \
    -H "X-Role: PUBLISHER" \
    -d '{
      "versionNumber": "1.0.0",
      "os": "macOS",
      "releaseNotes": "Initial release",
      "fileRef": "https://downloads.example.com/postman-1.0.0.dmg"
    }'
  ```

- **List Versions for Software** *(Sorted newest first)*
  ```bash
  curl http://localhost:8083/software/{softwareId}/versions
  ```

### Download Endpoints

- **Download / Record Event**
  ```bash
  curl http://localhost:8083/download/{versionId} \
    -H "X-User-Id: user-123"
  ```

### Search Endpoints

- **Fuzzy search with OS/category facets**
  ```bash
  curl "http://localhost:8084/search?q=Postman&os=LINUX&category=developer-tools&page=0&size=20"
  ```
  Search supports typo-tolerant matching across software names and descriptions, with name matches ranked higher. The response includes `hits`, `total`, pagination fields, and `facets.os` / `facets.category` buckets. `q`, `os`, and `category` are optional.

- **Rebuild the search index**
  ```bash
  curl -X POST http://localhost:8084/admin/reindex
  ```
  Reindexing bulk-loads the catalog into a new versioned index, then atomically switches the `software` alias.

### Feedback and Analytics Endpoints

- **Submit feedback** *(one feedback per user per version; rating 1-5)*
  ```bash
  curl -i -X POST http://localhost:8085/feedback \
    -H "Content-Type: application/json" \
    -d '{"versionId":"<version-id>","userId":"<user-id>","rating":5,"comment":"Excellent release"}'
  ```

- **List software feedback**
  ```bash
  curl http://localhost:8085/feedback/<software-id>
  ```

- **Get analytics report** *(asynchronous; allow time for Kafka consumption)*
  ```bash
  curl http://localhost:8086/reports/<software-id>
  ```

---

## Running Tests

### Automated Unit Tests
```bash
cd catalog-service
./mvnw test
```

### Automated API Test Suite
A modular bash test suite is located in the [`api-tests/`](api-tests/) directory. Run all test suites or target a specific module:

```bash
# Run all test suites against port 8083 (or export BASE_URL)
./api-tests/run-tests.sh

# Or run individual modules:
./api-tests/run-tests.sh software
./api-tests/run-tests.sh search
./api-tests/run-tests.sh feedback
./api-tests/run-tests.sh versions
./api-tests/run-tests.sh downloads
./api-tests/run-tests.sh auth
```

The search suite requires the catalog and search services plus Elasticsearch. The feedback suite requires the catalog, feedback, and analytics services plus Kafka. Override defaults with `BASE_URL`, `SEARCH_BASE_URL`, `FEEDBACK_BASE_URL`, and `ANALYTICS_BASE_URL`.

Includes:
- Unit tests for all services with Mockito (`SoftwareServiceTest`, `VersionServiceTest`, `DownloadServiceTest`)
- Search integration tests with Testcontainers for MySQL and Elasticsearch
- Feedback/outbox and analytics unit tests; API checks in `api-tests/tests/feedback.sh`
- JaCoCo test coverage enforcement (configured for >= 80% line coverage)

---

## Implementation Roadmap

- [x] **Phase 0:** Project bootstrap, Docker Compose, MySQL 8 setup
- [x] **Phase 1:** Catalog Service CRUD, modular architecture, validation, MySQL Flyway migrations
- [x] **Phase 2:** Search service with Elasticsearch (fuzzy search, facets, alias swap)
- [x] **Phase 3:** Feedback API, Kafka integration, Transactional Outbox pattern, Analytics consumer
- [ ] **Phase 4:** Kong API Gateway (routing, JWT authentication, rate limiting) & Kubernetes deployment
- [ ] **Phase 5:** Observability (Prometheus, Grafana, Loki), alerts, and k6 load testing
