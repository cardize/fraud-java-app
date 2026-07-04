# Fraud

![CI](https://github.com/cardize/fraud-java-app/actions/workflows/ci.yml/badge.svg)

A multi-module **fraud detection platform** written in Java 21 + Spring Boot.
It evaluates incoming transactions with a rule/scenario engine and statistical anomaly detection;
the decision is returned synchronously, while heavy work runs asynchronously through a durable outbox.

## Architecture

Clean Architecture + Hexagonal (Ports & Adapters) + CQRS. Each layer is a separate **Maven module**.
The dependency direction always points **inward**: `api → infrastructure → application → domain`.
Maven rejects an import in the wrong direction at compile time — the architecture rules are
enforced by the build.

```
fraud-parent (pom)
├─ fraud-domain          → Entities, aggregates, enums (core business model)
│      dependencies: none (only jakarta.persistence-api)  ·  knows NOTHING about Spring
├─ fraud-application     → CQRS (Command/Handler/Mediator), FraudParameters,
│                             ScenarioService, anomaly, PORT interfaces
│      dependencies: domain + spring-context/tx
├─ fraud-infrastructure  → JPA adapters, RuleEvaluator (SpEL), scenario processors,
│                             outbox, cache, multi-tenancy, message publishers
│      dependencies: application + spring-boot-starter-data-jpa
├─ fraud-api             → Controllers + security + bootstrap (runnable jar)
│      dependencies: infrastructure + web + security + actuator
└─ fraud-gateway         → API Gateway / reverse proxy (8090, separate application)
       dependencies: spring-cloud-starter-gateway (reactive)
```

### Ports & Adapters
The application layer uses infrastructure through **interfaces (ports)**; the actual
implementation (adapter) lives in infrastructure:

| Port (application) | Adapter (infrastructure) |
|---|---|
| `TransactionStore` | `JpaTransactionStore` / `JdbcTransactionStore` |
| `ScenarioProcessor` | `Card/Pf/PayCell/TrKart ScenarioProcessor` |
| `OfflineOperationPublisher` | `OutboxOfflineOperationPublisher` |
| `AnomalyDetector` | `StatisticalAnomalyDetector` / `NoOpAnomalyDetector` |
| `ScenarioAdminStore` | `JpaScenarioAdminStore` (evicts the scenario cache on every mutation) |
| `ExpressionValidator` | `SpelExpressionValidator` (write-time safe-SpEL validation) |
| `AuditTrail` | `PersistentAuditTrail` (in the API module — resolves the actor from the security context) |

## Tech Stack
- **Java 21**, **Spring Boot 3.3**, **Maven** (multi-module)
- **Spring Web** (REST), **Spring Security + JWT** (jjwt; RBAC via a `roles` claim, BCrypt user store), **springdoc-openapi** (Swagger UI)
- **Bucket4j** (login rate limiting), HSTS/clickjacking/referrer-policy headers
- **Spring Data JPA / Hibernate**, **JdbcTemplate** (hot path)
- **SpEL** (rule evaluation), **Spring Cache** (Redis optional)
- **Flyway** (default) / **Liquibase** (alternative) — schema migrations
- **Kafka / RabbitMQ** (outbox publish target), **Spring Cloud Gateway**
- **H2** (development), **PostgreSQL** (production/testcontainers)
- **JUnit 5 + MockMvc + Testcontainers + Awaitility** (tests)

## End-to-End Flow

```
POST /api/v1/transactions/get-fraud-response-for-card
  → TransactionController
  → Mediator.send(command)
  → GetFraudResponseForCardHandler  (@Transactional)
      1) Persist the transaction + duplicate check
      2) Build FraudParameters
      3) ScenarioService → XxxScenarioProcessor
            → ScenarioCatalog loads scenarios from the DB (cached)
            → BaseScenarioProcessor evaluates in parallel (SpEL rules)
      4) fraudResponseCode (APPROVE / REJECT / REVIEW) is returned to the client
      5) offlinePublisher.publish(...) → OutboxMessage (atomic with the business record)
         → OutboxRelay (@Scheduled) sends via the selected publisher
```

## Build & Run (Java 21 + Maven)

```bash
# build all modules
mvn clean install

# 1) Start the API (8080)
mvn -pl fraud-api -am spring-boot:run

# 2) (optional) Start the gateway (8090 → 8080 proxy)
mvn -pl fraud-gateway -am spring-boot:run
```

### Run with Docker (real Postgres + Kafka)

```bash
docker compose up --build
```

Starts four containers: **PostgreSQL 16** (Flyway migrates on boot), **Kafka** (single-node
KRaft), the **API** (8080, management 9090 — outbox publishes to real Kafka) and the
**gateway** (8090, management 9091, routed to the api container). One `Dockerfile` builds both
bootable modules via `--build-arg MODULE=fraud-api|fraud-gateway` (multi-stage; non-root runtime
user; pom-only layer for dependency caching).

### Example request (JWT-protected)

```bash
# 1) Get a token. Seeded users (dev defaults): admin/fraud123 (roles ADMIN,USER),
#    analyst/analyst123 (USER). Override via FRAUD_ADMIN_PASSWORD / FRAUD_ANALYST_PASSWORD.
#    Login returns a short-lived access token + a rotating refresh token.
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"fraud123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

# Refresh (rotation): exchanges the refresh token for a NEW access+refresh pair. Each refresh
# token is one-shot; replaying a consumed one revokes the user's whole token family (theft response).
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" -d '{"refreshToken":"<refreshToken from login>"}'

# 2) Fraud check — a high amount (>5000) triggers the REJECT scenario
curl -X POST http://localhost:8080/api/v1/transactions/get-fraud-response-for-card \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"module":1,"transactionMessageId":1001,"shadowCardNo":"CARD123",
       "amount":6000,"merchantId":"MERCH1","transactionDate":"2026-01-01T03:00:00Z"}'

# Anomaly check
curl -X POST http://localhost:8080/api/v1/ai/check-transaction \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"transactionId":"11111111-1111-1111-1111-111111111111","shadowCardNo":"CARD123",
       "amount":50000,"merchantId":"M1","transactionDate":"2026-01-01T03:00:00Z"}'

# Scenario management (GET: any authenticated user; POST/PUT/DELETE: ADMIN role).
# Expressions are validated at write time (safe SpEL only) and the scenario cache is evicted on
# every mutation — the change affects the very next transaction.
# Listing is paged (?page, ?size — clamped to 100) with optional filters (?productType, ?module).
curl "http://localhost:8080/api/v1/scenarios?productType=CARD&module=1&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:8080/api/v1/scenarios \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Blocked Merchant","productType":"CARD","module":1,"priority":0,
       "fraudResponseCode":"REVIEW","rules":[{"name":"blocked","expression":"merchantId == '\''BADSHOP'\''"}]}'
curl -X PUT http://localhost:8080/api/v1/scenarios/4 \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Blocked Merchant v2","productType":"CARD","module":1,"priority":0,
       "fraudResponseCode":"REJECT","rules":[{"name":"blocked","expression":"merchantId == '\''BADSHOP'\''"}]}'
curl -X DELETE http://localhost:8080/api/v1/scenarios/4 -H "Authorization: Bearer $TOKEN"

# Audit trail (ADMIN): the last 100 security-relevant actions (logins, scenario mutations,
# token refreshes) with actor + correlation id
curl http://localhost:8080/api/v1/audit -H "Authorization: Bearer $TOKEN"

# Clear the scenario cache (requires the ADMIN role — the analyst user gets 403)
curl -X POST http://localhost:8080/api/v1/cache/evict-scenarios -H "Authorization: Bearer $TOKEN"

# Logout (blacklists the token; it is rejected from then on)
curl -X POST http://localhost:8080/api/v1/auth/logout -H "Authorization: Bearer $TOKEN"
```

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **H2 console:** http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:fraud`)
- **Health/Prometheus:** http://localhost:9090/actuator/health · http://localhost:9090/actuator/prometheus
  (separate management port — needs no JWT; in production it is closed off purely at the
  network/firewall level. The gateway's own actuator is on 9091.)

## Tests & CI
```bash
mvn test                                   # unit + MockMvc integration tests
mvn -Dtest=ContainersFraudFlowTest test    # Postgres+Kafka (requires Docker)
```

GitHub Actions (`.github/workflows/ci.yml`) runs `mvn verify` (including the Testcontainers
suite — Docker is available on hosted runners) on every push/PR to `master`, and validates both
Docker images on push.

## Configuration keys (application.yml)
| Key | Values | Effect |
|---|---|---|
| `fraud.persistence.transaction-store` | `jpa` (default) / `jdbc` | Transaction writes: ORM or raw SQL |
| `fraud.ai.enabled` | `true` (default) / `false` | Anomaly detection on/off |
| `fraud.outbox.publisher` | `logging` (default) / `kafka` / `rabbit` | Outbox publish target |
| `fraud.outbox.retention-days` | int (default 7) | Retention of PROCESSED outbox records |
| `FRAUD_ALLOWED_ORIGINS` (gateway) | comma-separated origins | CORS allowed origins (default: localhost only) |
| `FRAUD_MANAGEMENT_PORT` (default 9090) / `FRAUD_GATEWAY_MANAGEMENT_PORT` (default 9091) | port | Actuator's separate, JWT-free management port |
| `server.compression.enabled` | `true` (default) | Gzip for JSON/XML/HTML responses (>1KB) |
| `spring.cache.type` | `caffeine` (default) / `redis` | Cache provider (bounded size + TTL) |
| `fraud.scenario.parallel` / `max-parallelism` | bool / int | Parallel scenario execution |
| `fraud.security.jwt-secret` | string | JWT signing key (env: `FRAUD_JWT_SECRET`) |
| `FRAUD_ADMIN_PASSWORD` / `FRAUD_ANALYST_PASSWORD` | string | Seeded users' passwords (dev defaults: `fraud123` / `analyst123`) |
| `fraud.security.refresh-expiration-ms` | ms (default 7 days) | Rotating refresh token lifetime (env: `FRAUD_REFRESH_EXPIRATION_MS`) |
| `fraud.security.login-rate-limit.capacity` / `window-seconds` | int (default 5/60) | Login brute-force limit (per IP) |
| `fraud.retention.transactions-days` / `message-claims-days` | int (default 90/30) | Retention for transactions / dedup claims (daily `DataRetentionJob`) |
| profile `multitenant` | on/off | Per-tenant DB + per-tenant Flyway (`X-Tenant` header) |
| profile `liquibase` | on/off | Migration tool: Liquibase instead of Flyway (default) |
| profile `redis` | on/off | Switches the cache provider to Redis |

> Notes:
> - **RBAC:** the JWT carries a `roles` claim (from the `users` table); `/api/v1/cache/**` requires `ROLE_ADMIN`.
> - **Correlation id:** every response carries `X-Correlation-Id` (generated when absent) and every log line prints it (`[cid:...]`) — one request's logs can be traced end-to-end.
> - **Business metrics:** `fraud.decisions{code}`, `fraud.auth.login{result}`, `fraud.scenario.duration{productType}`, `fraud.outbox.pending`, `fraud.outbox.published{result}` — all on `/actuator/prometheus` (port 9090).
> - With H2, `flyway-core` is enough; for Postgres, `flyway-database-postgresql` is added (test scope here).
> - Kafka/Rabbit only connect to a broker when `publisher` is set to that value; the default `logging` works offline.
> - Multi-tenant run: `mvn -pl fraud-api -am spring-boot:run -Dspring-boot.run.profiles=multitenant`,
>   with `-H "X-Tenant: alpha"` on requests. At startup, `MultiTenantSeeder` onboards EVERY tenant
>   (scenarios + users), so logins work against alpha/beta too.
> - **Audit trail:** logins (success/failure), token refreshes, logout and scenario mutations are
>   written append-only to `audit_log` with actor + correlation id; read via `GET /api/v1/audit` (ADMIN).

## Module & Package Map
```
com.fraud
├─ api/                 → Controllers, security/ (JWT+refresh), audit/, tenant/, config/ (OpenAPI, MultiTenantSeeder), exception handler
├─ application/
│   ├─ cqrs/            → Command, CommandHandler, Mediator
│   ├─ transactions/    → command + handler + TransactionStore port + dto
│   ├─ scenarios/       → scenario CRUD commands/handlers + ScenarioAdminStore & ExpressionValidator ports
│   ├─ fraud/           → FraudParameters, ScenarioService, ScenarioProcessor port
│   ├─ anomaly/         → AnomalyDetector port + command/handler/dto
│   ├─ queue/           → OfflineOperation + publisher port
│   ├─ tenant/          → TenantProvider port
│   └─ common/          → ApiResult
├─ domain/              → rule/ (Rule, Scenario, RuleType), transaction/, shared/
└─ infrastructure/
    ├─ persistence/     → JPA repos + adapters, entity rows, seeder, message claims, user accounts
    ├─ maintenance/     → DataRetentionJob (transactions + message-claims retention)
    ├─ rules/           → RuleEvaluator (SpEL), SpelExpressionValidator, Base/Card/... ScenarioProcessor, ScenarioCatalog
    ├─ outbox/          → OutboxMessage, OutboxRelay, publisher/ (logging/kafka/rabbit)
    ├─ tenant/          → TenantContext, RoutingDataSource, multi-tenant config + migrator
    ├─ anomaly/         → StatisticalAnomalyDetector, CardStatistics(Store)
    └─ config/          → CacheConfig, ScenarioExecutorConfig
```
