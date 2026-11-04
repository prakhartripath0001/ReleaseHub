ReleaseHub: Product Requirements Document
Version: 1.0 (Draft)
Type: Portfolio / learning project
Stack: Java, Spring Boot, PostgreSQL, Elasticsearch, Kafka, Kong, Docker, Kubernetes, Prometheus, Grafana
1. Overview
ReleaseHub is a software distribution and feedback platform. Publishers upload software releases, users search for and download them, and users rate and review what they download. Feedback and download events flow through Kafka into an analytics service that produces reports for publishers.
The project mirrors a real-world software downloads page: a catalog API, indexed search, an external API exposed through a gateway, a feedback pipeline, unit testing, observability, and containerized deployment.
2. Problem Statement
Users struggle to find the right software version and to know whether it is reliable. Publishers have no structured view of how their releases are received. Existing download pages often combine weak search, no feedback loop, and no usable analytics.
3. Goals and Non-Goals
Goals
• Provide a fast, relevant search over software and versions.
• Let users download releases and submit ratings and comments.
• Give publishers aggregated analytics (downloads, ratings, trends).
• Expose all public APIs through a single secured gateway.
• Be observable, tested, and deployable with one command.
Non-Goals
• Payments or licensing.
• Full user account management (a simple JWT or API-key scheme is enough).
• Hosting very large binaries (use metadata plus links, or MinIO for small files).
• A polished production UI (a minimal React frontend is a stretch goal).
4. Target Users and Personas
Persona
Description
Key needs
End User
Developer or IT user looking for software
Find the right version quickly, download it, see what others think
Publisher
Team that owns software releases
Upload releases, see download and rating trends
Admin
Platform operator
Monitor health, manage content, review logs and alerts
5. User Stories
1. As an end user, I can search by name, tag, or keyword and tolerate typos, so I find software even with imperfect queries.
2. As an end user, I can filter results by operating system, category, and version.
3. As an end user, I can download a specific version of a product.
4. As an end user, I can submit a star rating and a comment for a version.
5. As a publisher, I can create a software entry and publish new versions.
6. As a publisher, I can view reports showing downloads per day, average rating per version, and top search terms.
7. As an admin, I can see request rates, error rates, latency, and Kafka consumer lag in dashboards.
8. As an admin, I receive an alert when the error rate exceeds the threshold.
6. Functional Requirements
6.1 Catalog Service
• FR-1: CRUD for Software (name, description, category, tags, publisher) and Version (version number, OS, release notes, file reference, release date).
• FR-2: GET /software supports pagination, sorting, and filtering by OS, category, and tag.
• FR-3: GET /software/{id}/versions lists versions, newest first.
• FR-4: GET /download/{versionId} records a download event and returns a download URL or redirect.
• FR-5: Write endpoints are restricted to publishers and admins.
• FR-6: All inputs are validated, and errors use a consistent error schema.
6.2 Search
• FR-7: Software and version metadata are indexed in Elasticsearch (or OpenSearch).
• FR-8: The index is updated when catalog data changes, via an event or a scheduled indexer.
• FR-9: GET /search?q= supports fuzzy matching, field boosting (name over description), and facets by OS and category.
• FR-10: The indexer can perform a full reindex without downtime (alias swap).
6.3 Feedback Service
• FR-11: POST /feedback accepts software ID, version, rating (1 to 5), and an optional comment.
• FR-12: Feedback is stored in the database and a feedback.submitted event is published to Kafka.
• FR-13: Publishing uses the transactional outbox pattern, so database state and Kafka events stay consistent.
• FR-14: A user can submit at most one feedback entry per version (enforced by a unique constraint).
• FR-15: The catalog service also publishes download.completed events to Kafka.
6.4 Analytics Service
• FR-16: Consumes feedback.submitted and download.completed topics.
• FR-17: Aggregates average rating per version, downloads per day, and feedback counts.
• FR-18: Processing is idempotent, and messages that fail repeatedly go to a dead-letter topic.
• FR-19: GET /reports/{softwareId} returns aggregated metrics, and an optional scheduled job exports a CSV or PDF report.
6.5 API Gateway (Kong)
• FR-20: Kong is the only public entry point and routes to /api/catalog, /api/feedback, /api/reports, and /api/search.
• FR-21: Authentication through the JWT or key-auth plugin.
• FR-22: Rate limiting per consumer, CORS, and request-ID injection (correlation ID).
• FR-23: Configuration is declarative (DB-less mode) and kept in version control.
7. Non-Functional Requirements
Area
Requirement
Performance
Search p95 latency under 300 ms at 50 requests per second in local load tests
Reliability
No lost feedback events; at-least-once delivery with idempotent consumers
Scalability
Stateless services; the Horizontal Pod Autoscaler scales catalog and feedback services on CPU
Security
JWT validation at the gateway, secrets in Kubernetes Secrets, no credentials in images or Git
Testability
At least 80% line coverage on service logic; integration tests with Testcontainers
Observability
Metrics, structured logs with correlation IDs, and dashboards for every service
Portability
One-command local startup with Docker Compose; Kubernetes deployment on minikube or kind
8. System Architecture
Components: React client (optional), Kong Gateway, Catalog Service, Feedback Service, Analytics Service, PostgreSQL, Elasticsearch, Kafka, Prometheus, Grafana, and Loki.
Request flow (feedback):
1. Client calls POST /api/feedback through Kong.
2. Kong validates the token, applies rate limits, and adds a correlation ID.
3. The Feedback Service writes the feedback and an outbox record in one transaction.
4. An outbox relay publishes the event to Kafka.
5. The Analytics Service consumes the event and updates the aggregates.
6. Publishers see the updated metrics through GET /api/reports/{id}.
9. Data Model (Summary)
• software: id, name, description, category, tags, publisher_id, created_at
• version: id, software_id, version_number, os, release_notes, file_ref, released_at
• download_event: id, version_id, user_id, timestamp
• feedback: id, version_id, user_id, rating, comment, created_at (unique on user and version)
• outbox: id, aggregate_id, event_type, payload, created_at, published_at
• report_aggregate: software_id, version_id, date, downloads, avg_rating, feedback_count
10. Event Contracts
Topic
Key
Payload
feedback.submitted
version_id
feedbackId, softwareId, versionId, rating, comment, timestamp
download.completed
version_id
downloadId, softwareId, versionId, userId, timestamp
*.dlq
original key
original payload, error reason, retry count
11. Observability and Operations
• Metrics: Spring Boot Actuator with Micrometer, scraped by Prometheus.
• Dashboards: request rate, error rate, p95 latency, JVM health, Kafka consumer lag, downloads per hour, and ratings distribution.
• Logging: JSON structured logs carrying the correlation ID, shipped to Loki.
• Alerts: error rate above 5% for 5 minutes, consumer lag above threshold, and pod restarts.
• Health: readiness and liveness probes on every service.
12. Testing Strategy
• Unit tests: JUnit 5 and Mockito for services, validators, and mappers.
• Integration tests: Testcontainers for PostgreSQL, Kafka, and Elasticsearch.
• Contract checks: verify event schemas between producer and consumer.
• Coverage gate: JaCoCo in CI, failing the build below 80%.
• Load test: k6 script for search and download endpoints, used to demonstrate autoscaling.
13. Deployment
• Multi-stage Dockerfile per service, with a non-root user.
• docker-compose.yml for local development.
• Kubernetes manifests or a Helm chart: Deployments, Services, ConfigMaps, Secrets, Ingress (Kong Ingress Controller), and an HPA.
• GitHub Actions pipeline: build, test, coverage gate, image build, and push.
14. Milestones
Week
Milestone
Exit criteria
1
Catalog API
CRUD, pagination, validation, and unit tests passing
2
Search
Indexer working, fuzzy search and facets, reindex tested
3
Feedback and Kafka
Outbox pattern, events published, analytics consumer with DLQ
4
Gateway and Deployment
Kong routing and auth, Docker images, running on Kubernetes
5
Observability and Polish
Dashboards, alerts, CI pipeline, README, and architecture diagram
15. Success Metrics
• Search relevance: the expected result appears in the top 3 for at least 90% of a hand-built test query set.
• Zero lost feedback events in a failure test (Kafka paused, then resumed).
• Test coverage at or above 80%.
• A fresh clone starts the full stack with a single command.
• Dashboards show live traffic during the load test, and the autoscaler scales up under load.
16. Risks and Mitigations
Risk
Impact
Mitigation
Too much infrastructure for local machines
Slow development
Use docker-compose profiles, and run only the services needed for each phase
Duplicate Kafka messages
Wrong analytics
Idempotent consumer keyed on event ID
Search index drifts from the database
Stale results
Alias-based reindex and a periodic reconciliation job
Scope creep
Project never finishes
Treat the frontend and PDF reports as stretch goals
Coveo is a paid tool
Cannot replicate exactly
Use Elasticsearch, and note the Coveo-equivalent concepts in the README
17. Stretch Goals
• Minimal React frontend with search, download, and star rating.
• Notifications when a new version of a followed product is released.
• Download heatmaps and trend charts in Grafana.
• Search-term analytics feeding back into relevance tuning.
18. Open Questions
• Elasticsearch, OpenSearch, or Meilisearch for the search engine?
• JWT issued by a simple auth service, or static API keys behind Kong?
• Store binaries in MinIO, or keep metadata and external links only?
