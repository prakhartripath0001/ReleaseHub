# ReleaseHub — Implementation Plan

> **Horizon:** 5 weeks, one milestone per week.  
> Each phase lists exact tasks, files to create, and a clear definition of done.

---

## Table of Contents

1. [Phase 0 — Project Bootstrap](#phase-0--project-bootstrap-day-1)
2. [Phase 1 — Catalog Service](#phase-1--catalog-service-week-1)
3. [Phase 2 — Search](#phase-2--search-week-2)
4. [Phase 3 — Feedback and Kafka](#phase-3--feedback-and-kafka-week-3)
5. [Phase 4 — Gateway and Deployment](#phase-4--gateway-and-deployment-week-4)
6. [Phase 5 — Observability and Polish](#phase-5--observability-and-polish-week-5)
7. [Stretch Goals](#stretch-goals-post-week-5)

---

## Phase 0 — Project Bootstrap (Day 1)

Before writing any service code, set up the mono-repo skeleton and shared infrastructure.

### Tasks

**Repository layout**

```
releasehub/
  catalog-service/
  search-service/        # or a module inside catalog
  feedback-service/
  analytics-service/
  gateway/               # Kong declarative config
  infra/
    docker-compose.yml
    k8s/
  docs/
    prd.md
    plan.md
    architecture.md
  .github/workflows/
    ci.yml
```

**Shared setup**

- [ ] Create the GitHub repository and push the skeleton.
- [ ] Add a root `README.md` with a one-command startup guide.
- [ ] Add `.gitignore` for Java, Docker, and IDE files.
- [ ] Add `docker-compose.yml` with:
  - MySQL (port 3306)
  - Kafka + Zookeeper (or Redpanda as a single-container alternative)
  - Elasticsearch (port 9200)
  - Prometheus (port 9090)
  - Grafana (port 3000)
  - Loki
- [ ] Verify `docker compose up` starts all infra services cleanly.
- [ ] Bootstrap each Spring Boot service using [start.spring.io](https://start.spring.io) (Java 21, Maven or Gradle, Spring Web, Actuator, Validation, Testcontainers).

**Definition of done**

- `docker compose up` starts with no errors.
- Each service has a `/actuator/health` endpoint returning `UP`.
- Repository is pushed to GitHub.

---

## Phase 1 — Catalog Service (Week 1)

Build the core CRUD API for `Software` and `Version`, with full validation, error handling, and unit tests.

### Stack additions this phase

- Spring Data JPA + Flyway (database migrations)
- MySQL (already in Docker Compose)
- MapStruct (DTO mapping)
- JUnit 5 + Mockito
- Testcontainers (integration tests)

### Tasks

**Database schema**

- [ ] Create Flyway migration `V1__init.sql`:
  ```sql
  CREATE TABLE software (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          TEXT NOT NULL,
    description   TEXT,
    category      TEXT,
    tags          TEXT[],
    publisher_id  UUID NOT NULL,
    created_at    TIMESTAMPTZ DEFAULT now()
  );

  CREATE TABLE version (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    software_id     UUID NOT NULL REFERENCES software(id),
    version_number  TEXT NOT NULL,
    os              TEXT,
    release_notes   TEXT,
    file_ref        TEXT,
    released_at     TIMESTAMPTZ DEFAULT now()
  );

  CREATE TABLE download_event (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id  UUID NOT NULL REFERENCES version(id),
    user_id     UUID,
    ts          TIMESTAMPTZ DEFAULT now()
  );
  ```

**Domain and persistence layer**

- [ ] JPA entities: `SoftwareEntity`, `VersionEntity`, `DownloadEventEntity`.
- [ ] Spring Data repositories: `SoftwareRepository`, `VersionRepository`.
- [ ] DTOs (request / response) and MapStruct mappers.

**Service layer**

- [ ] `SoftwareService`: create, update, delete, findById, findAll (with `Pageable` + `Specification` for filtering).
- [ ] `VersionService`: create version, list versions newest-first, delete.
- [ ] `DownloadService`: record download event, resolve download URL.

**REST controllers**

- [ ] `SoftwareController` — `POST /software`, `GET /software`, `GET /software/{id}`, `PUT /software/{id}`, `DELETE /software/{id}`.
- [ ] `VersionController` — `POST /software/{id}/versions`, `GET /software/{id}/versions`, `GET /versions/{id}`.
- [ ] `DownloadController` — `GET /download/{versionId}`.

**Cross-cutting**

- [ ] Global `@ControllerAdvice` for validation errors, 404, and 500 — returning a consistent `ErrorResponse` body.
- [ ] Role check: write endpoints require `PUBLISHER` or `ADMIN` role (use a simple header-based role resolver for now; Kong JWT comes in Phase 4).

**Testing**

- [ ] Unit tests for all service methods (Mockito mocks for repositories).
- [ ] Integration tests with `@SpringBootTest` + Testcontainers MySQL for each controller.
- [ ] JaCoCo plugin configured; build fails below 80%.

**Definition of done**

- All CRUD endpoints return correct responses and HTTP codes.
- Filtering (`?os=LINUX&category=tools`) and pagination work.
- Download endpoint records the event and returns a URL.
- Unit and integration tests pass; coverage >= 80%.

---

## Phase 2 — Search (Week 2)

Index software and version metadata in Elasticsearch and expose a fuzzy, faceted search endpoint.

### Stack additions this phase

- Spring Data Elasticsearch (or the REST High-Level Client / new Java API client)
- Elasticsearch 8.x (already in Docker Compose)

### Tasks

**Index design**

- [ ] Define the `software` index mapping:
  ```json
  {
    "mappings": {
      "properties": {
        "name":        { "type": "text", "boost": 3 },
        "description": { "type": "text" },
        "category":    { "type": "keyword" },
        "tags":        { "type": "keyword" },
        "os":          { "type": "keyword" }
      }
    }
  }
  ```
- [ ] Use index aliases (`software_v1` aliased to `software`) to allow zero-downtime reindex.

**Indexer**

- [ ] `SearchIndexer` service:
  - Triggered by an application event published from `SoftwareService` on create/update/delete.
  - Calls Elasticsearch to upsert or delete the document.
- [ ] `ReindexJob`: reads all software from MySQL and bulk-indexes to a new index version, then atomically swaps the alias.

**Search endpoint**

- [ ] `GET /search?q=&os=&category=&page=&size=`
  - Multi-match query across `name` and `description` with `fuzziness: AUTO`.
  - Filter by `os` and `category` (term filters).
  - Aggregations for facets: `os` and `category` buckets returned alongside hits.
- [ ] Response includes hits (with score) and facet counts.

**Testing**

- [ ] Integration tests with Testcontainers Elasticsearch:
  - Index a known set of documents.
  - Assert fuzzy match returns the correct document.
  - Assert facet counts are correct.
- [ ] Test the alias-swap reindex path without downtime.

**Definition of done**

- Fuzzy search returns relevant results for intentional typos.
- Facet counts match data in the index.
- Creating or updating a software entry automatically updates the index.
- Full reindex can run without taking the endpoint offline.

---

## Phase 3 — Feedback and Kafka (Week 3)

Implement the feedback API with the transactional outbox pattern, analytics consumer, and dead-letter handling.

### Stack additions this phase

- Spring Kafka
- Kafka (already in Docker Compose)
- Scheduled outbox relay

### Tasks

**Database schema (feedback service)**

- [ ] Flyway migration `V2__feedback.sql`:
  ```sql
  CREATE TABLE feedback (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version_id  UUID NOT NULL,
    user_id     UUID NOT NULL,
    rating      SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TIMESTAMPTZ DEFAULT now(),
    UNIQUE (user_id, version_id)
  );

  CREATE TABLE outbox (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id  UUID NOT NULL,
    event_type    TEXT NOT NULL,
    payload       JSONB NOT NULL,
    created_at    TIMESTAMPTZ DEFAULT now(),
    published_at  TIMESTAMPTZ
  );
  ```

**Feedback API**

- [ ] `POST /feedback` — validate, save feedback + outbox record in a single `@Transactional` block.
- [ ] `GET /feedback/{softwareId}` — list feedback for a software entry.

**Outbox relay**

- [ ] `@Scheduled` job runs every 500 ms, reads unpublished outbox rows, publishes to Kafka, marks as published.
- [ ] On publish failure, leave the row for the next cycle (at-least-once guarantee).

**Kafka topics**

- [ ] `feedback.submitted` (3 partitions, replication factor 1 for local).
- [ ] `download.completed` — also published from the catalog service download endpoint.
- [ ] `feedback.submitted.dlq` and `download.completed.dlq`.

**Analytics service**

- [ ] `FeedbackConsumer` — `@KafkaListener` on `feedback.submitted`:
  - Upsert `report_aggregate` row (downloads, avg_rating, feedback_count).
  - Idempotent: keyed on `feedbackId`; use a `processed_event` table or check before inserting.
  - On repeated failure (3 retries), publish to DLQ.
- [ ] `DownloadConsumer` — same pattern for `download.completed`.
- [ ] `GET /reports/{softwareId}` — returns aggregated data.

**Testing**

- [ ] Integration test: submit feedback via HTTP, assert outbox row created, assert Kafka message received (use embedded Kafka or Testcontainers Kafka).
- [ ] Consumer test: publish a message to the topic, assert aggregate is updated.
- [ ] Failure test: publish a malformed message, assert it lands in the DLQ after retries.

**Definition of done**

- Feedback submission is atomic (DB + outbox).
- Analytics service reflects new feedback within one polling cycle.
- Duplicate messages do not corrupt aggregates.
- Failed messages go to the DLQ; nothing is silently dropped.

---

## Phase 4 — Gateway and Deployment (Week 4)

Wire Kong as the single public entry point, containerize all services, and deploy to Kubernetes.

### Stack additions this phase

- Kong (DB-less, declarative config)
- Docker multi-stage builds
- Kubernetes manifests (or Helm)
- GitHub Actions

### Tasks

**Kong configuration (`gateway/kong.yml`)**

- [ ] Declare services and routes for `/api/catalog`, `/api/search`, `/api/feedback`, `/api/reports`.
- [ ] Enable `jwt` plugin globally (or per route) for authentication.
- [ ] Enable `rate-limiting` plugin (e.g., 100 req/min per consumer).
- [ ] Enable `cors` plugin.
- [ ] Enable `request-id` plugin to inject `X-Correlation-ID` on every request.
- [ ] Add Kong to `docker-compose.yml` and verify routing works locally.

**Dockerfiles**

- [ ] Multi-stage `Dockerfile` per service:
  ```dockerfile
  # Stage 1 — build
  FROM eclipse-temurin:21-jdk AS build
  WORKDIR /app
  COPY . .
  RUN ./mvnw package -DskipTests

  # Stage 2 — runtime
  FROM eclipse-temurin:21-jre
  RUN adduser --system appuser
  USER appuser
  COPY --from=build /app/target/*.jar app.jar
  ENTRYPOINT ["java", "-jar", "app.jar"]
  ```
- [ ] Build and push images to GitHub Container Registry (GHCR) via CI.

**Kubernetes manifests (`infra/k8s/`)**

- [ ] `Deployment` + `Service` for each microservice.
- [ ] `ConfigMap` for non-sensitive config (Kafka bootstrap, Elasticsearch URL).
- [ ] `Secret` for database credentials and JWT signing key.
- [ ] `Ingress` using the Kong Ingress Controller.
- [ ] `HorizontalPodAutoscaler` for catalog-service and feedback-service (CPU target: 60%).
- [ ] Readiness probe: `GET /actuator/health/readiness`.
- [ ] Liveness probe: `GET /actuator/health/liveness`.

**GitHub Actions (`ci.yml`)**

```
Trigger: push to main / pull request

Jobs:
  build-and-test:
    - Checkout
    - Set up JDK 21
    - Run tests (Testcontainers via Docker-in-Docker)
    - JaCoCo coverage gate (fail below 80%)

  build-and-push (on main only):
    - Build Docker image
    - Push to GHCR
```

**Definition of done**

- All API calls routed through Kong; direct service ports are unexposed.
- JWT-authenticated requests succeed; unauthenticated requests return 401.
- Rate limit kicks in after the configured threshold.
- `kubectl apply -f infra/k8s/` deploys the full stack on minikube/kind.
- HPA is present and functional.
- CI pipeline is green.

---

## Phase 5 — Observability and Polish (Week 5)

Add dashboards, alerts, structured logging, and finalize documentation.

### Stack additions this phase

- Micrometer + Prometheus
- Grafana dashboards (JSON provisioning)
- Loki + Logback JSON appender
- k6 load test script

### Tasks

**Metrics**

- [ ] Add `spring-boot-starter-actuator` and `micrometer-registry-prometheus` to each service.
- [ ] Expose `/actuator/prometheus` scrape endpoint.
- [ ] Add `prometheus.yml` scrape config for all services.
- [ ] Add custom counters/timers for: download events, feedback submissions, Kafka consumer lag.

**Grafana dashboards**

Provision JSON dashboards under `infra/grafana/dashboards/`:

- [ ] **Service Overview:** request rate (rpm), error rate (%), p50/p95/p99 latency per service.
- [ ] **JVM Health:** heap usage, GC pause time, thread count per service.
- [ ] **Kafka:** consumer lag per topic, messages in/out per second, DLQ message count.
- [ ] **Business Metrics:** downloads per hour, average rating per software, feedback count over time.

**Alerts (`infra/prometheus/alerts.yml`)**

- [ ] Error rate > 5% for 5 minutes — severity: warning.
- [ ] Kafka consumer lag > 1000 for 2 minutes — severity: warning.
- [ ] Pod restart count > 3 in 10 minutes — severity: critical.

**Structured logging**

- [ ] Add `logstash-logback-encoder` dependency to each service.
- [ ] Configure `logback-spring.xml` to output JSON with fields: `timestamp`, `level`, `service`, `correlationId`, `message`.
- [ ] Add a `CorrelationIdFilter` that extracts `X-Correlation-ID` from the request and stores it in MDC.
- [ ] Configure Loki data source in Grafana; verify log queries work with label `{service="catalog-service"}`.

**Load test**

- [ ] Write a k6 script (`infra/k6/load-test.js`) that:
  - Runs 50 virtual users for 2 minutes.
  - Hits `GET /api/search?q=...` and `GET /api/download/{id}`.
  - Asserts p95 < 300 ms.
- [ ] Run the test against the Kubernetes deployment and capture the HPA scaling event in a screenshot.

**Documentation**

- [ ] `docs/architecture.md` — component diagram (Mermaid) + data flow description.
- [ ] Root `README.md` — prerequisites, one-command startup, endpoint reference, running tests, CI badge.
- [ ] Add Swagger/OpenAPI (`springdoc-openapi`) to each service; verify `/v3/api-docs` and `/swagger-ui.html`.

**Definition of done**

- Grafana dashboards load with live data during the k6 load test.
- Alerts fire correctly in a simulated failure (kill a pod, pause Kafka).
- Logs in Loki are queryable by correlation ID across services.
- k6 p95 latency for search is under 300 ms.
- HPA scales catalog-service up during the load test.
- README enables a first-time user to run the stack from scratch.

---

## Stretch Goals (Post-Week 5)

| Goal | Notes |
|------|-------|
| Minimal React frontend | Search bar, results list, download button, 1–5 star rating form |
| New-version notifications | Publisher triggers an event; users subscribed to a product receive an email or webhook |
| Download heatmaps in Grafana | Geo-IP enrichment on download events, displayed on a world map panel |
| Search-term analytics | Store query strings, surface top terms to publishers, feed into relevance tuning |
| PDF export for reports | Scheduled job generates a PDF via JasperReports or iText and stores it in MinIO |

---

## Quick Reference — Dependency Order

```
Phase 0 (infra up)
   |
   +-- Phase 1 (Catalog API + DB schema)
         |
         +-- Phase 2 (Search — reads from catalog DB, indexes to ES)
         |
         +-- Phase 3 (Feedback + Kafka — catalog publishes download events)
               |
               +-- Phase 4 (Kong routes all services; Docker + K8s)
                     |
                     +-- Phase 5 (Observability across all services)
```

Each phase has a clear entry point (previous phase done), concrete tasks, and a binary definition of done. If a phase runs over time, the stretch goals are the first thing to drop — not the tests or observability.

