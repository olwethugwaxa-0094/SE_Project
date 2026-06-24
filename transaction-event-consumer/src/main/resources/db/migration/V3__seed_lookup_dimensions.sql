-- ============================================================
-- V2: Seed static lookup dimensions
-- ============================================================

INSERT INTO rules_engine.dim_payment_channel
    (channel_code, channel_desc, e_batch_id, e_ingest_id, e_operation, e_source_system, e_row_hash)
VALUES
    ('POS',      'Point of Sale purchase',               'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('POS')),
    ('ONLINE',   'Online purchase',                      'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('ONLINE')),
    ('ATM',      'ATM withdrawal or deposit',            'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('ATM')),
    ('CASHSEND', 'CashSend transfer',                    'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('CASHSEND')),
    ('TRANSFER', 'Interbank or own-account transfer',    'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('TRANSFER'))
ON CONFLICT (channel_code) DO NOTHING;

INSERT INTO rules_engine.dim_card_auth_status
    (status_code, status_desc, e_batch_id, e_ingest_id, e_operation, e_source_system, e_row_hash)
VALUES
    ('APPROVED', 'Transaction approved by issuer',        'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('APPROVED')),
    ('DECLINED', 'Transaction declined by issuer',        'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('DECLINED')),
    ('REVERSED', 'Transaction reversed after settlement', 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('REVERSED'))
ON CONFLICT (status_code) DO NOTHING;

-- UNKNOWN merchant sentinel — fallback for unresolvable merchants
INSERT INTO rules_engine.dim_merchant
    (merchant_name, merchant_desc, merchant_category_code, city, province,
     effective_from, is_current,
     e_batch_id, e_ingest_id, e_operation, e_source_system, e_row_hash)
VALUES
    ('UNKNOWN', 'Merchant data not available', NULL, NULL, NULL,
     CURRENT_TIMESTAMP, TRUE,
     'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('UNKNOWN'))
ON CONFLICT DO NOTHING;
