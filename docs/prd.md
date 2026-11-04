# ReleaseHub — Product Requirements Document

> **Version:** 1.0 (Draft) &nbsp;|&nbsp; **Type:** Portfolio / Learning Project  
> **Stack:** Java · Spring Boot · MySQL · Elasticsearch · Kafka · Kong · Docker · Kubernetes · Prometheus · Grafana

---

## Table of Contents

1. [Overview](#1-overview)
2. [Problem Statement](#2-problem-statement)
3. [Goals and Non-Goals](#3-goals-and-non-goals)
4. [Target Users and Personas](#4-target-users-and-personas)
5. [User Stories](#5-user-stories)
6. [Functional Requirements](#6-functional-requirements)
7. [Non-Functional Requirements](#7-non-functional-requirements)
8. [System Architecture](#8-system-architecture)
9. [Data Model](#9-data-model-summary)
10. [Event Contracts](#10-event-contracts)
11. [Observability and Operations](#11-observability-and-operations)
12. [Testing Strategy](#12-testing-strategy)
13. [Deployment](#13-deployment)
14. [Milestones](#14-milestones)
15. [Success Metrics](#15-success-metrics)
16. [Risks and Mitigations](#16-risks-and-mitigations)
17. [Stretch Goals](#17-stretch-goals)
18. [Open Questions](#18-open-questions)

---

## 1. Overview

**ReleaseHub** is a software distribution and feedback platform. Publishers upload software releases, users search for and download them, and users rate and review what they download. Feedback and download events flow through Kafka into an analytics service that produces reports for publishers.

The project mirrors a real-world software downloads page: a catalog API, indexed search, an external API exposed through a gateway, a feedback pipeline, unit testing, observability, and containerized deployment.

---

## 2. Problem Statement

Users struggle to find the right software version and to know whether it is reliable. Publishers have no structured view of how their releases are received. Existing download pages often combine weak search, no feedback loop, and no usable analytics.

---

## 3. Goals and Non-Goals

### Goals

- Provide a fast, relevant search over software and versions.
- Let users download releases and submit ratings and comments.
- Give publishers aggregated analytics (downloads, ratings, trends).
- Expose all public APIs through a single secured gateway.
- Be observable, tested, and deployable with one command.

### Non-Goals

- Payments or licensing.
- Full user account management *(a simple JWT or API-key scheme is enough)*.
- Hosting very large binaries *(use metadata plus links, or MinIO for small files)*.
- A polished production UI *(a minimal React frontend is a stretch goal)*.

---

## 4. Target Users and Personas

| Persona | Description | Key Needs |
|---------|-------------|-----------|
| **End User** | Developer or IT user looking for software | Find the right version quickly, download it, see what others think |
| **Publisher** | Team that owns software releases | Upload releases, see download and rating trends |
| **Admin** | Platform operator | Monitor health, manage content, review logs and alerts |

---

## 5. User Stories

1. As an **end user**, I can search by name, tag, or keyword and tolerate typos, so I find software even with imperfect queries.
2. As an **end user**, I can filter results by operating system, category, and version.
3. As an **end user**, I can download a specific version of a product.
4. As an **end user**, I can submit a star rating and a comment for a version.
5. As a **publisher**, I can create a software entry and publish new versions.
6. As a **publisher**, I can view reports showing downloads per day, average rating per version, and top search terms.
7. As an **admin**, I can see request rates, error rates, latency, and Kafka consumer lag in dashboards.
8. As an **admin**, I receive an alert when the error rate exceeds the threshold.

---

## 6. Functional Requirements

### 6.1 Catalog Service

| ID | Requirement |
|----|-------------|
| FR-1 | CRUD for **Software** (`name`, `description`, `category`, `tags`, `publisher`) and **Version** (`version_number`, `os`, `release_notes`, `file_ref`, `release_date`). |
| FR-2 | `GET /software` supports pagination, sorting, and filtering by OS, category, and tag. |
| FR-3 | `GET /software/{id}/versions` lists versions, newest first. |
| FR-4 | `GET /download/{versionId}` records a download event and returns a download URL or redirect. |
| FR-5 | Write endpoints are restricted to publishers and admins. |
| FR-6 | All inputs are validated, and errors use a consistent error schema. |

### 6.2 Search

| ID | Requirement |
|----|-------------|
| FR-7 | Software and version metadata are indexed in Elasticsearch (or OpenSearch). |
| FR-8 | The index is updated when catalog data changes, via an event or a scheduled indexer. |
| FR-9 | `GET /search?q=` supports fuzzy matching, field boosting (name over description), and facets by OS and category. |
| FR-10 | The indexer can perform a full reindex without downtime (alias swap). |

### 6.3 Feedback Service

| ID | Requirement |
|----|-------------|
| FR-11 | `POST /feedback` accepts software ID, version, rating (1–5), and an optional comment. |
| FR-12 | Feedback is stored in the database and a `feedback.submitted` event is published to Kafka. |
| FR-13 | Publishing uses the **transactional outbox pattern**, so database state and Kafka events stay consistent. |
| FR-14 | A user can submit at most one feedback entry per version *(enforced by a unique constraint)*. |
| FR-15 | The catalog service also publishes `download.completed` events to Kafka. |

### 6.4 Analytics Service

| ID | Requirement |
|----|-------------|
| FR-16 | Consumes `feedback.submitted` and `download.completed` topics. |
| FR-17 | Aggregates average rating per version, downloads per day, and feedback counts. |
| FR-18 | Processing is idempotent; messages that fail repeatedly go to a dead-letter topic. |
| FR-19 | `GET /reports/{softwareId}` returns aggregated metrics; an optional scheduled job exports a CSV or PDF report. |

### 6.5 API Gateway (Kong)

| ID | Requirement |
|----|-------------|
| FR-20 | Kong is the only public entry point and routes to `/api/catalog`, `/api/feedback`, `/api/reports`, and `/api/search`. |
| FR-21 | Authentication through the JWT or key-auth plugin. |
| FR-22 | Rate limiting per consumer, CORS, and request-ID injection (correlation ID). |
| FR-23 | Configuration is declarative (DB-less mode) and kept in version control. |

---

## 7. Non-Functional Requirements

| Area | Requirement |
|------|-------------|
| **Performance** | Search p95 latency under 300 ms at 50 req/s in local load tests |
| **Reliability** | No lost feedback events; at-least-once delivery with idempotent consumers |
| **Scalability** | Stateless services; HPA scales catalog and feedback services on CPU |
| **Security** | JWT validation at the gateway, secrets in Kubernetes Secrets, no credentials in images or Git |
| **Testability** | ≥ 80% line coverage on service logic; integration tests with Testcontainers |
| **Observability** | Metrics, structured logs with correlation IDs, and dashboards for every service |
| **Portability** | One-command local startup with Docker Compose; Kubernetes deployment on minikube or kind |

---

## 8. System Architecture

**Components:** React client *(optional)* · Kong Gateway · Catalog Service · Feedback Service · Analytics Service · MySQL · Elasticsearch · Kafka · Prometheus · Grafana · Loki

### Request Flow — Feedback

```
Client
  │
  ▼
Kong Gateway          ← validates token, rate-limits, injects correlation ID
  │
  ▼
Feedback Service      ← writes feedback + outbox record in one transaction
  │
  ▼
Outbox Relay          ← publishes event to Kafka
  │
  ▼
Analytics Service     ← consumes event, updates aggregates
  │
  ▼
GET /api/reports/{id} ← publishers view updated metrics
```

---

## 9. Data Model (Summary)

```sql
software        (id, name, description, category, tags, publisher_id, created_at)
version         (id, software_id, version_number, os, release_notes, file_ref, released_at)
download_event  (id, version_id, user_id, timestamp)
feedback        (id, version_id, user_id, rating, comment, created_at)  -- unique(user_id, version_id)
outbox          (id, aggregate_id, event_type, payload, created_at, published_at)
report_aggregate(software_id, version_id, date, downloads, avg_rating, feedback_count)
```

---

## 10. Event Contracts

| Topic | Key | Payload |
|-------|-----|---------|
| `feedback.submitted` | `version_id` | `feedbackId`, `softwareId`, `versionId`, `rating`, `comment`, `timestamp` |
| `download.completed` | `version_id` | `downloadId`, `softwareId`, `versionId`, `userId`, `timestamp` |
| `*.dlq` | original key | original payload, `errorReason`, `retryCount` |

---

## 11. Observability and Operations

| Concern | Details |
|---------|---------|
| **Metrics** | Spring Boot Actuator + Micrometer, scraped by Prometheus |
| **Dashboards** | Request rate, error rate, p95 latency, JVM health, Kafka consumer lag, downloads/hour, ratings distribution |
| **Logging** | JSON structured logs carrying the correlation ID, shipped to Loki |
| **Alerts** | Error rate > 5% for 5 min · consumer lag above threshold · pod restarts |
| **Health** | Readiness and liveness probes on every service |

---

## 12. Testing Strategy

| Layer | Tooling |
|-------|---------|
| **Unit tests** | JUnit 5 + Mockito for services, validators, and mappers |
| **Integration tests** | Testcontainers for MySQL, Kafka, and Elasticsearch |
| **Contract checks** | Verify event schemas between producer and consumer |
| **Coverage gate** | JaCoCo in CI — build fails below 80% |
| **Load test** | k6 script for search and download endpoints, used to demonstrate autoscaling |

---

## 13. Deployment

- **Dockerfile:** Multi-stage build per service, non-root user.
- **Local:** `docker-compose.yml` for full-stack development.
- **Kubernetes:** Manifests or Helm chart — Deployments, Services, ConfigMaps, Secrets, Ingress (Kong Ingress Controller), HPA.
- **CI/CD:** GitHub Actions pipeline — build → test → coverage gate → image build → push.

---

## 14. Milestones

| Week | Milestone | Exit Criteria |
|------|-----------|---------------|
| 1 | **Catalog API** | CRUD, pagination, validation, and unit tests passing |
| 2 | **Search** | Indexer working, fuzzy search and facets, reindex tested |
| 3 | **Feedback & Kafka** | Outbox pattern, events published, analytics consumer with DLQ |
| 4 | **Gateway & Deployment** | Kong routing and auth, Docker images, running on Kubernetes |
| 5 | **Observability & Polish** | Dashboards, alerts, CI pipeline, README, and architecture diagram |

---

## 15. Success Metrics

- **Search relevance:** Expected result appears in top 3 for >= 90% of a hand-built test query set.
- **Zero lost feedback events** in a failure test (Kafka paused, then resumed).
- **Test coverage** at or above 80%.
- **A fresh clone** starts the full stack with a single command.
- **Dashboards** show live traffic during the load test, and the autoscaler scales up under load.

---

## 16. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Too much infrastructure for local machines | Slow development | Use docker-compose profiles; run only services needed for each phase |
| Duplicate Kafka messages | Wrong analytics | Idempotent consumer keyed on event ID |
| Search index drifts from the database | Stale results | Alias-based reindex and a periodic reconciliation job |
| Scope creep | Project never finishes | Treat frontend and PDF reports as stretch goals |
| Coveo is a paid tool | Cannot replicate exactly | Use Elasticsearch and note Coveo-equivalent concepts in the README |

---

## 17. Stretch Goals

- [ ] Minimal React frontend with search, download, and star rating.
- [ ] Notifications when a new version of a followed product is released.
- [ ] Download heatmaps and trend charts in Grafana.
- [ ] Search-term analytics feeding back into relevance tuning.

---

## 18. Open Questions

- [ ] Elasticsearch, OpenSearch, or Meilisearch for the search engine?
- [ ] JWT issued by a simple auth service, or static API keys behind Kong?
- [ ] Store binaries in MinIO, or keep metadata and external links only?
