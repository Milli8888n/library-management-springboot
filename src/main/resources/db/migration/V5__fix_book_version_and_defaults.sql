-- ===================================================================
-- Flyway Migration V5: Fix Book version column for Optimistic Locking
-- ===================================================================

UPDATE book SET version = 0 WHERE version IS NULL;

ALTER TABLE book MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;
