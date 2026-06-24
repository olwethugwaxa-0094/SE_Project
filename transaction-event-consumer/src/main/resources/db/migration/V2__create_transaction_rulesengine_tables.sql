-- ============================================================
-- V1: Transaction & Rules Engine Tables
-- Schema: rules_engine
--
-- Dimension tables use SCD Type 2 for client, account, and merchant.
-- Static lookup dimensions (channel, auth status) are insert-only.
-- fact_transaction covers all transaction events (VALID and INVALID),
--   with all TransactionEvent fields flattened directly.
-- fact_scored_transaction is the fraud scoring audit table,
--   also carrying all TransactionEvent fields.
-- src_transaction_event is an append-only raw source mirror,
--   with all TransactionEvent fields parsed from the payload.
-- All tables include housekeeping columns for ETL lineage tracking.
-- ============================================================

CREATE SCHEMA IF NOT EXISTS rules_engine;

-- ============================================================
-- HOUSEKEEPING COLUMNS (embedded in every table)
-- e_batch_id      : batch/job identifier that loaded the row
-- e_ingest_id     : unique UUID per ingest event
-- e_operation     : INSERT, UPDATE, DELETE
-- e_source_system : originating system name
-- e_row_hash      : SHA-256 of business columns for change detection
-- e_loaded_at     : timestamp row was written (set by application)
-- e_updated_at    : timestamp row was updated (set by application)
-- ============================================================

-- ============================================================
-- STATIC LOOKUP DIMENSIONS
-- ============================================================

CREATE TABLE IF NOT EXISTS rules_engine.dim_payment_channel (
    channel_key         BIGSERIAL       PRIMARY KEY,
    channel_code        VARCHAR(20)     NOT NULL UNIQUE,
    channel_desc        VARCHAR(100)    NOT NULL,
    e_batch_id          VARCHAR(100)    NOT NULL,
    e_ingest_id         UUID            NOT NULL,
    e_operation         VARCHAR(20)     NOT NULL,
    e_source_system     VARCHAR(100)    NOT NULL,
    e_row_hash          VARCHAR(64)     NOT NULL,
    e_loaded_at         TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    e_updated_at        TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS rules_engine.dim_card_auth_status (
    auth_status_key     BIGSERIAL       PRIMARY KEY,
    status_code         VARCHAR(51)     NOT NULL UNIQUE,
    status_desc         VARCHAR(100)    NOT NULL,
    e_batch_id          VARCHAR(100)    NOT NULL,
    e_ingest_id         UUID            NOT NULL,
    e_operation         VARCHAR(20)     NOT NULL,
    e_source_system     VARCHAR(100)    NOT NULL,
    e_row_hash          VARCHAR(64)     NOT NULL,
    e_loaded_at         TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    e_updated_at        TIMESTAMPTZ
);

-- ============================================================
-- SCD TYPE 2 DIMENSIONS
-- ============================================================

CREATE TABLE IF NOT EXISTS rules_engine.dim_client (
    client_key          BIGSERIAL       PRIMARY KEY,
    cif_nr              BIGINT          NOT NULL,
    effective_from      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_to        TIMESTAMP,
    is_current          BOOLEAN         NOT NULL DEFAULT TRUE,
    e_batch_id          VARCHAR(100)    NOT NULL,
    e_ingest_id         UUID            NOT NULL,
    e_operation         VARCHAR(20)     NOT NULL,
    e_source_system     VARCHAR(100)    NOT NULL,
    e_row_hash          VARCHAR(64)     NOT NULL,
    e_loaded_at         TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    e_updated_at        TIMESTAMPTZ
);

CREATE UNIQUE INDEX uidx_dim_client_cif_current
    ON rules_engine.dim_client (cif_nr) WHERE is_current = TRUE;
CREATE INDEX idx_dim_client_cif_nr     ON rules_engine.dim_client(cif_nr);
CREATE INDEX idx_dim_client_is_current ON rules_engine.dim_client(is_current);

CREATE TABLE IF NOT EXISTS rules_engine.dim_account (
    account_key         BIGSERIAL       PRIMARY KEY,
    account_nr          BIGINT          NOT NULL,
    cif_nr              BIGINT          NOT NULL,
    branch              INTEGER,
    effective_from      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_to        TIMESTAMP,
    is_current          BOOLEAN         NOT NULL DEFAULT TRUE,
    e_batch_id          VARCHAR(100)    NOT NULL,
    e_ingest_id         UUID            NOT NULL,
    e_operation         VARCHAR(20)     NOT NULL,
    e_source_system     VARCHAR(100)    NOT NULL,
    e_row_hash          VARCHAR(64)     NOT NULL,
    e_loaded_at         TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    e_updated_at        TIMESTAMPTZ
);

CREATE UNIQUE INDEX uidx_dim_account_nr_current
    ON rules_engine.dim_account (account_nr) WHERE is_current = TRUE;
CREATE INDEX idx_dim_account_account_nr ON rules_engine.dim_account(account_nr);
CREATE INDEX idx_dim_account_cif_nr     ON rules_engine.dim_account(cif_nr);
CREATE INDEX idx_dim_account_is_current ON rules_engine.dim_account(is_current);

CREATE TABLE IF NOT EXISTS rules_engine.dim_merchant (
    merchant_key            BIGSERIAL       PRIMARY KEY,
    merchant_name           VARCHAR(48)     NOT NULL,
    merchant_desc           VARCHAR(250),
    merchant_category_code  INTEGER,
    city                    VARCHAR(41),
    province                VARCHAR(25),
    effective_from          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_to            TIMESTAMP,
    is_current              BOOLEAN         NOT NULL DEFAULT TRUE,
    e_batch_id              VARCHAR(100)    NOT NULL,
    e_ingest_id             UUID            NOT NULL,
    e_operation             VARCHAR(20)     NOT NULL,
    e_source_system         VARCHAR(100)    NOT NULL,
    e_row_hash              VARCHAR(64)     NOT NULL,
    e_loaded_at             TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    e_updated_at            TIMESTAMPTZ
);

CREATE UNIQUE INDEX uidx_dim_merchant_name_current
    ON rules_engine.dim_merchant (merchant_name) WHERE is_current = TRUE;
CREATE INDEX idx_dim_merchant_name       ON rules_engine.dim_merchant(merchant_name);
CREATE INDEX idx_dim_merchant_mcc        ON rules_engine.dim_merchant(merchant_category_code);
CREATE INDEX idx_dim_merchant_is_current ON rules_engine.dim_merchant(is_current);

-- ============================================================
-- BLACKLISTED MERCHANTS
-- ============================================================

CREATE TABLE IF NOT EXISTS rules_engine.dim_blacklisted_merchant (
    blacklist_key           BIGSERIAL       PRIMARY KEY,
    merchant_name           VARCHAR(48)     NOT NULL,
    merchant_category_code  INTEGER,
    reason                  VARCHAR(500)    NOT NULL,
    source                  VARCHAR(20)     NOT NULL CHECK (source IN ('INTERNAL', 'EXTERNAL', 'REGULATORY')),
    is_active               BOOLEAN         NOT NULL DEFAULT TRUE,
    blacklisted_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deactivated_at          TIMESTAMP,
    e_batch_id              VARCHAR(100)    NOT NULL,
    e_ingest_id             UUID            NOT NULL,
    e_operation             VARCHAR(20)     NOT NULL,
    e_source_system         VARCHAR(100)    NOT NULL,
    e_row_hash              VARCHAR(64)     NOT NULL,
    e_loaded_at             TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    e_updated_at            TIMESTAMPTZ
);

CREATE INDEX idx_blacklist_merchant_name ON rules_engine.dim_blacklisted_merchant(merchant_name);
CREATE INDEX idx_blacklist_is_active     ON rules_engine.dim_blacklisted_merchant(is_active);
CREATE INDEX idx_blacklist_source        ON rules_engine.dim_blacklisted_merchant(source);

-- ============================================================
-- TRANSACTION AUTHORIZATION DIMENSION
-- ============================================================

CREATE TABLE IF NOT EXISTS rules_engine.dim_transaction_auth (
    auth_key            BIGSERIAL       PRIMARY KEY,
    auth_trace_id       VARCHAR(25)     NOT NULL,
    card_nr             VARCHAR(100),
    auth_status_key     BIGINT          NOT NULL REFERENCES rules_engine.dim_card_auth_status(auth_status_key),
    effective_from      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_to        TIMESTAMP,
    is_current          BOOLEAN         NOT NULL DEFAULT TRUE,
    e_batch_id          VARCHAR(100)    NOT NULL,
    e_ingest_id         UUID            NOT NULL,
    e_operation         VARCHAR(20)     NOT NULL,
    e_source_system     VARCHAR(100)    NOT NULL,
    e_row_hash          VARCHAR(64)     NOT NULL,
    e_loaded_at         TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    e_updated_at        TIMESTAMPTZ
);

CREATE UNIQUE INDEX uidx_dim_auth_trace_current
    ON rules_engine.dim_transaction_auth (auth_trace_id) WHERE is_current = TRUE;
CREATE INDEX idx_dim_auth_trace_id   ON rules_engine.dim_transaction_auth(auth_trace_id);
CREATE INDEX idx_dim_auth_is_current ON rules_engine.dim_transaction_auth(is_current);

-- ============================================================
-- RAW EVENT STORE — source mirror
-- Flattened TransactionEvent fields for direct querying.
-- ============================================================

CREATE TABLE IF NOT EXISTS rules_engine.src_transaction_event (
    id                      BIGSERIAL       PRIMARY KEY,

    -- Kafka provenance
    kafka_key               VARCHAR(100),
    kafka_topic             VARCHAR(200)    NOT NULL,
    kafka_partition         INTEGER         NOT NULL,
    kafka_offset            BIGINT          NOT NULL,
    received_at             TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    raw_payload             TEXT            NOT NULL,

    -- TransactionEvent header
    event_id                VARCHAR(100),
    event_type              VARCHAR(50),

    -- TransactionMetadata
    transaction_id          VARCHAR(100),
    transaction_date        TIMESTAMP,
    posting_date            DATE,
    amount                  NUMERIC(19, 4),
    balance                 NUMERIC(19, 4),

    -- PaymentDetails
    channel                 VARCHAR(20),
    trancode                INTEGER,
    trantypedesc            VARCHAR(75),
    money_in                BOOLEAN,
    card_nr                 VARCHAR(100),

    -- MerchantData
    merchant_name           VARCHAR(48),
    merchant_desc           VARCHAR(250),
    merchant_category_code  INTEGER,
    city                    VARCHAR(41),
    province                VARCHAR(25),

    -- ClientData
    cif_nr                  BIGINT,
    account_nr              BIGINT,
    branch                  INTEGER,

    -- Authentication
    auth_trace_id           VARCHAR(25),
    card_auth_status        VARCHAR(51),

    e_batch_id              VARCHAR(100)    NOT NULL,
    e_ingest_id             UUID            NOT NULL,
    e_operation             VARCHAR(20)     NOT NULL,
    e_source_system         VARCHAR(100)    NOT NULL,
    e_row_hash              VARCHAR(64)     NOT NULL,
    e_loaded_at             TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    e_updated_at            TIMESTAMPTZ,

    CONSTRAINT uq_src_event_partition_offset UNIQUE (kafka_topic, kafka_partition, kafka_offset)
);

CREATE INDEX idx_src_event_event_id    ON rules_engine.src_transaction_event(event_id);
CREATE INDEX idx_src_event_kafka_key   ON rules_engine.src_transaction_event(kafka_key);
CREATE INDEX idx_src_event_received_at ON rules_engine.src_transaction_event(received_at);
CREATE INDEX idx_src_event_cif_nr      ON rules_engine.src_transaction_event(cif_nr);
CREATE INDEX idx_src_event_txn_id      ON rules_engine.src_transaction_event(transaction_id);

-- ============================================================
-- FACT TABLE
-- All TransactionEvent fields flattened, plus dimension FKs.
-- Dimension FKs nullable for INVALID events.
-- ============================================================

CREATE TABLE IF NOT EXISTS rules_engine.fact_transaction (
    fact_key                BIGSERIAL           PRIMARY KEY,
    correlation_id          VARCHAR(36),
    status                  VARCHAR(20)         NOT NULL,

    -- TransactionEvent header
    event_id                VARCHAR(100),
    event_type              VARCHAR(50),

    -- TransactionMetadata
    transaction_id          VARCHAR(100)        NOT NULL UNIQUE,
    transaction_date        TIMESTAMP,
    posting_date            DATE,
    amount                  NUMERIC(19, 4),
    balance                 NUMERIC(19, 4),

    -- PaymentDetails
    channel                 VARCHAR(20),
    trancode                INTEGER,
    trantypedesc            VARCHAR(75),
    money_in                BOOLEAN,
    card_nr                 VARCHAR(100),

    -- MerchantData
    merchant_name           VARCHAR(48),
    merchant_desc           VARCHAR(250),
    merchant_category_code  INTEGER,
    city                    VARCHAR(41),
    province                VARCHAR(25),

    -- ClientData
    cif_nr                  BIGINT,
    account_nr              BIGINT,
    branch                  INTEGER,

    -- Authentication
    auth_trace_id           VARCHAR(25),
    card_auth_status        VARCHAR(51),

    -- Dimension FKs (nullable for INVALID)
    client_key              BIGINT              REFERENCES rules_engine.dim_client(client_key),
    account_key             BIGINT              REFERENCES rules_engine.dim_account(account_key),
    merchant_key            BIGINT              REFERENCES rules_engine.dim_merchant(merchant_key),
    channel_key             BIGINT              REFERENCES rules_engine.dim_payment_channel(channel_key),
    auth_key                BIGINT              REFERENCES rules_engine.dim_transaction_auth(auth_key),

    e_batch_id              VARCHAR(100)        NOT NULL,
    e_ingest_id             UUID                NOT NULL,
    e_operation             VARCHAR(20)         NOT NULL,
    e_source_system         VARCHAR(100)        NOT NULL,
    e_row_hash              VARCHAR(64)         NOT NULL,
    e_loaded_at             TIMESTAMPTZ         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    e_updated_at            TIMESTAMPTZ
);

CREATE INDEX idx_fact_transaction_date ON rules_engine.fact_transaction(transaction_date);
CREATE INDEX idx_fact_posting_date     ON rules_engine.fact_transaction(posting_date);
CREATE INDEX idx_fact_correlation_id   ON rules_engine.fact_transaction(correlation_id);
CREATE INDEX idx_fact_status           ON rules_engine.fact_transaction(status);
CREATE INDEX idx_fact_cif_nr           ON rules_engine.fact_transaction(cif_nr);
CREATE INDEX idx_fact_client_key       ON rules_engine.fact_transaction(client_key);
CREATE INDEX idx_fact_account_key      ON rules_engine.fact_transaction(account_key);
CREATE INDEX idx_fact_merchant_key     ON rules_engine.fact_transaction(merchant_key);
CREATE INDEX idx_fact_channel_key      ON rules_engine.fact_transaction(channel_key);
CREATE INDEX idx_fact_auth_key         ON rules_engine.fact_transaction(auth_key);

-- ============================================================
-- SCORING AUDIT TABLE
-- All TransactionEvent fields flattened for self-contained audit.
-- ============================================================

CREATE TABLE IF NOT EXISTS rules_engine.fact_scored_transaction (
    scored_key              BIGSERIAL       PRIMARY KEY,
    correlation_id          VARCHAR(36)     NOT NULL,

    -- TransactionEvent header
    event_id                VARCHAR(100),
    event_type              VARCHAR(50),

    -- TransactionMetadata
    transaction_id          VARCHAR(100)    NOT NULL,
    transaction_date        TIMESTAMP,
    posting_date            DATE,
    amount                  NUMERIC(19, 4),
    balance                 NUMERIC(19, 4),

    -- PaymentDetails
    channel                 VARCHAR(20),
    trancode                INTEGER,
    trantypedesc            VARCHAR(75),
    money_in                BOOLEAN,
    card_nr                 VARCHAR(100),

    -- MerchantData
    merchant_name           VARCHAR(48),
    merchant_desc           VARCHAR(250),
    merchant_category_code  INTEGER,
    city                    VARCHAR(41),
    province                VARCHAR(25),

    -- ClientData
    cif_nr                  BIGINT,
    account_nr              BIGINT,
    branch                  INTEGER,

    -- Authentication
    auth_trace_id           VARCHAR(25),
    card_auth_status        VARCHAR(51),

    -- Kafka provenance
    kafka_key               VARCHAR(100),

    -- Scoring output
    score                   INTEGER         NOT NULL,
    matched_rules           TEXT            NOT NULL DEFAULT '[]',
    rule_set_version        VARCHAR(50)     NOT NULL,
    degraded_mode           BOOLEAN         NOT NULL DEFAULT FALSE,
    routed_to               VARCHAR(100)    NOT NULL,
    scored_at               TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    e_batch_id              VARCHAR(100)    NOT NULL,
    e_ingest_id             UUID            NOT NULL,
    e_operation             VARCHAR(20)     NOT NULL,
    e_source_system         VARCHAR(100)    NOT NULL,
    e_row_hash              VARCHAR(64)     NOT NULL,
    e_loaded_at             TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    e_updated_at            TIMESTAMPTZ,

    CONSTRAINT uq_fact_scored_correlation_id UNIQUE (correlation_id)
);

CREATE INDEX idx_fact_scored_transaction_id ON rules_engine.fact_scored_transaction(transaction_id);
CREATE INDEX idx_fact_scored_cif_nr         ON rules_engine.fact_scored_transaction(cif_nr);
CREATE INDEX idx_fact_scored_score          ON rules_engine.fact_scored_transaction(score);
CREATE INDEX idx_fact_scored_routed_to      ON rules_engine.fact_scored_transaction(routed_to);
CREATE INDEX idx_fact_scored_scored_at      ON rules_engine.fact_scored_transaction(scored_at);
CREATE INDEX idx_fact_scored_degraded_mode  ON rules_engine.fact_scored_transaction(degraded_mode);
