-- ============================================================
-- V5: Seed non-gambling blacklisted merchants
-- Table: rules_engine.dim_blacklisted_merchant
--
-- Categories covered (no gambling MCCs):
--   6051 — Quasi-cash / crypto exchange
--   6099 — Non-financial institutions / money transfer (unlicensed)
--   6141 — Personal credit / unregistered loan provider
--   6211 — Security brokers / unregistered forex dealer
--   4829 — Wire transfer / money order (unregistered MTO)
--   5968 — Continuity/subscription merchants (negative-option traps)
--   5999 — Miscellaneous retail (advance-fee scam / counterfeit fronts)
--   7372 — Computer programming / tech-support scam
-- ============================================================

INSERT INTO rules_engine.dim_blacklisted_merchant
    (merchant_name, merchant_category_code, reason, source, is_active,
     blacklisted_at, deactivated_at,
     e_batch_id, e_ingest_id, e_operation, e_source_system, e_row_hash)
VALUES

-- ----------------------------------------------------------------
-- Unregistered crypto exchanges / quasi-cash (MCC 6051)
-- ----------------------------------------------------------------
('CRYPTO SWAP FAST',
 6051,
 'Unregistered crypto exchange operating without FSCA licence. SARB advisory 2024-JAN-09. Peer-to-peer structuring: clients deposit R4,990 increments just below the R5,000 FICA threshold. FIC SAR-2024-0061: linked to proceeds of ransomware payments.',
 'REGULATORY', TRUE, '2024-01-12 08:00:00', NULL,
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('CRYPTO SWAP FAST')),

('COINFLIP ZA',
 6051,
 'FSCA enforcement action 2023-OCT-04. Ghost crypto broker with no order-book — client funds deposited are immediately swept to offshore wallets. VISA MC0 chargeback ratio: 42%. Confirmed Ponzi structure via FIC STR-2023-0089.',
 'REGULATORY', TRUE, '2023-10-10 08:00:00', NULL,
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('COINFLIP ZA')),

-- ----------------------------------------------------------------
-- Unregistered money transfer operators (MCC 4829 / 6099)
-- ----------------------------------------------------------------
('GLOBAL REMIT ZA',
 4829,
 'Unregistered money transfer operator — SARB directive 2024-FEB-07. Processes cross-border transfers without SARB exchange-control approval. Structuring pattern: multiple R4,999 transfers per day to same offshore IBAN from different client accounts.',
 'REGULATORY', TRUE, '2024-02-10 09:00:00', NULL,
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('GLOBAL REMIT ZA')),

('SPEEDPAY AFRICA',
 6099,
 'Shared indicator — Nedbank and Standard Bank fraud team (external watchlist 2024-MAR-01). Mule-network hub: receives inward transfers from compromised accounts and immediately sub-divides to 10+ recipient numbers via CashSend equivalents. FIC SAR-2024-0077.',
 'EXTERNAL', TRUE, '2024-03-05 08:00:00', NULL,
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('SPEEDPAY AFRICA')),

-- ----------------------------------------------------------------
-- Unregistered loan providers / mashonisas (MCC 6141)
-- ----------------------------------------------------------------
('FASTCASH LOANS ZA',
 6141,
 'Unregistered micro-lender charging 30-50% weekly interest — NCR enforcement action NCR-ENF-2023-044. Card-on-file used to debit recurring "processing fees" without client consent. FIC SAR-2024-0029: proceeds recycled through retail POS to obscure origin.',
 'REGULATORY', TRUE, '2023-12-01 08:00:00', NULL,
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('FASTCASH LOANS ZA')),

('INSTANT LOAN SA',
 6141,
 'Advance-fee loan scam — NCR complaint NCA-2024-1187. Victims pay a R500-R2,000 "insurance deposit" before loan disbursement; loan is never granted. High chargeback rate (51%). SAPS case 2024/03/14 opened for fraud and contravention of the NCA.',
 'INTERNAL', TRUE, '2024-03-16 09:00:00', NULL,
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('INSTANT LOAN SA')),

-- ----------------------------------------------------------------
-- Unregistered forex / investment brokers (MCC 6211)
-- ----------------------------------------------------------------
('FOREX FLIP ZA',
 6211,
 'Unregistered forex broker operating as a Ponzi scheme — FSCA public warning 2024-FEB-15. Promises 30% monthly returns. Client deposits accepted via card but withdrawals systematically blocked. FIC SAR-2024-0051: R28M in client funds unaccounted for.',
 'REGULATORY', TRUE, '2024-02-18 08:00:00', NULL,
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('FOREX FLIP ZA')),

('TRADE FAST CAPITAL',
 6211,
 'Internal investigation TXN-INV-2024-003: mirror-trading scheme. Clients receive fabricated trade confirmations; actual funds sent to offshore SPV. Accounts held by 14 confirmed mule clients at Capitec. FSCA referral pending.',
 'INTERNAL', TRUE, '2024-01-25 09:00:00', NULL,
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('TRADE FAST CAPITAL')),

-- ----------------------------------------------------------------
-- Tech-support scam / software subscription traps (MCC 7372 / 5968)
-- ----------------------------------------------------------------
('TECH SUPPORT SA',
 7372,
 'Tech-support scam operation — SAPS cybercrime unit case CS-2024-0041. Victims cold-called about "virus detected" and charged R1,500-R5,000 for fictitious software. Remote access used to initiate additional card transactions after initial payment.',
 'INTERNAL', TRUE, '2024-02-20 08:00:00', NULL,
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('TECH SUPPORT SA')),

('CLICK SUBSCRIBE ZA',
 5968,
 'Negative-option subscription trap — CPA complaint CPA-2024-0318. Victims enrolled in R299/month plans via pre-ticked consent on unrelated purchase pages. Cancellation pathway non-functional. FIC SAR-2024-0034: 1,200+ victim accounts identified across ZA banks.',
 'EXTERNAL', TRUE, '2024-02-28 09:00:00', NULL,
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('CLICK SUBSCRIBE ZA')),

-- ----------------------------------------------------------------
-- Advance-fee / 419 scam fronts (MCC 5999)
-- ----------------------------------------------------------------
('PRIZE CLAIM CENTER',
 5999,
 'Advance-fee (419) scam merchant — FIC SAR-2024-0019. Victims told they won a competition and must pay R500-R2,000 in "release fees". Merchant registered in Nigeria with ZA-issued card terminal. SAPS directive to freeze merchant settlement.',
 'REGULATORY', TRUE, '2024-01-18 08:00:00', NULL,
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('PRIZE CLAIM CENTER')),

('CHEAP GADGETS 4U',
 5999,
 'Counterfeit goods and phishing front — SARS customs seizure 2023-DEC-12. Advertises electronics online at 80% discount; ships counterfeit or nothing. Card credentials harvested at checkout via embedded payment skimmer. Chargeback rate: 67%.',
 'INTERNAL', TRUE, '2024-01-05 09:00:00', NULL,
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('CHEAP GADGETS 4U')),

-- ----------------------------------------------------------------
-- Deactivated historical entry — retained for audit trail
-- ----------------------------------------------------------------
('LOAN ADVANCE FAST',
 6141,
 'Historical unregistered mashonisa flagged 2022. Ceased trading after NCR deregistration 2023-09-30. Entry deactivated; retained for historical chargeback query support.',
 'INTERNAL', FALSE, '2022-06-01 08:00:00', '2023-10-01 00:00:00',
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('LOAN ADVANCE FAST'));
