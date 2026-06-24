-- ============================================================
-- Seed: Fraudulent transactions
-- Patterns:
--   1. ATM velocity        — same card, multiple ATMs within minutes
--   2. Geographic anomaly  — card used in two cities simultaneously
--   3. Account takeover    — large online purchases at 03:00
--   4. CashSend bust-out   — rapid CashSend to multiple numbers
--   5. Transfer layering   — smurfing via small EFTs to different banks
--   6. Friendly fraud      — POS reversed after goods received
-- ============================================================

INSERT INTO payments.transactions (
    transaction_id, transaction_date, posting_date, amount, balance,
    channel, trancode, trantypedesc, money_in, card_nr,
    merchant_name, merchant_desc, merchant_category_code, city, province,
    cif_nr, account_nr, branch,
    auth_trace_id, card_auth_status
) VALUES

-- ----------------------------------------------------------------
-- Pattern 1: ATM velocity — cif 200001 hits 5 ATMs in 9 minutes at 02:00
-- ----------------------------------------------------------------
('TXN-FR-V001', '2024-03-10 02:01:00', '2024-03-10', -1000.00, 9000.00,  'ATM',      4501, 'ATM Withdrawal', FALSE, '************3311', 'CAPITEC ATM', 'Capitec ATM Bellville',          6011, 'Bellville',     'Western Cape', 200001, 5000000001, 1002, 'FR-AUT-V001', 'APPROVED'),
('TXN-FR-V002', '2024-03-10 02:03:00', '2024-03-10', -1000.00, 8000.00,  'ATM',      4501, 'ATM Withdrawal', FALSE, '************3311', 'CAPITEC ATM', 'Capitec ATM Parow',              6011, 'Parow',         'Western Cape', 200001, 5000000001, 1002, 'FR-AUT-V002', 'APPROVED'),
('TXN-FR-V003', '2024-03-10 02:05:00', '2024-03-10', -1000.00, 7000.00,  'ATM',      4501, 'ATM Withdrawal', FALSE, '************3311', 'CAPITEC ATM', 'Capitec ATM Goodwood',           6011, 'Goodwood',      'Western Cape', 200001, 5000000001, 1002, 'FR-AUT-V003', 'APPROVED'),
('TXN-FR-V004', '2024-03-10 02:07:00', '2024-03-10', -1000.00, 6000.00,  'ATM',      4501, 'ATM Withdrawal', FALSE, '************3311', 'CAPITEC ATM', 'Capitec ATM Maitland',           6011, 'Maitland',      'Western Cape', 200001, 5000000001, 1002, 'FR-AUT-V004', 'DECLINED'),
('TXN-FR-V005', '2024-03-10 02:10:00', '2024-03-10', -1000.00, 6000.00,  'ATM',      4501, 'ATM Withdrawal', FALSE, '************3311', 'ABSA ATM',    'ABSA ATM Thornton',              6011, 'Thornton',      'Western Cape', 200001, 5000000001, 1002, 'FR-AUT-V005', 'DECLINED'),

-- ----------------------------------------------------------------
-- Pattern 2: Geographic anomaly — cif 200002 card used Sandton then Cape Town 4 minutes apart
-- ----------------------------------------------------------------
('TXN-FR-G001', '2024-03-11 11:14:00', '2024-03-11', -4500.00, 5500.00,  'POS',      4001, 'POS Purchase',   FALSE, '************8899', 'SAMSUNG STORE', 'Samsung Sandton City',          5732, 'Sandton',       'Gauteng',      200002, 5000000002, 2001, 'FR-AUT-G001', 'APPROVED'),
('TXN-FR-G002', '2024-03-11 11:18:00', '2024-03-11', -3800.00, 1700.00,  'ONLINE',   4002, 'Online Purchase',FALSE, NULL,               'TAKEALOT',      'Takealot.com',                  5964, 'Online',        NULL,           200002, 5000000002, NULL, 'FR-AUT-G002', 'APPROVED'),
('TXN-FR-G003', '2024-03-11 11:20:00', '2024-03-11', -2900.00, 1700.00,  'ONLINE',   4002, 'Online Purchase',FALSE, NULL,               'WISH',          'Wish.com',                      5964, 'Online',        NULL,           200002, 5000000002, NULL, 'FR-AUT-G003', 'DECLINED'),

-- ----------------------------------------------------------------
-- Pattern 3: Account takeover — cif 200003 large online purchases at 03:00
-- ----------------------------------------------------------------
('TXN-FR-C001', '2024-03-12 03:02:00', '2024-03-12', -8750.00, 1250.00,  'ONLINE',   4002, 'Online Purchase',FALSE, NULL,               'ALIEXPRESS',    'AliExpress.com',                5964, 'Online',        NULL,           200003, 5000000003, NULL, 'FR-AUT-C001', 'APPROVED'),
('TXN-FR-C002', '2024-03-12 03:04:00', '2024-03-12', -7200.00, 1250.00,  'ONLINE',   4002, 'Online Purchase',FALSE, NULL,               'SHEIN',         'Shein.com',                     5651, 'Online',        NULL,           200003, 5000000003, NULL, 'FR-AUT-C002', 'APPROVED'),
('TXN-FR-C003', '2024-03-12 03:06:00', '2024-03-12', -6500.00, 1250.00,  'ONLINE',   4002, 'Online Purchase',FALSE, NULL,               'DHGATE',        'DHgate.com',                    5964, 'Online',        NULL,           200003, 5000000003, NULL, 'FR-AUT-C003', 'DECLINED'),

-- ----------------------------------------------------------------
-- Pattern 4: CashSend bust-out — cif 200004 sends CashSend to 5 numbers in 10 minutes
-- ----------------------------------------------------------------
('TXN-FR-CS01', '2024-03-13 14:00:00', '2024-03-13', -1000.00, 9000.00,  'CASHSEND', 4601, 'CashSend',       FALSE, NULL,               NULL,            'CashSend to +27810001111',       NULL, NULL,            NULL,           200004, 5000000004, NULL, 'FR-AUT-CS01', NULL),
('TXN-FR-CS02', '2024-03-13 14:02:00', '2024-03-13', -1000.00, 8000.00,  'CASHSEND', 4601, 'CashSend',       FALSE, NULL,               NULL,            'CashSend to +27820002222',       NULL, NULL,            NULL,           200004, 5000000004, NULL, 'FR-AUT-CS02', NULL),
('TXN-FR-CS03', '2024-03-13 14:04:00', '2024-03-13', -1000.00, 7000.00,  'CASHSEND', 4601, 'CashSend',       FALSE, NULL,               NULL,            'CashSend to +27830003333',       NULL, NULL,            NULL,           200004, 5000000004, NULL, 'FR-AUT-CS03', NULL),
('TXN-FR-CS04', '2024-03-13 14:06:00', '2024-03-13', -1000.00, 6000.00,  'CASHSEND', 4601, 'CashSend',       FALSE, NULL,               NULL,            'CashSend to +27840004444',       NULL, NULL,            NULL,           200004, 5000000004, NULL, 'FR-AUT-CS04', NULL),
('TXN-FR-CS05', '2024-03-13 14:09:00', '2024-03-13', -1000.00, 5000.00,  'CASHSEND', 4601, 'CashSend',       FALSE, NULL,               NULL,            'CashSend to +27850005555',       NULL, NULL,            NULL,           200004, 5000000004, NULL, 'FR-AUT-CS05', NULL),

-- ----------------------------------------------------------------
-- Pattern 5: Transfer layering (smurfing) — cif 200005 splits large amount
-- into small EFTs across 4 banks within 15 minutes to avoid detection
-- ----------------------------------------------------------------
('TXN-FR-T001', '2024-03-14 22:00:00', '2024-03-14', -4999.00, 16001.00, 'TRANSFER', 4701, 'Interbank Transfer',FALSE,NULL,              NULL,            'EFT to ABSA 4091234567',         NULL, NULL,            NULL,           200005, 5000000005, NULL, 'FR-AUT-T001', NULL),
('TXN-FR-T002', '2024-03-14 22:05:00', '2024-03-14', -4999.00, 11002.00, 'TRANSFER', 4701, 'Interbank Transfer',FALSE,NULL,              NULL,            'EFT to FNB 6201234567',          NULL, NULL,            NULL,           200005, 5000000005, NULL, 'FR-AUT-T002', NULL),
('TXN-FR-T003', '2024-03-14 22:09:00', '2024-03-14', -4999.00, 6003.00,  'TRANSFER', 4701, 'Interbank Transfer',FALSE,NULL,              NULL,            'EFT to Nedbank 1987654321',      NULL, NULL,            NULL,           200005, 5000000005, NULL, 'FR-AUT-T003', NULL),
('TXN-FR-T004', '2024-03-14 22:14:00', '2024-03-14', -4999.00, 1004.00,  'TRANSFER', 4701, 'Interbank Transfer',FALSE,NULL,              NULL,            'EFT to Standard Bank 0512345678',NULL, NULL,            NULL,           200005, 5000000005, NULL, 'FR-AUT-T004', NULL),

-- ----------------------------------------------------------------
-- Pattern 6: Friendly fraud — cif 200006 buys then reverses after goods received
-- ----------------------------------------------------------------
('TXN-FR-F001', '2024-03-05 10:00:00', '2024-03-05', -3200.00, 6800.00,  'ONLINE',   4002, 'Online Purchase',FALSE, NULL,               'SUPERBALIST',   'Superbalist.com',                5651, 'Online',        NULL,           200006, 5000000006, NULL, 'FR-AUT-F001', 'APPROVED'),
('TXN-FR-F002', '2024-03-05 10:05:00', '2024-03-05', -2800.00, 4000.00,  'ONLINE',   4002, 'Online Purchase',FALSE, NULL,               'ZANDO',         'Zando.co.za',                    5651, 'Online',        NULL,           200006, 5000000006, NULL, 'FR-AUT-F002', 'APPROVED'),
('TXN-FR-F003', '2024-03-12 09:00:00', '2024-03-12', 3200.00,  7200.00,  'ONLINE',   4003, 'Online Reversal',TRUE,  NULL,               'SUPERBALIST',   'Reversal - Superbalist.com',     5651, 'Online',        NULL,           200006, 5000000006, NULL, 'FR-AUT-F003', 'REVERSED'),
('TXN-FR-F004', '2024-03-12 09:10:00', '2024-03-12', 2800.00,  10000.00, 'ONLINE',   4003, 'Online Reversal',TRUE,  NULL,               'ZANDO',         'Reversal - Zando.co.za',         5651, 'Online',        NULL,           200006, 5000000006, NULL, 'FR-AUT-F004', 'REVERSED')
ON CONFLICT (transaction_id) DO NOTHING;
