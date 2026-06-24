# Transaction Producer

A Spring Boot microservice that accepts transaction events via a REST API and publishes them to an Apache Kafka topic. It also includes a scheduled batch producer that replays persisted transactions at a configurable interval.

## What It Does

- Exposes a REST endpoint to receive transaction events
- Persists events to PostgreSQL (`payments` schema)
- Computes a SHA-256 checksum for each event before publishing
- Publishes events asynchronously to the `transaction-events` Kafka topic
- Scheduled job periodically shuffles and republishes all stored transactions

## Prerequisites

- Java 21
- Maven 3.8+
- Running infrastructure (Kafka, PostgreSQL) — see the root `docker-compose.yml`

## Running the Application

### Start infrastructure first

From the project root:

```bash
docker-compose up -d
```

### Run with Maven

```bash
cd transaction-producer
mvn spring-boot:run
```

### Run the JAR

```bash
mvn clean package
java -jar target/transaction-producer-*.jar
```

The application starts on port **8080**.

## Configuration

Key settings in `src/main/resources/application.yaml`:

| Property | Default | Description |
|---|---|---|
| `spring.kafka.bootstrap-servers` | `localhost:19092,29092,39092` | Kafka broker addresses |
| `app.kafka.topic` | `transaction-events` | Target Kafka topic |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/connect_dev` | PostgreSQL connection |
| `spring.datasource.hikari.schema` | `payments` | Database schema |
| Scheduler fixed delay | 60 000 ms | Interval between scheduled batch runs |

## REST API

### Publish a transaction event

```
POST /v1/transactionevent
Content-Type: application/json
```

**Request body** — a `TransactionEvent` JSON object containing:

| Field | Type | Description |
|---|---|---|
| `transactionEvent` | object | Core transaction details |
| `transactionMetadata` | object | Channel, timestamp, correlation ID |
| `paymentDetails` | object | Amount, currency, account references |
| `clientData` | object | Client profile information |
| `merchantData` | object | Merchant details |
| `authentication` | object | Auth method and result |

**Response:**
- `201 Created` — event accepted and published to Kafka
- `400 Bad Request` — validation failure

## Kafka Publishing

Each message is published with:
- **Key** — derived from the transaction identifier
- **Value** — JSON-serialised `TransactionEvent`
- **Header** — `checksum` (SHA-256 hex of the serialised payload)

Publishing is fire-and-forget with async callbacks for success/failure logging.

## Database

Flyway migrations run automatically on startup and create the `payments` schema tables. The producer reads existing transactions via JPA for the scheduled batch job.

## Actuator Endpoints

| Endpoint | URL |
|---|---|
| Health | `http://localhost:8080/actuator/health` |
| Info | `http://localhost:8080/actuator/info` |
| Metrics | `http://localhost:8080/actuator/metrics` |
| Prometheus | `http://localhost:8080/actuator/prometheus` |

## Project Structure

```
transaction-producer/
└── src/main/java/za/co/capitecbank/transaction_producer/
    ├── TransactionProducerApplication.java
    ├── config/          # Kafka producer and topic configuration
    ├── constants/       # Kafka message constants
    ├── controller/      # REST endpoint
    ├── domain/          # TransactionEvent and related models
    ├── repository/      # JPA repository
    └── service/         # Kafka producer and mapper services
```
