-- V7: unique accession numbers for every physical copy + circulation policy defaults (PostgreSQL).
-- Never edit V1-V6; this migration is additive and preserves customized rows.

-- 1. Accession number column (backfilled deterministically from the copy id).
ALTER TABLE book_copies ADD COLUMN accession_number VARCHAR(40) NULL;
UPDATE book_copies
SET accession_number = CONCAT('ACC-', TO_CHAR(COALESCE(acquired_on, CURRENT_DATE), 'YYYY'), '-', LPAD(id::text, 6, '0'))
WHERE accession_number IS NULL;
ALTER TABLE book_copies ALTER COLUMN accession_number SET NOT NULL;
ALTER TABLE book_copies ADD CONSTRAINT uk_copy_accession UNIQUE (accession_number);

-- 2. Twelve-hour default pickup window (only rows still on the old 48h default;
-- any administrator-customized hold window is left untouched).
UPDATE loan_policies SET hold_hours = 12 WHERE hold_hours = 48;

-- 3. Twenty-day standard student loan period (only rows still on the old 14-day default).
UPDATE loan_policies SET loan_days = 20 WHERE member_type = 'STUDENT' AND loan_days = 14;
