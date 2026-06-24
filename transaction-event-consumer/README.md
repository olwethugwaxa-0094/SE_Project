# Transaction Event Consumer

A Spring Boot microservice that consumes transaction events from Apache Kafka, validates them, evaluates them against a configurable fraud detection rules engine, assigns risk scores, and routes transactions to downstream topics for approval or manual review.

## What It Does

1. **Consumes** raw events from the `transaction-events` Kafka topic
2. **Validates** each event — checksum verification, required field checks, JSON integrity
3. **Persists** a source mirror record for audit and replay protection
4. **Enriches** valid events by resolving dimension data (client, merchant, channel, auth)
5. **Scores** events using a hot-swappable rules engine backed by Redis velocity metrics
6. **Routes** scored events:
   - Score **below threshold** → `transaction.approved`
   - Score **at/above threshold** → `transaction.review`
   - All scored events → `transaction.scored`
   - Failed/unprocessable → `transaction.dlq`

## Prerequisites

- Java 21
- Maven 3.8+
- Running infrastructure (Kafka, PostgreSQL, Redis) — see the root `docker-compose.yml`

## Running the Application

### Start infrastructure first

From the project root:

```bash
docker-compose up -d
```

### Run with Maven

```bash
cd transaction-event-consumer
mvn spring-boot:run
```

### Run the JAR

```bash
mvn clean package
java -jar target/transaction-event-consumer-*.jar
```

The application starts on port **8081**.

## Configuration

Key settings in `src/main/resources/application.yaml`:

| Property | Default | Description |
|---|---|---|
| `spring.kafka.bootstrap-servers` | `localhost:19092,29092,39092` | Kafka broker addresses |
| `app.kafka.source-topic` | `transaction-events` | Inbound topic |
| `app.kafka.consumer.group-id` | `transaction-event-consumer-group` | Consumer group |
| `app.kafka.consumer.concurrency` | `3` | Concurrent listener threads |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/connect_dev` | PostgreSQL connection |
| `spring.datasource.hikari.schema` | `rules_engine` | Database schema |
| `spring.data.redis.host` | `localhost` | Redis host |
| `spring.data.redis.port` | `6379` | Redis port |
| `app.rules.score-threshold` | `70` | Routing threshold |

## Fraud Detection Rules Engine

Rules are configured in `application.yaml` under `app.rules.rule-sets`. Each rule defines:

- **conditions** — field expressions evaluated against an enriched `EvaluationContext`
- **score** — risk points added when the rule matches
- **description** — human-readable label

Built-in example rules:

| Rule | Score | Description |
|---|---|---|
| `HIGH_AMOUNT_ONLINE` | 40 | High-value online transaction |
| `BLACKLISTED_MERCHANT` | 80 | Merchant on blacklist |
| Velocity spike rules | variable | Unusual transaction frequency |
| `ODD_HOURS_GAMBLING` | 30 | Gambling transaction between midnight and 5 AM |

Rule sets are **hot-swappable** — they can be updated without restarting the application.

## Velocity Tracking

Redis stores time-windowed transaction counts per client and merchant. These counts are available to rule conditions (e.g. `velocityCount > 5`). A **Resilience4j circuit breaker** wraps all Redis calls; if Redis is unavailable the consumer falls back to querying PostgreSQL velocity data, ensuring continued operation in degraded mode.

## Data Warehouse Schema

The consumer writes to a star-schema warehouse in the `rules_engine` PostgreSQL schema:

| Layer | Tables |
|---|---|
| Source mirror | `src_transaction_event` |
| Facts | `fact_transaction`, `fact_scored_transaction` |
| Dimensions | `dim_client`, `dim_account`, `dim_merchant`, `dim_payment_channel`, `dim_transaction_auth`, `dim_blacklisted_merchant` |

Dimensions follow the **SCD-2** (Slowly Changing Dimension Type 2) pattern to preserve historical snapshots.

## Kafka Topics

| Topic | Role |
|---|---|
| `transaction-events` | Source — inbound raw events |
| `transaction.scored` | Sink — all processed events with scores |
| `transaction.approved` | Sink — events below score threshold |
| `transaction.review` | Sink — events at or above score threshold |
| `transaction.dlq` | Dead letter queue — invalid/failed events |

## UI Access

After running `docker-compose up -d` from the project root, the following web UIs are available:

| Tool | URL | Credentials |
|---|---|---|
| Redis Insight | `http://localhost:5540` | None (no login required) |
| pgAdmin | `http://localhost:5050` | Email: `olwethuntsukumbini@capitecbank.co.za` / Password: `password` |
| Kafka UI | `http://localhost:11000` | None (no login required) |

## Actuator Endpoints

| Endpoint | URL |
|---|---|
| Health | `http://localhost:8081/actuator/health` |
| Info | `http://localhost:8081/actuator/info` |
| Metrics | `http://localhost:8081/actuator/metrics` |
| Prometheus | `http://localhost:8081/actuator/prometheus` |

## Running Tests

```bash
mvn test
```

Tests use an H2 in-memory database and Awaitility for async assertion. Integration tests verify end-to-end processing without requiring a live Kafka or Redis instance.

## Project Structure

```
transaction-event-consumer/
└── src/main/java/za/co/capitecbank/transaction_event_consumer/
    ├── TransactionEventConsumerApplication.java
    ├── config/          # Kafka consumer configuration
    ├── constants/       # Application-wide constants
    ├── domain/          # TransactionEvent, ScoredEvent, ValidationResult
    ├── entity/          # JPA entities (facts, dimensions, source mirror)
    ├── repository/      # JPA repositories (12 repositories)
    ├── rules/           # Rules engine, condition evaluator, context builder
    ├── service/         # Consumer, processing, validation, dimension services
    ├── validator/       # Transaction event validators
    └── velocity/        # Redis velocity tracking with DB fallback
```
