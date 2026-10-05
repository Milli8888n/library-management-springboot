-- ===================================================================
-- Flyway Migration V4: Add missing columns to pre-existing tables
-- ===================================================================

-- book: add created_at
ALTER TABLE book
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Normalize book.status default
ALTER TABLE book MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- category: add created_at
ALTER TABLE category
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- borrowing: add created_at
ALTER TABLE borrowing
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Normalize borrowing.total_fine to DECIMAL(12,0)
ALTER TABLE borrowing MODIFY COLUMN total_fine DECIMAL(12,0) NOT NULL DEFAULT 0;
