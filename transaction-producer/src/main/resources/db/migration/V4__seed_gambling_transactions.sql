-- ============================================================
-- V4: Seed gambling-house transactions + blacklisted-merchant hits
--
-- Rule targeted:
--   ODD_HOURS_GAMBLING_MCC (rule id 8, score 30)
--   — Gambling MCCs (7993, 7995, 7801) transacted midnight–05:00.
--   — Low score on its own; intended to combine with other rules.
--
-- Additional fraud patterns included:
--   A. Repeat blacklisted-merchant hits (BLACKLISTED_MERCHANT)
--   B. Odd-hours + blacklisted combined (elevated combined score)
--   C. Velocity on same blacklisted MID within minutes
--
-- CIF range 300001–300020 reserved for gambling-related seed clients.
-- Account range 6000000001–6000000020.
-- ============================================================

INSERT INTO payments.transactions (
    transaction_id, transaction_date, posting_date, amount, balance,
    channel, trancode, trantypedesc, money_in, card_nr,
    merchant_name, merchant_desc, merchant_category_code, city, province,
    cif_nr, account_nr, branch,
    auth_trace_id, card_auth_status
) VALUES

-- ================================================================
-- GROUP A: ODD_HOURS_GAMBLING_MCC only
-- Licensed / legitimate gambling sites used at odd hours.
-- Score ~30 — flagged alone as low risk; interesting when stacked.
-- ================================================================

-- A1: cif 300001 — recurring Hollywoodbets sessions 01:00-03:00
('TXN-GAMB-A001', '2024-03-01 01:15:00', '2024-03-01', -250.00, 4750.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'HOLLYWOODBETS', 'Hollywoodbets Online',
 7995, 'Online', NULL,
 300001, 6000000001, NULL, 'GAM-AUT-A001', 'APPROVED'),

('TXN-GAMB-A002', '2024-03-02 02:40:00', '2024-03-02', -500.00, 4250.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'HOLLYWOODBETS', 'Hollywoodbets Online',
 7995, 'Online', NULL,
 300001, 6000000001, NULL, 'GAM-AUT-A002', 'APPROVED'),

('TXN-GAMB-A003', '2024-03-03 00:55:00', '2024-03-03', -150.00, 4100.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'HOLLYWOODBETS', 'Hollywoodbets Online',
 7995, 'Online', NULL,
 300001, 6000000001, NULL, 'GAM-AUT-A003', 'APPROVED'),

-- A2: cif 300002 — Betway POS at a casino venue, after midnight
('TXN-GAMB-A004', '2024-03-05 01:30:00', '2024-03-05', -800.00, 9200.00,
 'POS', 4001, 'POS Purchase', FALSE, '************6612',
 'BETWAY CASINO', 'Betway GrandWest Casino',
 7801, 'Cape Town', 'Western Cape',
 300002, 6000000002, 1001, 'GAM-AUT-A004', 'APPROVED'),

('TXN-GAMB-A005', '2024-03-05 03:10:00', '2024-03-05', -1200.00, 8000.00,
 'POS', 4001, 'POS Purchase', FALSE, '************6612',
 'BETWAY CASINO', 'Betway GrandWest Casino',
 7801, 'Cape Town', 'Western Cape',
 300002, 6000000002, 1001, 'GAM-AUT-A005', 'APPROVED'),

-- A3: cif 300003 — video game / eSports betting at 04:45
('TXN-GAMB-A006', '2024-03-06 04:45:00', '2024-03-06', -300.00, 5700.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'SUPABETS', 'Supabets eSports',
 7993, 'Online', NULL,
 300003, 6000000003, NULL, 'GAM-AUT-A006', 'APPROVED'),

('TXN-GAMB-A007', '2024-03-07 03:55:00', '2024-03-07', -450.00, 5250.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'SUPABETS', 'Supabets eSports',
 7993, 'Online', NULL,
 300003, 6000000003, NULL, 'GAM-AUT-A007', 'APPROVED'),

-- A4: cif 300004 — lottery purchase in dead hours (00:05)
('TXN-GAMB-A008', '2024-03-08 00:05:00', '2024-03-08', -50.00, 2950.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'ITHUBA LOTTO', 'Ithuba National Lottery',
 7995, 'Online', NULL,
 300004, 6000000004, NULL, 'GAM-AUT-A008', 'APPROVED'),

('TXN-GAMB-A009', '2024-03-09 04:20:00', '2024-03-09', -50.00, 2900.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'ITHUBA LOTTO', 'Ithuba National Lottery',
 7995, 'Online', NULL,
 300004, 6000000004, NULL, 'GAM-AUT-A009', 'APPROVED'),

-- A5: cif 300005 — Sun City POS gambling spend at 02:00
('TXN-GAMB-A010', '2024-03-10 02:00:00', '2024-03-10', -3500.00, 16500.00,
 'POS', 4001, 'POS Purchase', FALSE, '************7723',
 'SUN CITY CASINO', 'Sun City Resort Casino',
 7801, 'Sun City', 'North West',
 300005, 6000000005, 3001, 'GAM-AUT-A010', 'APPROVED'),

('TXN-GAMB-A011', '2024-03-10 04:10:00', '2024-03-10', -2000.00, 14500.00,
 'POS', 4001, 'POS Purchase', FALSE, '************7723',
 'SUN CITY CASINO', 'Sun City Resort Casino',
 7801, 'Sun City', 'North West',
 300005, 6000000005, 3001, 'GAM-AUT-A011', 'APPROVED'),

-- ================================================================
-- GROUP B: ODD_HOURS_GAMBLING_MCC + BLACKLISTED_MERCHANT combined
-- Transactions at blacklisted gambling MCCs between midnight–05:00.
-- Stacked score makes these high-priority flags.
-- ================================================================

-- B1: cif 300006 — SPIN PALACE ZA (blacklisted, MCC 7801) at 01:00
('TXN-GAMB-B001', '2024-03-11 01:00:00', '2024-03-11', -1500.00, 8500.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'SPIN PALACE ZA', 'Spin Palace ZA Online Casino',
 7801, 'Online', NULL,
 300006, 6000000006, NULL, 'GAM-AUT-B001', 'APPROVED'),

('TXN-GAMB-B002', '2024-03-11 01:45:00', '2024-03-11', -2000.00, 6500.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'SPIN PALACE ZA', 'Spin Palace ZA Online Casino',
 7801, 'Online', NULL,
 300006, 6000000006, NULL, 'GAM-AUT-B002', 'APPROVED'),

('TXN-GAMB-B003', '2024-03-11 02:30:00', '2024-03-11', -2500.00, 4000.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'SPIN PALACE ZA', 'Spin Palace ZA Online Casino',
 7801, 'Online', NULL,
 300006, 6000000006, NULL, 'GAM-AUT-B003', 'DECLINED'),

-- B2: cif 300007 — LOTTO FAST WIN (blacklisted, MCC 7995) at 03:30
-- Advance-fee lottery pattern: recurring small charges at night
('TXN-GAMB-B004', '2024-03-12 03:30:00', '2024-03-12', -100.00, 4900.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'LOTTO FAST WIN', 'Lotto Fast Win - Claim Your Prize',
 7995, 'Online', NULL,
 300007, 6000000007, NULL, 'GAM-AUT-B004', 'APPROVED'),

('TXN-GAMB-B005', '2024-03-13 03:35:00', '2024-03-13', -100.00, 4800.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'LOTTO FAST WIN', 'Lotto Fast Win - Claim Your Prize',
 7995, 'Online', NULL,
 300007, 6000000007, NULL, 'GAM-AUT-B005', 'APPROVED'),

('TXN-GAMB-B006', '2024-03-14 03:40:00', '2024-03-14', -200.00, 4600.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'LOTTO FAST WIN', 'Lotto Fast Win - Claim Your Prize',
 7995, 'Online', NULL,
 300007, 6000000007, NULL, 'GAM-AUT-B006', 'APPROVED'),

-- B3: cif 300008 — QUICKBET SHADOW (blacklisted, MCC 7993) card-testing pattern
-- Low-value R1-R5 transactions 01:00-03:00
('TXN-GAMB-B007', '2024-03-13 01:00:00', '2024-03-13', -1.00, 4999.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'QUICKBET SHADOW', 'Quickbet Shadow Betting',
 7993, 'Online', NULL,
 300008, 6000000008, NULL, 'GAM-AUT-B007', 'APPROVED'),

('TXN-GAMB-B008', '2024-03-13 01:02:00', '2024-03-13', -2.00, 4997.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'QUICKBET SHADOW', 'Quickbet Shadow Betting',
 7993, 'Online', NULL,
 300008, 6000000008, NULL, 'GAM-AUT-B008', 'APPROVED'),

('TXN-GAMB-B009', '2024-03-13 01:04:00', '2024-03-13', -5.00, 4992.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'QUICKBET SHADOW', 'Quickbet Shadow Betting',
 7993, 'Online', NULL,
 300008, 6000000008, NULL, 'GAM-AUT-B009', 'APPROVED'),

('TXN-GAMB-B010', '2024-03-13 01:06:00', '2024-03-13', -3.00, 4989.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'QUICKBET SHADOW', 'Quickbet Shadow Betting',
 7993, 'Online', NULL,
 300008, 6000000008, NULL, 'GAM-AUT-B010', 'APPROVED'),

('TXN-GAMB-B011', '2024-03-13 01:08:00', '2024-03-13', -50.00, 4939.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'QUICKBET SHADOW', 'Quickbet Shadow Betting',
 7993, 'Online', NULL,
 300008, 6000000008, NULL, 'GAM-AUT-B011', 'DECLINED'),

-- ================================================================
-- GROUP C: Velocity on blacklisted gambling MID
-- Same blacklisted merchant, multiple hits within minutes at night.
-- Combines BLACKLISTED_MERCHANT + ODD_HOURS_GAMBLING_MCC + velocity.
-- ================================================================

-- C1: cif 300009 — INSTABET ZA (blacklisted, MCC 7993) — 5 hits in 12 min at 02:00
('TXN-GAMB-C001', '2024-03-15 02:00:00', '2024-03-15', -500.00, 9500.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'INSTABET ZA', 'Instabet ZA Sports Betting',
 7993, 'Online', NULL,
 300009, 6000000009, NULL, 'GAM-AUT-C001', 'APPROVED'),

('TXN-GAMB-C002', '2024-03-15 02:02:00', '2024-03-15', -500.00, 9000.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'INSTABET ZA', 'Instabet ZA Sports Betting',
 7993, 'Online', NULL,
 300009, 6000000009, NULL, 'GAM-AUT-C002', 'APPROVED'),

('TXN-GAMB-C003', '2024-03-15 02:05:00', '2024-03-15', -500.00, 8500.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'INSTABET ZA', 'Instabet ZA Sports Betting',
 7993, 'Online', NULL,
 300009, 6000000009, NULL, 'GAM-AUT-C003', 'APPROVED'),

('TXN-GAMB-C004', '2024-03-15 02:08:00', '2024-03-15', -500.00, 8000.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'INSTABET ZA', 'Instabet ZA Sports Betting',
 7993, 'Online', NULL,
 300009, 6000000009, NULL, 'GAM-AUT-C004', 'DECLINED'),

('TXN-GAMB-C005', '2024-03-15 02:12:00', '2024-03-15', -500.00, 8000.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'INSTABET ZA', 'Instabet ZA Sports Betting',
 7993, 'Online', NULL,
 300009, 6000000009, NULL, 'GAM-AUT-C005', 'DECLINED'),

-- C2: cif 300010 — CRYPTO CASINO ZA (blacklisted, MCC 7801) — large amounts at 03:00
('TXN-GAMB-C006', '2024-03-16 03:00:00', '2024-03-16', -4500.00, 15500.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'CRYPTO CASINO ZA', 'Crypto Casino ZA',
 7801, 'Online', NULL,
 300010, 6000000010, NULL, 'GAM-AUT-C006', 'APPROVED'),

('TXN-GAMB-C007', '2024-03-16 03:05:00', '2024-03-16', -3800.00, 11700.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'CRYPTO CASINO ZA', 'Crypto Casino ZA',
 7801, 'Online', NULL,
 300010, 6000000010, NULL, 'GAM-AUT-C007', 'APPROVED'),

('TXN-GAMB-C008', '2024-03-16 03:09:00', '2024-03-16', -6000.00, 5700.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'CRYPTO CASINO ZA', 'Crypto Casino ZA',
 7801, 'Online', NULL,
 300010, 6000000010, NULL, 'GAM-AUT-C008', 'DECLINED'),

-- ================================================================
-- GROUP D: Daytime gambling at blacklisted MCCs (not ODD_HOURS)
-- These should trigger BLACKLISTED_MERCHANT only — control group.
-- ================================================================

-- D1: cif 300011 — SA MEGA LOTTERY during business hours
('TXN-GAMB-D001', '2024-03-17 10:30:00', '2024-03-17', -75.00, 4925.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'SA MEGA LOTTERY', 'SA Mega Lottery - Enter Now',
 7995, 'Online', NULL,
 300011, 6000000011, NULL, 'GAM-AUT-D001', 'APPROVED'),

('TXN-GAMB-D002', '2024-03-18 14:15:00', '2024-03-18', -75.00, 4850.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'SA MEGA LOTTERY', 'SA Mega Lottery - Enter Now',
 7995, 'Online', NULL,
 300011, 6000000011, NULL, 'GAM-AUT-D002', 'APPROVED'),

-- D2: cif 300012 — WIN2DAY ZA card-testing during business hours
('TXN-GAMB-D003', '2024-03-17 11:00:00', '2024-03-17', -1.00, 2999.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'WIN2DAY ZA', 'Win2Day ZA Lottery',
 7995, 'Online', NULL,
 300012, 6000000012, NULL, 'GAM-AUT-D003', 'APPROVED'),

('TXN-GAMB-D004', '2024-03-17 11:02:00', '2024-03-17', -1.00, 2998.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'WIN2DAY ZA', 'Win2Day ZA Lottery',
 7995, 'Online', NULL,
 300012, 6000000012, NULL, 'GAM-AUT-D004', 'APPROVED'),

('TXN-GAMB-D005', '2024-03-17 11:04:00', '2024-03-17', -2.00, 2996.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'WIN2DAY ZA', 'Win2Day ZA Lottery',
 7995, 'Online', NULL,
 300012, 6000000012, NULL, 'GAM-AUT-D005', 'APPROVED'),

-- D3: cif 300013 — LUCKY SPINS CASINO POS during the day
('TXN-GAMB-D006', '2024-03-18 16:00:00', '2024-03-18', -2000.00, 18000.00,
 'POS', 4001, 'POS Purchase', FALSE, '************4490',
 'LUCKY SPINS CASINO', 'Lucky Spins Casino Rosebank',
 7801, 'Johannesburg', 'Gauteng',
 300013, 6000000013, 2002, 'GAM-AUT-D006', 'APPROVED'),

('TXN-GAMB-D007', '2024-03-19 15:30:00', '2024-03-19', -3500.00, 14500.00,
 'POS', 4001, 'POS Purchase', FALSE, '************4490',
 'LUCKY SPINS CASINO', 'Lucky Spins Casino Rosebank',
 7801, 'Johannesburg', 'Gauteng',
 300013, 6000000013, 2002, 'GAM-AUT-D007', 'APPROVED'),

-- ================================================================
-- GROUP E: VIDEOSLOTS DARK — offshore carousel (MCC 7993, blacklisted)
-- Mixed timing: some odd-hours, some daytime. Tests rule stacking.
-- cif 300014
-- ================================================================

('TXN-GAMB-E001', '2024-03-20 00:15:00', '2024-03-20', -800.00, 7200.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'VIDEOSLOTS DARK', 'Videoslots Dark Gaming',
 7993, 'Online', NULL,
 300014, 6000000014, NULL, 'GAM-AUT-E001', 'APPROVED'),

('TXN-GAMB-E002', '2024-03-20 09:00:00', '2024-03-20', -800.00, 6400.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'VIDEOSLOTS DARK', 'Videoslots Dark Gaming',
 7993, 'Online', NULL,
 300014, 6000000014, NULL, 'GAM-AUT-E002', 'APPROVED'),

('TXN-GAMB-E003', '2024-03-20 02:30:00', '2024-03-20', -1200.00, 5200.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'VIDEOSLOTS DARK', 'Videoslots Dark Gaming',
 7993, 'Online', NULL,
 300014, 6000000014, NULL, 'GAM-AUT-E003', 'DECLINED'),

('TXN-GAMB-E004', '2024-03-21 14:00:00', '2024-03-21', -600.00, 5200.00,
 'ONLINE', 4002, 'Online Purchase', FALSE, NULL,
 'VIDEOSLOTS DARK', 'Videoslots Dark Gaming',
 7993, 'Online', NULL,
 300014, 6000000014, NULL, 'GAM-AUT-E004', 'APPROVED')
ON CONFLICT (transaction_id) DO NOTHING;
