# PayGuard

Java 21 + Spring Boot ile yazılmış, çok modüllü bir **ödeme sahtekarlığı (fraud) tespit platformu**.
Gelen işlemleri kural/senaryo motoru ve istatistiksel anomali tespitiyle değerlendirir; kararı senkron
döner, ağır işleri kalıcı bir outbox üzerinden asenkron yürütür.

## Mimari

Clean Architecture + Hexagonal (Ports & Adapters) + CQRS. Her katman ayrı bir **Maven modülü**.
Bağımlılık yönü **daima içe doğru**: `api → infrastructure → application → domain`.
Maven, ters yöndeki bir import'u derleme zamanında engeller — mimari kurallar build ile zorlanır.

```
payguard-parent (pom)
├─ payguard-domain          → Entity, aggregate, enum'lar (çekirdek iş modeli)
│      bağımlılık: yok (sadece jakarta.persistence-api)  ·  Spring BİLMEZ
├─ payguard-application     → CQRS (Command/Handler/Mediator), FraudParameters,
│                             ScenarioService, anomali, PORT arayüzleri
│      bağımlılık: domain + spring-context/tx
├─ payguard-infrastructure  → JPA adapter, RuleEvaluator (SpEL), senaryo işlemcileri,
│                             outbox, cache, multi-tenant, mesaj yayımcıları
│      bağımlılık: application + spring-boot-starter-data-jpa
├─ payguard-api             → Controller + güvenlik + bootstrap (çalıştırılabilir jar)
│      bağımlılık: infrastructure + web + security + actuator
└─ payguard-gateway         → API Gateway / reverse proxy (8090, ayrı uygulama)
       bağımlılık: spring-cloud-starter-gateway (reaktif)
```

### Ports & Adapters
Application katmanı altyapıyı **arayüz (port)** üzerinden kullanır; gerçek implementasyon (adapter)
infrastructure'dadır:

| Port (application) | Adapter (infrastructure) |
|---|---|
| `TransactionStore` | `JpaTransactionStore` / `JdbcTransactionStore` |
| `ScenarioProcessor` | `Card/Pf/PayCell/TrKart ScenarioProcessor` |
| `OfflineOperationPublisher` | `OutboxOfflineOperationPublisher` |
| `AnomalyDetector` | `StatisticalAnomalyDetector` / `NoOpAnomalyDetector` |

## Teknoloji Yığını
- **Java 21**, **Spring Boot 3.3**, **Maven** (çok modül)
- **Spring Web** (REST), **Spring Security + JWT** (jjwt), **springdoc-openapi** (Swagger UI)
- **Spring Data JPA / Hibernate**, **JdbcTemplate** (hot-path)
- **SpEL** (kural değerlendirme), **Spring Cache** (Redis opsiyonel)
- **Flyway** (varsayılan) / **Liquibase** (alternatif) — şema migration
- **Kafka / RabbitMQ** (outbox yayım hedefi), **Spring Cloud Gateway**
- **H2** (geliştirme), **PostgreSQL** (üretim/testcontainers)
- **JUnit 5 + MockMvc + Testcontainers + Awaitility** (testler)

## Uçtan Uca Akış

```
POST /api/v1/transactions/get-fraud-response-for-card
  → TransactionController
  → Mediator.send(command)
  → GetFraudResponseForCardHandler  (@Transactional)
      1) Transaction kaydet + duplicate kontrol
      2) FraudParameters üret
      3) ScenarioService → XxxScenarioProcessor
            → ScenarioCatalog DB'den senaryoları yükler (cache'li)
            → BaseScenarioProcessor paralel değerlendirir (SpEL kural)
      4) fraudResponseCode (APPROVE / REJECT / REVIEW) istemciye döner
      5) offlinePublisher.publish(...) → OutboxMessage (iş kaydıyla atomik)
         → OutboxRelay (@Scheduled) seçili yayımcıya gönderir
```

## Derleme & Çalıştırma (Java 21 + Maven)

```bash
# tüm modülleri derle
mvn clean install

# 1) API'yi ayağa kaldır (8080)
mvn -pl payguard-api -am spring-boot:run

# 2) (opsiyonel) Gateway'i ayağa kaldır (8090 → 8080 proxy)
mvn -pl payguard-gateway -am spring-boot:run
```

### Örnek istek (JWT korumalı)

```bash
# 1) Token al (demo şifre: payguard123)
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"payguard123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

# 2) Fraud kontrolü — yüksek tutar (>5000) REJECT senaryosunu tetikler
curl -X POST http://localhost:8080/api/v1/transactions/get-fraud-response-for-card \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"module":1,"transactionMessageId":1001,"shadowCardNo":"CARD123",
       "amount":6000,"merchantId":"MERCH1","transactionDate":"2026-01-01T03:00:00Z"}'

# Anomali kontrolü
curl -X POST http://localhost:8080/api/v1/ai/check-transaction \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"transactionId":"11111111-1111-1111-1111-111111111111","shadowCardNo":"CARD123",
       "amount":50000,"merchantId":"M1","transactionDate":"2026-01-01T03:00:00Z"}'

# Senaryo cache temizleme
curl -X POST http://localhost:8080/api/v1/cache/evict-scenarios -H "Authorization: Bearer $TOKEN"
```

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **H2 console:** http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:payguard`)
- **Health:** http://localhost:8080/actuator/health

## Testler
```bash
mvn test                                   # birim + MockMvc entegrasyon testleri
mvn -Dtest=ContainersFraudFlowTest test    # Postgres+Kafka (Docker gerekir)
```

## Yapılandırma anahtarları (application.yml)
| Anahtar | Değerler | Etki |
|---|---|---|
| `payguard.persistence.transaction-store` | `jpa` (vars.) / `jdbc` | İşlem yazımı: ORM mi ham SQL mi |
| `payguard.ai.enabled` | `true` (vars.) / `false` | Anomali tespiti açık/kapalı |
| `payguard.outbox.publisher` | `logging` (vars.) / `kafka` / `rabbit` | Outbox yayım hedefi |
| `spring.cache.type` | `simple` (vars.) / `redis` | Cache sağlayıcı |
| `payguard.scenario.parallel` / `max-parallelism` | bool / int | Senaryo paralel yürütme |
| `payguard.security.jwt-secret` / `demo-password` | string | JWT anahtarı / login demo şifresi |
| profil `multitenant` | aktif/değil | Tenant başına ayrı DB + per-tenant Flyway (`X-Tenant` header) |
| profil `liquibase` | aktif/değil | Migration aracı: Flyway (vars.) yerine Liquibase |
| profil `redis` | aktif/değil | Cache sağlayıcıyı Redis'e geçirir |

> Notlar:
> - Flyway H2 ile `flyway-core` yeterlidir; Postgres için `flyway-database-postgresql` (test kapsamında) eklenir.
> - Kafka/Rabbit yalnızca `publisher` o değere ayarlanınca broker'a bağlanır; varsayılan `logging` offline çalışır.
> - Çok-kiracı çalıştırma: `mvn -pl payguard-api -am spring-boot:run -Dspring-boot.run.profiles=multitenant`,
>   istekte `-H "X-Tenant: alpha"`.

## Modül & Paket Haritası
```
com.payguard
├─ api/                 → Controller, ApiResult, security/, tenant/, config/(OpenAPI)
├─ application/
│   ├─ cqrs/            → Command, CommandHandler, Mediator
│   ├─ transactions/    → komut + handler + TransactionStore portu + dto
│   ├─ fraud/           → FraudParameters, ScenarioService, ScenarioProcessor portu
│   ├─ anomaly/         → AnomalyDetector portu + komut/handler/dto
│   └─ queue/           → OfflineOperation + publisher portu
├─ domain/              → rule/ (Rule, Scenario, RuleType), transaction/, shared/
└─ infrastructure/
    ├─ persistence/     → JPA repo + adapter'lar, entity satırları, seeder
    ├─ rules/           → RuleEvaluator (SpEL), Base/Card/... ScenarioProcessor, ScenarioCatalog
    ├─ outbox/          → OutboxMessage, OutboxRelay, publisher/ (logging/kafka/rabbit)
    ├─ tenant/          → TenantContext, RoutingDataSource, multi-tenant config + migrator
    ├─ anomaly/         → StatisticalAnomalyDetector, CardStatistics(Store)
    └─ config/          → CacheConfig
```
