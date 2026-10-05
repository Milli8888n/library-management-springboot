-- ===================================================================
-- Flyway Migration V3: Seed Roles and Users for Catalogue Ledger
-- ===================================================================

INSERT IGNORE INTO roles (id, name) VALUES
(1, 'ROLE_ADMIN'),
(2, 'ROLE_LIBRARIAN');

INSERT IGNORE INTO users (id, username, password, full_name, email, active) VALUES
(1, 'admin', '$2a$10$W4Lq512U3HAFyP1wMw7jxuLxqnycFwXYelW8.suoEs5EG0D8M1Q3q', 'Quản Trị Viên', 'admin@library.vn', TRUE),
(2, 'librarian', '$2a$10$kMtpgGbNGB.owbMZB/70U.PpEsFq/keaXhNiiVENOvdDYVMl1ol1m', 'Nguyễn Thu Hà', 'thuha.nguyen@library.vn', TRUE);

UPDATE users SET password = '$2a$10$W4Lq512U3HAFyP1wMw7jxuLxqnycFwXYelW8.suoEs5EG0D8M1Q3q', active = TRUE WHERE username = 'admin';
UPDATE users SET password = '$2a$10$kMtpgGbNGB.owbMZB/70U.PpEsFq/keaXhNiiVENOvdDYVMl1ol1m', active = TRUE WHERE username = 'librarian';

INSERT IGNORE INTO user_roles (user_id, role_id) VALUES
(1, 1),
(1, 2),
(2, 2);
