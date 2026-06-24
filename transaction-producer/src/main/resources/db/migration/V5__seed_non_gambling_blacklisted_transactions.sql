-- ============================================================
-- V5: Fraudulent transactions at non-gambling blacklisted merchants
-- Table: payments.transactions
--
-- Rules targeted (score threshold = 70):
--   BLACKLISTED_MERCHANT         score 80  — isBlacklisted = true
--   HIGH_AMOUNT_ONLINE           score 70  — amount >= 10000, channel = ONLINE
--   MID_AMOUNT_ONLINE            score 50  — amount >= 5000,  channel = ONLINE
--   HIGH_VELOCITY_SPIKE_10M      score 80  — recentTxnCount10m > 2
--   MID_VELOCITY_SPIKE_10M       score 40  — recentTxnCount10m > 5
--   DECLINED_CARD_AUTH_STATUS    score 80  — cardAuthStatus = DECLINED
--   REVERSED_CARD_AUTH_STATUS    score 80  — cardAuthStatus = REVERSED
--
-- CIF range  400001–400020  reserved for this seed batch.
-- Account    7000000001–7000000020.
--
-- Fraud patterns per group:
--   F. Crypto exchange structuring     — BLACKLISTED_MERCHANT alone (score 80 >= 70)
--   G. High-value online + blacklisted — BLACKLISTED_MERCHANT + HIGH_AMOUNT_ONLINE
--   H. Advance-fee loan recurring hits — BLACKLISTED_MERCHANT + velocity
--   I. Forex Ponzi large deposits      — BLACKLISTED_MERCHANT + HIGH_AMOUNT_ONLINE
--   J. Tech-support scam remote access — BLACKLISTED_MERCHANT + DECLINED
--   K. Subscription trap enumeration   — BLACKLISTED_MERCHANT + velocity + DECLINED
--   L. 419 scam advance-fee            — BLACKLISTED_MERCHANT alone (daytime control)
--   M. Money transfer structuring      — BLACKLISTED_MERCHANT + velocity
--   N. Counterfeit + reversal fraud    — BLACKLISTED_MERCHANT + REVERSED
-- ============================================================

INSERT INTO payments.transactions (
    transaction_id, transaction_date, posting_date, amount, balance,
    channel, trancode, trantypedesc, money_in, card_nr,
    merchant_name, merchant_desc, merchant_category_code, city, province,
    cif_nr, account_nr, branch,
    auth_trace_id, card_auth_status
) VALUES

-- ================================================================
-- GROUP F: Crypto exchange structuring (CRYPTO SWAP FAST, MCC 6051)
-- Multiple R4,990 transactions just below FICA R5,000 threshold.
-- BLACKLISTED_MERCHANT alone scores 80 — above threshold.
-- cif 400001
-- ================================================================

('TXN-BL-F001', '2024-03-18 10:00:00', '2024-03-18', -4990.00, 45010.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'CRYPTO SWAP FAST', 'Crypto Swap Fast Exchange',
 6051, 'Online', NULL,
 400001, 7000000001, NULL, 'BL-AUT-F001', 'APPROVED'),

('TXN-BL-F002', '2024-03-18 10:45:00', '2024-03-18', -4990.00, 40020.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'CRYPTO SWAP FAST', 'Crypto Swap Fast Exchange',
 6051, 'Online', NULL,
 400001, 7000000001, NULL, 'BL-AUT-F002', 'APPROVED'),

('TXN-BL-F003', '2024-03-18 11:30:00', '2024-03-18', -4990.00, 35030.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'CRYPTO SWAP FAST', 'Crypto Swap Fast Exchange',
 6051, 'Online', NULL,
 400001, 7000000001, NULL, 'BL-AUT-F003', 'APPROVED'),

('TXN-BL-F004', '2024-03-19 09:00:00', '2024-03-19', -4990.00, 30040.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'CRYPTO SWAP FAST', 'Crypto Swap Fast Exchange',
 6051, 'Online', NULL,
 400001, 7000000001, NULL, 'BL-AUT-F004', 'APPROVED'),

-- cif 400002 — COINFLIP ZA: single large deposit then immediate cashout
('TXN-BL-F005', '2024-03-19 14:00:00', '2024-03-19', -18500.00, 1500.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'COINFLIP ZA', 'Coinflip ZA Crypto Broker',
 6051, 'Online', NULL,
 400002, 7000000002, NULL, 'BL-AUT-F005', 'APPROVED'),

('TXN-BL-F006', '2024-03-19 14:10:00', '2024-03-19', -1499.00, 1.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'COINFLIP ZA', 'Coinflip ZA Crypto Broker',
 6051, 'Online', NULL,
 400002, 7000000002, NULL, 'BL-AUT-F006', 'DECLINED'),

-- ================================================================
-- GROUP G: High-value online + blacklisted merchant
-- FOREX FLIP ZA (MCC 6211) — Ponzi investment deposit.
-- BLACKLISTED_MERCHANT (80) + HIGH_AMOUNT_ONLINE (70) = 150.
-- cif 400003
-- ================================================================

('TXN-BL-G001', '2024-03-20 11:00:00', '2024-03-20', -25000.00, 25000.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'FOREX FLIP ZA', 'Forex Flip ZA Investment',
 6211, 'Online', NULL,
 400003, 7000000003, NULL, 'BL-AUT-G001', 'APPROVED'),

('TXN-BL-G002', '2024-03-21 09:30:00', '2024-03-21', -15000.00, 10000.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'FOREX FLIP ZA', 'Forex Flip ZA Investment',
 6211, 'Online', NULL,
 400003, 7000000003, NULL, 'BL-AUT-G002', 'APPROVED'),

-- cif 400004 — TRADE FAST CAPITAL (MCC 6211) — mirror-trading mule
('TXN-BL-G003', '2024-03-20 14:00:00', '2024-03-20', -12000.00, 8000.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'TRADE FAST CAPITAL', 'Trade Fast Capital Forex',
 6211, 'Online', NULL,
 400004, 7000000004, NULL, 'BL-AUT-G003', 'APPROVED'),

('TXN-BL-G004', '2024-03-21 10:00:00', '2024-03-21', -8000.00, 0.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'TRADE FAST CAPITAL', 'Trade Fast Capital Forex',
 6211, 'Online', NULL,
 400004, 7000000004, NULL, 'BL-AUT-G004', 'DECLINED'),

-- ================================================================
-- GROUP H: Advance-fee loan recurring hits + velocity
-- INSTANT LOAN SA (MCC 6141) — "insurance fee" charged repeatedly.
-- BLACKLISTED_MERCHANT (80) alone exceeds threshold.
-- Velocity cluster also fires HIGH_VELOCITY_SPIKE_10M (80) if 3+ in 10 min.
-- cif 400005
-- ================================================================

('TXN-BL-H001', '2024-03-21 13:00:00', '2024-03-21', -1500.00, 8500.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'INSTANT LOAN SA', 'Instant Loan SA - Processing Fee',
 6141, 'Online', NULL,
 400005, 7000000005, NULL, 'BL-AUT-H001', 'APPROVED'),

('TXN-BL-H002', '2024-03-21 13:03:00', '2024-03-21', -1500.00, 7000.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'INSTANT LOAN SA', 'Instant Loan SA - Insurance Deposit',
 6141, 'Online', NULL,
 400005, 7000000005, NULL, 'BL-AUT-H002', 'APPROVED'),

('TXN-BL-H003', '2024-03-21 13:06:00', '2024-03-21', -2000.00, 5000.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'INSTANT LOAN SA', 'Instant Loan SA - Admin Fee',
 6141, 'Online', NULL,
 400005, 7000000005, NULL, 'BL-AUT-H003', 'APPROVED'),

('TXN-BL-H004', '2024-03-21 13:08:00', '2024-03-21', -2000.00, 3000.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'INSTANT LOAN SA', 'Instant Loan SA - Surety Fee',
 6141, 'Online', NULL,
 400005, 7000000005, NULL, 'BL-AUT-H004', 'DECLINED'),

-- FASTCASH LOANS ZA (MCC 6141) — weekly debit without consent
-- cif 400006
('TXN-BL-H005', '2024-03-04 08:00:00', '2024-03-04', -650.00, 4350.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'FASTCASH LOANS ZA', 'Fastcash Loans ZA - Weekly Debit',
 6141, 'Online', NULL,
 400006, 7000000006, NULL, 'BL-AUT-H005', 'APPROVED'),

('TXN-BL-H006', '2024-03-11 08:00:00', '2024-03-11', -650.00, 3700.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'FASTCASH LOANS ZA', 'Fastcash Loans ZA - Weekly Debit',
 6141, 'Online', NULL,
 400006, 7000000006, NULL, 'BL-AUT-H006', 'APPROVED'),

('TXN-BL-H007', '2024-03-18 08:00:00', '2024-03-18', -650.00, 3050.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'FASTCASH LOANS ZA', 'Fastcash Loans ZA - Weekly Debit',
 6141, 'Online', NULL,
 400006, 7000000006, NULL, 'BL-AUT-H007', 'APPROVED'),

-- ================================================================
-- GROUP I: Unregistered money transfer structuring
-- GLOBAL REMIT ZA (MCC 4829) — multiple R4,999 transfers per day.
-- BLACKLISTED_MERCHANT (80) alone exceeds threshold.
-- Velocity cluster adds HIGH_VELOCITY_SPIKE_10M (80).
-- cif 400007
-- ================================================================

('TXN-BL-I001', '2024-03-22 09:00:00', '2024-03-22', -4999.00, 35001.00,
 'TRANSFER', 4701, 'Interbank Transfer', FALSE, NULL,
 'GLOBAL REMIT ZA', 'Global Remit ZA - Wire Transfer',
 4829, NULL, NULL,
 400007, 7000000007, NULL, 'BL-AUT-I001', NULL),

('TXN-BL-I002', '2024-03-22 09:04:00', '2024-03-22', -4999.00, 30002.00,
 'TRANSFER', 4701, 'Interbank Transfer', FALSE, NULL,
 'GLOBAL REMIT ZA', 'Global Remit ZA - Wire Transfer',
 4829, NULL, NULL,
 400007, 7000000007, NULL, 'BL-AUT-I002', NULL),

('TXN-BL-I003', '2024-03-22 09:08:00', '2024-03-22', -4999.00, 25003.00,
 'TRANSFER', 4701, 'Interbank Transfer', FALSE, NULL,
 'GLOBAL REMIT ZA', 'Global Remit ZA - Wire Transfer',
 4829, NULL, NULL,
 400007, 7000000007, NULL, 'BL-AUT-I003', NULL),

-- SPEEDPAY AFRICA (MCC 6099) — mule hub receiving and redistributing
-- cif 400008
('TXN-BL-I004', '2024-03-22 10:00:00', '2024-03-22', 15000.00, 15000.00,
 'TRANSFER', 4702, 'Inward Transfer', TRUE, NULL,
 'SPEEDPAY AFRICA', 'Speedpay Africa - Inward',
 6099, NULL, NULL,
 400008, 7000000008, NULL, 'BL-AUT-I004', NULL),

('TXN-BL-I005', '2024-03-22 10:02:00', '2024-03-22', -4900.00, 10100.00,
 'CASHSEND', 4601, 'CashSend', FALSE, NULL,
 'SPEEDPAY AFRICA', 'Speedpay Africa - CashSend Out',
 6099, NULL, NULL,
 400008, 7000000008, NULL, 'BL-AUT-I005', NULL),

('TXN-BL-I006', '2024-03-22 10:04:00', '2024-03-22', -4900.00, 5200.00,
 'CASHSEND', 4601, 'CashSend', FALSE, NULL,
 'SPEEDPAY AFRICA', 'Speedpay Africa - CashSend Out',
 6099, NULL, NULL,
 400008, 7000000008, NULL, 'BL-AUT-I006', NULL),

('TXN-BL-I007', '2024-03-22 10:06:00', '2024-03-22', -4900.00, 300.00,
 'CASHSEND', 4601, 'CashSend', FALSE, NULL,
 'SPEEDPAY AFRICA', 'Speedpay Africa - CashSend Out',
 6099, NULL, NULL,
 400008, 7000000008, NULL, 'BL-AUT-I007', NULL),

-- ================================================================
-- GROUP J: Tech-support scam — remote access charges + DECLINED
-- TECH SUPPORT SA (MCC 7372).
-- BLACKLISTED_MERCHANT (80) + DECLINED_CARD_AUTH_STATUS (80) = 160.
-- cif 400009
-- ================================================================

('TXN-BL-J001', '2024-03-23 11:00:00', '2024-03-23', -3500.00, 16500.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'TECH SUPPORT SA', 'Tech Support SA - Virus Removal',
 7372, 'Online', NULL,
 400009, 7000000009, NULL, 'BL-AUT-J001', 'APPROVED'),

('TXN-BL-J002', '2024-03-23 11:15:00', '2024-03-23', -4800.00, 11700.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'TECH SUPPORT SA', 'Tech Support SA - Annual Protection Plan',
 7372, 'Online', NULL,
 400009, 7000000009, NULL, 'BL-AUT-J002', 'APPROVED'),

('TXN-BL-J003', '2024-03-23 11:30:00', '2024-03-23', -6000.00, 5700.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'TECH SUPPORT SA', 'Tech Support SA - Premium Cleanup',
 7372, 'Online', NULL,
 400009, 7000000009, NULL, 'BL-AUT-J003', 'DECLINED'),

-- cif 400010 — second victim, higher single charge
('TXN-BL-J004', '2024-03-24 14:00:00', '2024-03-24', -5000.00, 3000.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'TECH SUPPORT SA', 'Tech Support SA - Remote Fix',
 7372, 'Online', NULL,
 400010, 7000000010, NULL, 'BL-AUT-J004', 'DECLINED'),

-- ================================================================
-- GROUP K: Subscription trap enumeration + DECLINED velocity
-- CLICK SUBSCRIBE ZA (MCC 5968).
-- BLACKLISTED_MERCHANT (80) + DECLINED_CARD_AUTH_STATUS (80) = 160.
-- Rapid velocity burst adds HIGH_VELOCITY_SPIKE_10M (80).
-- cif 400011
-- ================================================================

('TXN-BL-K001', '2024-03-23 20:00:00', '2024-03-23', -299.00, 4701.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'CLICK SUBSCRIBE ZA', 'Click Subscribe ZA - Monthly Plan',
 5968, 'Online', NULL,
 400011, 7000000011, NULL, 'BL-AUT-K001', 'APPROVED'),

('TXN-BL-K002', '2024-03-23 20:01:00', '2024-03-23', -299.00, 4402.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'CLICK SUBSCRIBE ZA', 'Click Subscribe ZA - Premium Upgrade',
 5968, 'Online', NULL,
 400011, 7000000011, NULL, 'BL-AUT-K002', 'APPROVED'),

('TXN-BL-K003', '2024-03-23 20:02:00', '2024-03-23', -299.00, 4103.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'CLICK SUBSCRIBE ZA', 'Click Subscribe ZA - Add-on Pack',
 5968, 'Online', NULL,
 400011, 7000000011, NULL, 'BL-AUT-K003', 'DECLINED'),

('TXN-BL-K004', '2024-03-23 20:03:00', '2024-03-23', -299.00, 4103.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'CLICK SUBSCRIBE ZA', 'Click Subscribe ZA - Add-on Pack',
 5968, 'Online', NULL,
 400011, 7000000011, NULL, 'BL-AUT-K004', 'DECLINED'),

-- ================================================================
-- GROUP L: 419 advance-fee scam — PRIZE CLAIM CENTER (MCC 5999)
-- BLACKLISTED_MERCHANT (80) alone exceeds threshold.
-- Daytime — control group, no other rule stacking.
-- cif 400012
-- ================================================================

('TXN-BL-L001', '2024-03-25 10:00:00', '2024-03-25', -1000.00, 9000.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'PRIZE CLAIM CENTER', 'Prize Claim Center - Release Fee',
 5999, 'Online', NULL,
 400012, 7000000012, NULL, 'BL-AUT-L001', 'APPROVED'),

('TXN-BL-L002', '2024-03-26 11:00:00', '2024-03-26', -2000.00, 7000.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'PRIZE CLAIM CENTER', 'Prize Claim Center - Customs Clearance',
 5999, 'Online', NULL,
 400012, 7000000012, NULL, 'BL-AUT-L002', 'APPROVED'),

('TXN-BL-L003', '2024-03-27 09:30:00', '2024-03-27', -1500.00, 5500.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'PRIZE CLAIM CENTER', 'Prize Claim Center - Tax Deposit',
 5999, 'Online', NULL,
 400012, 7000000012, NULL, 'BL-AUT-L003', 'APPROVED'),

-- ================================================================
-- GROUP M: Counterfeit goods + reversal fraud
-- CHEAP GADGETS 4U (MCC 5999).
-- BLACKLISTED_MERCHANT (80) + REVERSED_CARD_AUTH_STATUS (80) = 160.
-- cif 400013
-- ================================================================

('TXN-BL-M001', '2024-03-15 10:00:00', '2024-03-15', -4500.00, 15500.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'CHEAP GADGETS 4U', 'Cheap Gadgets 4U - Electronics Bundle',
 5999, 'Online', NULL,
 400013, 7000000013, NULL, 'BL-AUT-M001', 'APPROVED'),

('TXN-BL-M002', '2024-03-15 10:05:00', '2024-03-15', -3200.00, 12300.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'CHEAP GADGETS 4U', 'Cheap Gadgets 4U - Smartphone Deal',
 5999, 'Online', NULL,
 400013, 7000000013, NULL, 'BL-AUT-M002', 'APPROVED'),

('TXN-BL-M003', '2024-03-22 09:00:00', '2024-03-22', 4500.00, 16800.00,
 'ONLINE', 4003, 'Online Reversal', TRUE, NULL,
 'CHEAP GADGETS 4U', 'Reversal - Cheap Gadgets 4U',
 5999, 'Online', NULL,
 400013, 7000000013, NULL, 'BL-AUT-M003', 'REVERSED'),

('TXN-BL-M004', '2024-03-22 09:05:00', '2024-03-22', 3200.00, 20000.00,
 'ONLINE', 4003, 'Online Reversal', TRUE, NULL,
 'CHEAP GADGETS 4U', 'Reversal - Cheap Gadgets 4U',
 5999, 'Online', NULL,
 400013, 7000000013, NULL, 'BL-AUT-M004', 'REVERSED')
ON CONFLICT (transaction_id) DO NOTHING;
