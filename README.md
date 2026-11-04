# ReleaseHub

> A modern software distribution, cataloging, and release analytics platform built with **Spring Boot** and **MySQL 8**.

ReleaseHub mirrors a real-world software downloads and versioning hub: publishers upload and manage software releases, users discover and download specific versions, and feedback/download events drive real-time analytics.

---

## Tech Stack

| Layer | Technologies |
|---|---|
| **Backend** | Java 21, Spring Boot 4.x, Spring Data JPA, Hibernate, MapStruct |
| **Database & Migrations** | **MySQL 8**, Flyway |
| **Search (Phase 2)** | Elasticsearch / OpenSearch |
| **Messaging (Phase 3)** | Apache Kafka (Transactional Outbox Pattern) |
| **API Gateway (Phase 4)** | Kong Gateway |
| **Testing** | JUnit 5, Mockito, Testcontainers (MySQL), JaCoCo |
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
│ Catalog Service  ││ Feedback Service ││ Analytics Service│
│ (Software, Ver,  ││ (Outbox Pattern) ││ (Event Consumer) │
│  Downloads)      ││                  ││                  │
└────────┬─────────┘└────────┬─────────┘└────────┬─────────┘
         │                   │                   │
         ▼                   ▼                   ▼
    ┌─────────┐         ┌─────────┐         ┌─────────┐
    │ MySQL 8 │         │  Kafka  │         │Prometheus│
    └─────────┘         └─────────┘         └─────────┘
```

---

## Catalog Service Package Structure

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

---

## Quick Start

### 1. Prerequisites
- **JDK 21** or higher
- **Docker** and **Docker Compose**

### 2. Start Infrastructure (MySQL 8)
From the repository root:
```bash
cd infra
docker compose up -d
```
This spins up the MySQL 8 container with:
- **Port:** `3306`
- **Database:** `releasehub`
- **User:** `root`
- **Password:** `release@123`
- **Volume:** `releasehub_db` (Docker managed named volume)

### 3. Run Catalog Service
```bash
cd catalog-service
./mvnw spring-boot:run
```
Flyway will automatically execute database migrations (`V1__init.sql`) on startup.

---

## API Reference & Sample Requests

### Software Endpoints

- **Create Software** *(Requires `X-Role: PUBLISHER` or `ADMIN`)*
  ```bash
  curl -X POST http://localhost:8080/software \
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
  curl "http://localhost:8080/software?category=developer-tools&page=0&size=10"
  ```

- **Get Software by ID**
  ```bash
  curl http://localhost:8080/software/{softwareId}
  ```

- **Update Software** *(Requires `X-Role: PUBLISHER` or `ADMIN`)*
  ```bash
  curl -X PUT http://localhost:8080/software/{softwareId} \
    -H "Content-Type: application/json" \
    -H "X-Role: PUBLISHER" \
    -d '{
      "description": "Updated description",
      "tags": ["api", "testing", "rest"]
    }'
  ```

- **Delete Software** *(Requires `X-Role: PUBLISHER` or `ADMIN`)*
  ```bash
  curl -X DELETE http://localhost:8080/software/{softwareId} \
    -H "X-Role: ADMIN"
  ```

### Version Endpoints

- **Create New Version** *(Requires `X-Role: PUBLISHER` or `ADMIN`)*
  ```bash
  curl -X POST http://localhost:8080/software/{softwareId}/versions \
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
  curl http://localhost:8080/software/{softwareId}/versions
  ```

### Download Endpoints

- **Download / Record Event**
  ```bash
  curl http://localhost:8080/download/{versionId} \
    -H "X-User-Id: user-123"
  ```

---

## Running Tests

Unit tests and validation can be executed with:
```bash
cd catalog-service
./mvnw test
```

Includes:
- Unit tests for all services with Mockito (`SoftwareServiceTest`, `VersionServiceTest`, `DownloadServiceTest`)
- JaCoCo test coverage enforcement (configured for >= 80% line coverage)

---

## Implementation Roadmap

- [x] **Phase 0:** Project bootstrap, Docker Compose, MySQL 8 setup
- [x] **Phase 1:** Catalog Service CRUD, modular architecture, validation, MySQL Flyway migrations
- [ ] **Phase 2:** Search service with Elasticsearch (fuzzy search, facets, alias swap)
- [ ] **Phase 3:** Feedback API, Kafka integration, Transactional Outbox pattern, Analytics consumer
- [ ] **Phase 4:** Kong API Gateway (routing, JWT authentication, rate limiting) & Kubernetes deployment
- [ ] **Phase 5:** Observability (Prometheus, Grafana, Loki), alerts, and k6 load testing
