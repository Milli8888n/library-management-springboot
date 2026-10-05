-- ===================================================================
-- Flyway Migration V2: Seed Sample Data for Catalogue Ledger
-- ===================================================================

-- 1. Roles & Users
INSERT INTO roles (id, name) VALUES
(1, 'ROLE_ADMIN'),
(2, 'ROLE_LIBRARIAN');

INSERT INTO users (id, username, password, full_name, email, active) VALUES
(1, 'admin', '$2a$12$ym3inAeF9joqTaAkdit4ouXuWRv.3VS6TgK9JVkmf1FIsI1lEqL5e', 'Quản Trị Viên', 'admin@library.vn', TRUE),
(2, 'librarian', '$2a$12$ar7ReNAdbRbA/CAwayw7TuQSkVSwWmq.DlNsrfgLocaFQiWi3DjWS', 'Nguyễn Thu Hà', 'thuha.nguyen@library.vn', TRUE);

INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1),
(1, 2),
(2, 2);

-- 2. Categories
INSERT INTO category (id, name, description, active) VALUES
(1, 'Công nghệ thông tin', 'Lập trình, cơ sở dữ liệu, kiến trúc phần mềm và trí tuệ nhân tạo', TRUE),
(2, 'Kinh tế & Quản trị', 'Quản trị kinh doanh, tài chính doanh nghiệp, đầu tư và khởi nghiệp', TRUE),
(3, 'Văn học cổ điển', 'Tiểu thuyết, truyện ngắn, thơ ca kinh điển thế giới và Việt Nam', TRUE),
(4, 'Khoa học tự nhiên', 'Vật lý thiên văn, hóa học ứng dụng, toán học và sinh học', TRUE),
(5, 'Kỹ năng sống', 'Giao tiếp thuyết trình, quản lý thời gian, tư duy phản biện', TRUE),
(6, 'Lịch sử & Triết học', 'Lịch sử nhân loại, triết học phương Tây và phương Đông', TRUE),
(7, 'Tâm lý học', 'Tâm lý hành vi, tâm lý học nhận thức và phát triển cá nhân', TRUE),
(8, 'Ngoại ngữ & Từ điển', 'Sách luyện thi IELTS, TOEIC, tiếng Nhật, tiếng Trung', TRUE),
(9, 'Báo chí & Truyền thông', 'Kỹ năng viết báo, truyền thông kỹ thuật số và PR', TRUE),
(10, 'Tạp chí cũ (Ngừng mượn)', 'Lưu trữ các số báo và tạp chí lưu chiểu cũ', FALSE);

-- 3. Books
INSERT INTO book (id, isbn, title, author, category_id, total_quantity, available_quantity, status) VALUES
(1, '9780132350884', 'Clean Code', 'Robert C. Martin', 1, 5, 4, 'ACTIVE'),
(2, '9780134685991', 'Effective Java (3rd Edition)', 'Joshua Bloch', 1, 4, 2, 'ACTIVE'),
(3, '9780062316110', 'Sapiens: Lược sử loài người', 'Yuval Noah Harari', 6, 6, 5, 'ACTIVE'),
(4, '9780743273565', 'The Great Gatsby', 'F. Scott Fitzgerald', 3, 3, 3, 'ACTIVE'),
(5, '9781491950357', 'Designing Data-Intensive Applications', 'Martin Kleppmann', 1, 4, 3, 'ACTIVE'),
(6, '9780321125217', 'Domain-Driven Design', 'Eric Evans', 1, 3, 3, 'ACTIVE'),
(7, '9780061122415', 'Giết con chim nhại', 'Harper Lee', 3, 5, 5, 'ACTIVE'),
(8, '9780307887894', 'The Lean Startup', 'Eric Ries', 2, 4, 4, 'ACTIVE'),
(9, '9781501124020', 'Nguyên lý Quản trị', 'Ray Dalio', 2, 5, 4, 'ACTIVE'),
(10, '9780143127741', 'Thinking, Fast and Slow', 'Daniel Kahneman', 7, 4, 3, 'ACTIVE'),
(11, '9780393609394', 'Vũ trụ trong vỏ hạt dẻ', 'Stephen Hawking', 4, 3, 2, 'ACTIVE'),
(12, '9780441172719', 'Dune (Xứ Cát)', 'Frank Herbert', 3, 4, 4, 'ACTIVE'),
(13, '9781982137274', 'Atomic Habits (Thay đổi tí hon)', 'James Clear', 5, 8, 7, 'ACTIVE'),
(14, '9780316418461', 'Đắc Nhân Tâm', 'Dale Carnegie', 5, 10, 9, 'ACTIVE'),
(15, '9780201633610', 'Design Patterns (GoF)', 'Erich Gamma et al.', 1, 2, 2, 'ACTIVE'),
(16, '9780385547345', 'Bí mật tư duy triệu phú', 'T. Harv Eker', 2, 5, 5, 'ACTIVE'),
(17, '9780679720201', 'Tội ác và Hình phạt', 'Fyodor Dostoevsky', 3, 2, 2, 'INACTIVE'),
(18, '9780345391803', 'The Hitchhiker Guide to the Galaxy', 'Douglas Adams', 3, 1, 0, 'ACTIVE'),
(19, '9780132350891', 'The Clean Coder', 'Robert C. Martin', 1, 3, 3, 'ACTIVE'),
(20, '9780321356680', 'Effective Java (2nd Edition - Cũ)', 'Joshua Bloch', 1, 2, 0, 'DELETED');

-- 4. Members
INSERT INTO member (id, full_name, email, phone, status) VALUES
(1, 'Nguyễn Văn An', 'an.nguyen@example.com', '0901234567', 'ACTIVE'),
(2, 'Trần Thị Bình', 'binh.tran@example.com', '0912345678', 'ACTIVE'),
(3, 'Lê Minh Cường', 'cuong.le@example.com', '0987654321', 'BLOCKED'),
(4, 'Phạm Hoàng Dũng', 'dung.pham@example.com', '0933445566', 'ACTIVE'),
(5, 'Võ Thị Em', 'em.vo@example.com', '0944556677', 'ACTIVE'),
(6, 'Đặng Quốc Hưng', 'hung.dang@example.com', '0977889900', 'ACTIVE'),
(7, 'Bùi Mai Lan', 'lan.bui@example.com', '0922334455', 'ACTIVE'),
(8, 'Hoàng Trọng Nghĩa', 'nghia.hoang@example.com', '0911223344', 'ACTIVE'),
(9, 'Ngô Gia Phú', 'phu.ngo@example.com', '0966778899', 'ACTIVE'),
(10, 'Đỗ Bích Thảo', 'thao.do@example.com', '0988990011', 'BLOCKED');

-- 5. Borrowings
-- Phiếu 1 (#1041): Quá hạn 16 ngày (Hạn 15/09/2026 so với hiện tại 01/10/2026), Độc giả Trần Thị Bình
INSERT INTO borrowing (id, member_id, borrow_date, due_date, status, total_fine) VALUES
(1, 2, '2026-09-01', '2026-09-15', 'OVERDUE', 160000);
INSERT INTO borrowing_detail (id, borrowing_id, book_id, quantity, returned_quantity, fine_amount) VALUES
(1, 1, 1, 1, 0, 80000), -- 16 ngày trễ * 1 cuốn * 5.000 = 80.000
(2, 1, 2, 1, 0, 80000); -- 16 ngày trễ * 1 cuốn * 5.000 = 80.000

-- Phiếu 2 (#1042): Đang mượn (BORROWING), hạn trả 12/10/2026, Độc giả Nguyễn Văn An
INSERT INTO borrowing (id, member_id, borrow_date, due_date, status, total_fine) VALUES
(2, 1, '2026-09-28', '2026-10-12', 'BORROWING', 0);
INSERT INTO borrowing_detail (id, borrowing_id, book_id, quantity, returned_quantity, fine_amount) VALUES
(3, 2, 3, 1, 0, 0),
(4, 2, 5, 1, 0, 0);

-- Phiếu 3 (#1043): Quá hạn 5 ngày (Hạn 26/09/2026), Độc giả Phạm Hoàng Dũng
INSERT INTO borrowing (id, member_id, borrow_date, due_date, status, total_fine) VALUES
(3, 4, '2026-09-12', '2026-09-26', 'OVERDUE', 50000);
INSERT INTO borrowing_detail (id, borrowing_id, book_id, quantity, returned_quantity, fine_amount) VALUES
(5, 3, 9, 1, 0, 25000),
(6, 3, 10, 1, 0, 25000);

-- Phiếu 4 (#1044): ĐÃ TRẢ HẾT (RETURNED) - Khớp với màn hình mẫu S5.3! Độc giả Võ Thị Em
INSERT INTO borrowing (id, member_id, borrow_date, due_date, returned_date, status, total_fine) VALUES
(4, 5, '2026-09-10', '2026-09-24', '2026-09-22', 'RETURNED', 0);
INSERT INTO borrowing_detail (id, borrowing_id, book_id, quantity, returned_quantity, fine_amount) VALUES
(7, 4, 11, 1, 1, 0),
(8, 4, 13, 1, 1, 0);

-- Phiếu 5 (#1045): Đang mượn cuốn cuối cùng của Sách 18 (Hitchhiker) -> availableQuantity = 0!
INSERT INTO borrowing (id, member_id, borrow_date, due_date, status, total_fine) VALUES
(5, 6, '2026-09-25', '2026-10-09', 'BORROWING', 0);
INSERT INTO borrowing_detail (id, borrowing_id, book_id, quantity, returned_quantity, fine_amount) VALUES
(9, 5, 18, 1, 0, 0),
(10, 5, 14, 1, 0, 0);
