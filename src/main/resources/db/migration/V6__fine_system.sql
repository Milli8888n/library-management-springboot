-- V6: SRS v1.2 Section 12 — Fine Policy, Payment, Waiver
-- Compatible with MariaDB/MySQL (production) and H2 MODE=MySQL (test)

-- =========================================================
-- 1. New columns on Borrowing
-- =========================================================
ALTER TABLE borrowing ADD COLUMN IF NOT EXISTS total_fine_amount  DECIMAL(12,0) NOT NULL DEFAULT 0;
ALTER TABLE borrowing ADD COLUMN IF NOT EXISTS paid_fine_amount   DECIMAL(12,0) NOT NULL DEFAULT 0;
ALTER TABLE borrowing ADD COLUMN IF NOT EXISTS waived_fine_amount DECIMAL(12,0) NOT NULL DEFAULT 0;
ALTER TABLE borrowing ADD COLUMN IF NOT EXISTS unpaid_fine_amount DECIMAL(12,0) NOT NULL DEFAULT 0;
ALTER TABLE borrowing ADD COLUMN IF NOT EXISTS fine_status        VARCHAR(20)   NOT NULL DEFAULT 'NONE';

-- Migrate existing total_fine → total_fine_amount
UPDATE borrowing SET total_fine_amount = total_fine, unpaid_fine_amount = total_fine WHERE total_fine > 0;

-- =========================================================
-- 2. Snapshot columns on BorrowingDetail
-- =========================================================
ALTER TABLE borrowing_detail ADD COLUMN IF NOT EXISTS late_days           BIGINT        NOT NULL DEFAULT 0;
ALTER TABLE borrowing_detail ADD COLUMN IF NOT EXISTS daily_fine_snapshot DECIMAL(12,0) NOT NULL DEFAULT 5000;
ALTER TABLE borrowing_detail ADD COLUMN IF NOT EXISTS grace_days_snapshot INT           NOT NULL DEFAULT 0;

-- =========================================================
-- 3. FinePolicy table
-- =========================================================
CREATE TABLE IF NOT EXISTS fine_policy (
    id                BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    daily_fine_amount DECIMAL(12,0) NOT NULL DEFAULT 5000,
    max_fine_amount   DECIMAL(12,0) NULL,
    grace_days        INT           NOT NULL DEFAULT 0,
    active            BOOLEAN       NOT NULL DEFAULT FALSE,
    effective_from    DATE          NOT NULL,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed default policy if none exists
INSERT INTO fine_policy (daily_fine_amount, max_fine_amount, grace_days, active, effective_from)
SELECT 5000, NULL, 0, TRUE, '2026-01-01'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM fine_policy WHERE active = TRUE);

-- =========================================================
-- 4. FinePayment table
-- =========================================================
CREATE TABLE IF NOT EXISTS fine_payment (
    id           BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    borrowing_id BIGINT        NOT NULL,
    amount       DECIMAL(12,0) NOT NULL,
    payment_date DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    method       VARCHAR(30)   NOT NULL DEFAULT 'CASH',
    note         VARCHAR(500)  NULL,
    created_by   VARCHAR(100)  NULL,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fine_payment_borrowing FOREIGN KEY (borrowing_id) REFERENCES borrowing(id)
);

CREATE INDEX IF NOT EXISTS idx_fine_payment_borrowing ON fine_payment(borrowing_id);

-- =========================================================
-- 5. FineWaiver table
-- =========================================================
CREATE TABLE IF NOT EXISTS fine_waiver (
    id            BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    borrowing_id  BIGINT        NOT NULL,
    amount        DECIMAL(12,0) NOT NULL,
    reason        VARCHAR(500)  NOT NULL,
    approved_by   VARCHAR(100)  NOT NULL,
    approved_date DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fine_waiver_borrowing FOREIGN KEY (borrowing_id) REFERENCES borrowing(id)
);

CREATE INDEX IF NOT EXISTS idx_fine_waiver_borrowing ON fine_waiver(borrowing_id);
