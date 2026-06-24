-- ============================================================
-- V4: Seed blacklisted merchants
-- Table: rules_engine.dim_blacklisted_merchant
--
-- Sources:
--   REGULATORY — SARB / NGB / FIC regulatory advisories
--   EXTERNAL   — Industry watchlist / partner bank flags
--   INTERNAL   — Capitec internal fraud investigation
--
-- Gambling MCCs covered:
--   7801 — Casino / gambling establishments
--   7993 — Video amusement game supplies / betting platforms
--   7995 — Betting / lottery / casino chips
-- ============================================================

INSERT INTO rules_engine.dim_blacklisted_merchant
    (merchant_name, merchant_category_code, reason, source, is_active,
     blacklisted_at, deactivated_at,
     e_batch_id, e_ingest_id, e_operation, e_source_system, e_row_hash)
VALUES

-- ----------------------------------------------------------------
-- Unlicensed / fraudulent casinos (MCC 7801)
-- ----------------------------------------------------------------
('CRYPTO CASINO ZA',
 7801,
 'Unregistered crypto gambling platform — SARB advisory notice 2023-NOV-14. No NGB licence. Accepts card-not-present payments without 3DS enforcement. Payouts routed to offshore virtual IBANs.',
 'REGULATORY', TRUE, '2023-11-20 09:00:00', NULL,
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('CRYPTO CASINO ZA')),

('SPIN PALACE ZA',
 7801,
 'Fraudulent site cloning the legitimate SpinPalace brand. FIC SAR-2024-0018: used for structuring deposits. Domain hosted offshore with no ZA business registration. Triggers BLACKLISTED_MERCHANT + ODD_HOURS_GAMBLING_MCC combined pattern.',
 'INTERNAL', TRUE, '2024-01-15 08:00:00', NULL,
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('SPIN PALACE ZA')),

('LUCKY SPINS CASINO',
 7801,
 'NPA-linked money laundering front. NGB enforcement action NGB-ENF-2024-007. Receives repeated round-amount card deposits followed by same-day CRYPTO CASINO ZA cashout. Turnover does not match declared gaming revenue.',
 'REGULATORY', TRUE, '2024-02-01 08:00:00', NULL,
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('LUCKY SPINS CASINO')),

-- ----------------------------------------------------------------
-- Fraudulent lottery operators (MCC 7995)
-- ----------------------------------------------------------------
('LOTTO FAST WIN',
 7995,
 'Advance-fee lottery scam — FIC SAR-2024-0042. Victims charged recurring R50-R200 fees to "claim winnings" that do not exist. Pan-Africa operation with SA-registered merchant account. High chargeback rate (38%).',
 'INTERNAL', TRUE, '2024-01-08 09:30:00', NULL,
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('LOTTO FAST WIN')),

('SA MEGA LOTTERY',
 7995,
 'Clones Ithuba National Lottery branding and payment page. SARB cease-and-desist 2024-FEB-22. Harvests card credentials via fake payment form — no actual lottery draw conducted. Confirmed by SAPS cybercrime unit.',
 'REGULATORY', TRUE, '2024-02-25 08:00:00', NULL,
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('SA MEGA LOTTERY')),

('WIN2DAY ZA',
 7995,
 'Shared indicator from ABSA fraud team (external watchlist): ghost merchant used for card enumeration. 300+ low-value R1-R5 test transactions detected over a 72-hour window across multiple Capitec and ABSA cards.',
 'EXTERNAL', TRUE, '2024-03-01 10:00:00', NULL,
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('WIN2DAY ZA')),

-- ----------------------------------------------------------------
-- Unlicensed video game / sports betting platforms (MCC 7993)
-- ----------------------------------------------------------------
('QUICKBET SHADOW',
 7993,
 'Ghost merchant — unregistered MID with no physical or digital presence. NGB confirmed no licence on record. FIC SAR-2024-0055: card-testing vector; transactions cluster in 00:00-05:00 window at R1-R50 increments.',
 'INTERNAL', TRUE, '2024-03-05 08:00:00', NULL,
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('QUICKBET SHADOW')),

('INSTABET ZA',
 7993,
 'Unlicensed sports betting operator. NGB enforcement action NGB-ENF-2024-012. Accepts deposits but withholds winnings with fabricated "terms violations". Chargeback clustering on weekend night sessions (Fri-Sun 01:00-04:00).',
 'REGULATORY', TRUE, '2024-02-10 09:00:00', NULL,
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('INSTABET ZA')),

('VIDEOSLOTS DARK',
 7993,
 'VISA EU fraud team watchlist shared via industry alert 2024-JAN-18. Offshore operator targeting ZA cardholders with unlicensed video slot operations. Payments processed under carousel of acquirers to evade velocity checks.',
 'EXTERNAL', TRUE, '2024-01-20 08:00:00', NULL,
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('VIDEOSLOTS DARK')),

-- ----------------------------------------------------------------
-- Deactivated historical entry — retained for audit trail
-- ----------------------------------------------------------------
('JACKS CASINO ZA',
 7801,
 'Historically flagged 2022 for systematic chargeback abuse (win-then-reverse pattern). Merchant ceased trading 2023-06-30 following NGB licence revocation. Entry deactivated; retained for historical query support.',
 'INTERNAL', FALSE, '2022-08-01 08:00:00', '2023-07-01 00:00:00',
 'SEED', gen_random_uuid(), 'INSERT', 'flyway', md5('JACKS CASINO ZA'));
