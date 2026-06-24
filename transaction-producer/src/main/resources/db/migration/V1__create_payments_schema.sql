CREATE SCHEMA IF NOT EXISTS payments;

CREATE TABLE payments.transactions (
    -- transactionMetadata
    transaction_id        VARCHAR(50)       NOT NULL PRIMARY KEY,
    transaction_date      TIMESTAMP         NOT NULL,
    posting_date          DATE              NOT NULL,
    amount                NUMERIC(19, 4)    NOT NULL,
    balance               NUMERIC(19, 4),

    -- paymentDetails
    channel               VARCHAR(20),
    trancode              INTEGER,
    trantypedesc          VARCHAR(75),
    money_in              BOOLEAN,
    card_nr               VARCHAR(100),

    -- merchantData
    merchant_name         VARCHAR(48),
    merchant_desc         VARCHAR(250),
    merchant_category_code INTEGER,
    city                  VARCHAR(41),
    province              VARCHAR(25),

    -- clientData
    cif_nr                BIGINT            NOT NULL,
    account_nr            BIGINT            NOT NULL,
    branch                INTEGER,

    -- authentication
    auth_trace_id         VARCHAR(25),
    card_auth_status      VARCHAR(51),

    -- audit
    inserted              TIMESTAMP         DEFAULT CURRENT_TIMESTAMP,
    updated               TIMESTAMP
);

CREATE TABLE payments.transaction_validation (
    id                    BIGSERIAL         PRIMARY KEY,
    transaction_id        VARCHAR(50)       NOT NULL REFERENCES payments.transactions(transaction_id) ON DELETE CASCADE,
    validated_at          TIMESTAMP         NOT NULL DEFAULT NOW(),
    validation_status     VARCHAR(20)       NOT NULL,
    validation_type       VARCHAR(50)       NOT NULL,
    failure_reason        VARCHAR(500),
    risk_score            NUMERIC(5, 2),
    validator_ref         VARCHAR(100),
    created_at            TIMESTAMP         NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_cif_nr       ON payments.transactions(cif_nr);
CREATE INDEX idx_transactions_account_nr   ON payments.transactions(account_nr);
CREATE INDEX idx_transactions_posting_date ON payments.transactions(posting_date);
CREATE INDEX idx_transactions_channel      ON payments.transactions(channel);
CREATE INDEX idx_txn_val_transaction_id    ON payments.transaction_validation(transaction_id);
CREATE INDEX idx_txn_val_status            ON payments.transaction_validation(validation_status);
