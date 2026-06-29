# PayGuard — Java / Spring Boot Portu

.NET 8 fraud tespit platformunun Java karşılığı. **Dikey dilim** yaklaşımı: önce tek bir
uçtan uca akış (`get-fraud-response-for-card`) tam çalışır halde; mimari iskelet oturduktan
sonra diğer modüller eklenecek.

> Bu makinede Java/Maven kurulu değil ve internet yok. Kod yazıldı; **build için Java 21 + Maven**
> kurulu (ve Maven Central'a erişimi olan) bir ortamda çalıştır.

## Teknoloji Eşlemesi (.NET → Java)

| .NET (PayGuard) | Java (bu port) | Not |
|---|---|---|
| ASP.NET Core | **Spring Boot 3.3 / Spring Web** | Controller, DI, pipeline |
| Autofac (DI) | **Spring DI** | constructor injection |
| MediatR (`IRequest`/`IRequestHandler`) | **`cqrs/` paketi** (Command, CommandHandler, Mediator) | elle yazılmış hafif aracı |
| `IDataResult<T>` (Core.Utilities) | **`ApiResult<T>`** | sonuç zarfı |
| Entity Framework Core | **Spring Data JPA / Hibernate** | `@Entity`, repository |
| EF `IEntityTypeConfiguration` | **JPA anotasyonları** | `@Table`, `@Column`... |
| Dapper (sıcak yol) | **(ileride) Spring JdbcTemplate / jOOQ** | şimdilik JPA kullanıldı |
| Microsoft RulesEngine | **SpEL** (`RuleEvaluator`) | üretimde Easy Rules / Drools alternatifi |
| `Parallel.ForEachAsync` + `SemaphoreSlim` | **`ExecutorService` + `Future`** | `CardScenarioProcessor` |
| `System.Threading.Channels` + `BackgroundService` | **`BlockingQueue` + `@Async`** | `OfflineOperationQueue` |
| Hangfire | **(ileride) Spring `@Scheduled` / Quartz** | recurring job |
| Redis (StackExchange) | **(ileride) Spring Data Redis** | cache |
| ML.NET | **(ileride) DJL / Tribuo / ONNX Runtime** | anomali tespiti |
| Ocelot (API Gateway) | **(ileride) Spring Cloud Gateway** | reverse proxy |
| NLog | **SLF4J + Logback** | yerleşik |
| appsettings.json | **application.yml** | config |
| SQL Server / PostgreSQL | **H2 (öğrenme), JDBC sürücüsü ile değişir** | |

## Çok Modüllü Yapı (Clean Architecture + Ports & Adapters)

Her katman ayrı bir **Maven modülü** (.NET solution'daki ayrı projelerin karşılığı).
Bağımlılık yönü **daima içe doğru**: `api → infrastructure → application → domain`.
Maven, ters yönde bir import'u **derleme zamanında engeller** — mimari kurallar artık belgeyle değil
build ile zorlanır.

```
payguard-parent (pom)                         .NET: PayGuard.sln
├─ payguard-domain          → Rule, Scenario, Transaction, enum'lar   (.NET: Domain*, Domain.Shared)
│      bağımlılık: yok (sadece jakarta.persistence-api)  ·  Spring BİLMEZ
├─ payguard-application     → CQRS (Command/Handler/Mediator), FraudParameters,
│                             ScenarioService, kuyruk, PORT arayüzleri  (.NET: PayGuard.Application)
│      bağımlılık: domain + spring-context/tx
├─ payguard-infrastructure  → JPA adapter, RuleEvaluator(SpEL), CardScenarioProcessor  (.NET: Infrastructure + PayGRulesEngine)
│      bağımlılık: application + spring-boot-starter-data-jpa
├─ payguard-api             → Controller + güvenlik + bootstrap (çalıştırılabilir jar)  (.NET: PayGuard.Internal.API)
│      bağımlılık: infrastructure (gerisini transitif alır) + web + security + actuator
└─ payguard-gateway         → API Gateway / reverse proxy (8090, çalıştırılabilir jar)  (.NET: PayGuard.External.API / Ocelot)
       bağımlılık: spring-cloud-starter-gateway (reaktif, bağımsız uygulama)
```

### Ports & Adapters (neden önemli)
Application katmanı altyapıyı **arayüz (port)** üzerinden kullanır; gerçek implementasyon (adapter)
infrastructure'dadır. Bu, .NET'teki "interface Application'da, implementasyon Infrastructure'da" deseninin
birebir karşılığıdır:

| Port (application) | Adapter (infrastructure) | .NET karşılığı |
|---|---|---|
| `TransactionStore` | `JpaTransactionStore` → `TransactionJpaRepository` | `IRuleDapperRepository` + Dapper impl |
| `ScenarioProcessor` | `CardScenarioProcessor` | `IScenarioProcessor` + `ScenarioProcessorFactory` |

Spring, tüm `ScenarioProcessor` bean'lerini `ScenarioService`'e **liste** olarak enjekte eder; servis
ürün tipi → işlemci eşlemesini kurar (factory). Yeni ürün (PF/PayCell) eklemek = yeni bir bean yazmak;
mevcut kod değişmez (Open/Closed).

## Uçtan Uca Akış (dikey dilim)

```
POST /api/v1/transactions/get-fraud-response-for-card
  → TransactionController
  → Mediator.send(command)
  → GetFraudResponseForCardHandler
      1) Transaction kaydet + duplicate kontrol
      2) FraudParameters üret
      3) ScenarioService → (ürün tipine göre) XxxScenarioProcessor
            → ScenarioCatalog DB'den senaryoları yükler (ScenarioRow→Scenario)
            → BaseScenarioProcessor paralel değerlendirir (SpEL kural)
      4) fraudResponseCode (APPROVE / REJECT / REVIEW) istemciye döner
      5) offlinePublisher.publish(...) → Outbox tablosuna yazılır (4. ile AYNI transaction'da, atomik)
         → OutboxRelay (@Scheduled) sonradan yayımlar
```

### Ürün tipleri ve veri kaynağı
- `ScenarioService` tüm `ScenarioProcessor` bean'lerini ürün tipine göre eşler: **CARD, PF, PAYCELL, TRKART**.
  Her biri `BaseScenarioProcessor`'dan türer; yalnızca `supportedType()` ile ayrışır.
- Senaryolar artık **H2 veritabanından** okunur. `ScenarioSeeder` açılışta örnek veri yükler
  (2 CARD + 1 PF senaryosu). Tabloları görmek için: `http://localhost:8080/h2-console`
  (`scenarios` ve `rules` tabloları, JDBC URL: `jdbc:h2:mem:payguard`).

## Derleme & Çalıştırma (Java 21 + Maven olan ortamda)

```bash
# kök dizinden tüm modülleri derle
mvn clean install

# 1) Internal API'yi ayağa kaldır (8080)
mvn -pl payguard-api -am spring-boot:run

# 2) (opsiyonel) Gateway'i ayağa kaldır (8090 → 8080'e proxy) — .NET External API karşılığı
mvn -pl payguard-gateway -am spring-boot:run
```

Örnek istek (artık **JWT korumalı** — önce login, sonra Bearer token):
```bash
# 1) Token al (demo şifre: payguard123)
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"payguard123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

# 2) Fraud kontrolü (Gateway üzerinden gitmek için 8080 yerine 8090 kullan)
curl -X POST http://localhost:8080/api/v1/transactions/get-fraud-response-for-card \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "module": 1,
        "transactionMessageId": 1001,
        "shadowCardNo": "CARD123",
        "amount": 6000,
        "merchantId": "MERCH1",
        "transactionDate": "2026-01-01T03:00:00Z"
      }'
```
Beklenen: yüksek tutar (>5000) **REJECT** senaryosunu tetikler.

Testler: `mvn test` (SpEL kural değerlendirme testleri).

## Sıradaki Adımlar (yol haritası)
- [x] **Çok modüllü Maven yapısı** (4 katman modülü) + Ports & Adapters deseni.
- [x] **PF/PayCell/TrKart processor'ları** — `BaseScenarioProcessor` soyutlaması + 4 alt sınıf (.NET Base+Card deseni).
- [x] **Senaryolar DB'den okunuyor** — `ScenarioRow`/`RuleRow` JPA entity'leri (@OneToMany), `ScenarioCatalog` adapter row→domain map'ler, `ScenarioSeeder` örnek veri yükler.
- [x] **Cache abstraction** — `@Cacheable` ile senaryo yükleme cache'lenir; `CacheController` evict eder. Varsayılan in-memory; `redis` profili ile Redis (`CacheController` + `RedisSettings` karşılığı).
- [x] **JdbcTemplate hot-path** — `JdbcTransactionStore` (ham SQL, Dapper karşılığı). `payguard.persistence.transaction-store=jdbc` ile JPA adapter yerine devreye girer (koşullu bean).
- [x] **ML anomali servisi** — `AnomalyDetector` port + `StatisticalAnomalyDetector` (hibrit skor: z-score+frekans+zaman+model), `CheckTransaction` CQRS akışı, `AIController`. Gerçek DJL/ONNX modeli `modelScore` noktasına takılır.
- [x] **JWT güvenlik + Actuator** — `SecurityConfig` (stateless), `JwtService`/`JwtAuthenticationFilter`, `AuthController` (login). `/actuator/health` açık. (.NET JwtBearer + HealthChecks karşılığı.)
- [x] **Spring Cloud Gateway** — ayrı `payguard-gateway` modülü (8090→8080 proxy, route'lar yml'de). .NET `External.API`/Ocelot karşılığı.
- [x] **Kalıcı kuyruk (Outbox pattern)** — in-memory kuyruk kaldırıldı. `OfflineOperationPublisher` port → `OutboxOfflineOperationPublisher` iş kaydıyla aynı transaction'da `OutboxMessage` yazar; `OutboxRelay` (@Scheduled) yayımlar. Kayıp riski yok.
- [x] **Kafka/RabbitMQ publisher** — `MessagePublisher` port + 3 impl (`logging` vars. / `kafka` / `rabbit`), `payguard.outbox.publisher` ile seçilir. `OutboxRelay` artık bu port üzerinden gerçek broker'a yayımlar.
- [x] **Çok-tenant DB yönlendirme** — `TenantContext` (thread-local) + `TenantFilter` (`X-Tenant` header) + `TenantRoutingDataSource` (`AbstractRoutingDataSource`). `multitenant` profili ile tenant başına ayrı DB (.NET `ITenantProvider`/`ITenantDatabaseProvider` karşılığı).
- [x] **Flyway migration** — şema `db/migration/V1__init.sql`'de; Hibernate `ddl-auto: none` (Flyway sahibi). .NET EF Core Migrations karşılığı.
- [x] **Entegrasyon testleri** — `FraudFlowIntegrationTest` (@SpringBootTest + MockMvc): login→token, token'sız 4xx, yüksek tutar→REJECT, AI check→anomaly. Gerçek context + H2 + Flyway + seeder + güvenlik.
- [ ] (kalan) Liquibase alternatifi, per-tenant Flyway, Testcontainers ile gerçek Postgres/Kafka testleri, OpenAPI/Swagger.

## Yapılandırma anahtarları (application.yml)
| Anahtar | Değerler | Etki |
|---|---|---|
| `payguard.persistence.transaction-store` | `jpa` (vars.) / `jdbc` | İşlem yazımı: ORM mi ham SQL mi |
| `payguard.ai.enabled` | `true` (vars.) / `false` | Anomali tespiti açık/kapalı (kapalıyken `NoOpAnomalyDetector`) |
| `spring.cache.type` | `simple` (vars.) / `redis` | Cache sağlayıcı (`redis` profili Redis'e geçer) |
| `payguard.scenario.parallel` / `max-parallelism` | bool / int | Senaryo paralel yürütme |
| `payguard.security.jwt-secret` / `demo-password` | string | JWT imzalama anahtarı / login demo şifresi |
| `payguard.outbox.poll-interval-ms` | int (vars. 5000) | Outbox relay tarama sıklığı |
| `payguard.outbox.publisher` | `logging` (vars.) / `kafka` / `rabbit` | Outbox yayım hedefi (broker seçimi) |
| profil `multitenant` | aktif/değil | Tenant başına ayrı DB yönlendirme (`X-Tenant` header) |
| `spring.flyway.enabled` + `ddl-auto: none` | — | Şema Flyway ile yönetilir (V*.sql) |

> Not: Flyway H2 ile `flyway-core` yeterlidir; Postgres/MySQL için `flyway-database-*` modülü eklenir.
> Kafka/Rabbit yalnızca `publisher` o değere ayarlanınca broker'a bağlanır; varsayılan `logging` offline çalışır.

## Ek uç noktalar
```bash
# Anomali kontrolü (AIController)
curl -X POST http://localhost:8080/api/v1/ai/check-transaction \
  -H "Content-Type: application/json" \
  -d '{"transactionId":"11111111-1111-1111-1111-111111111111","shadowCardNo":"CARD123","amount":50000,"merchantId":"M1","transactionDate":"2026-01-01T03:00:00Z"}'

# Senaryo cache temizleme (CacheController)
curl -X POST http://localhost:8080/api/v1/cache/evict-scenarios
```
