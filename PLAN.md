# KẾ HOẠCH PHÁT TRIỂN HỆ THỐNG QUẢN LÝ MƯỢN TRẢ SÁCH THƯ VIỆN NỘI BỘ
<!-- /autoplan auto-review pipeline generated plan -->
**Hệ thống**: Thư viện nội bộ — Catalogue Ledger  
**Tài liệu tham chiếu**: [SRS_He_thong_quan_ly_muon_tra_sach (2).docx](file:///d:/Springboot/SRS_He_thong_quan_ly_muon_tra_sach%20(2).docx)  
**Thiết kế giao diện**: [Bộ 16 Màn hình Catalogue Ledger Figma/Stitch](file:///d:/Springboot/figma_16_screens)  
**Công nghệ**: Spring Boot 3.3.x, Java 21 LTS, Spring Data JPA, Thymeleaf, Spring Security, Flyway, MySQL 8.x

---

## 1. Phase 1: CEO Review (Chiến lược & Phạm vi Sản phẩm)

### 1.1 Thách thức tiền đề (Premise Challenge)
* **Tiền đề 1 (Ứng dụng cho ai)**: Hệ thống phục vụ Thủ thư và Ban quản lý thư viện nội bộ thao tác hàng ngày. Ưu tiên hàng đầu là **tính chính xác, tốc độ xử lý nghiệp vụ mượn/trả, bảo toàn dữ liệu tồn kho** thay vì tính năng mạng xã hội hay giải trí.
* **Tiền đề 2 (Độ bao phủ Use Case)**: Thực hiện đầy đủ 21 Use Case (toàn bộ 19 chức năng cốt lõi + 2 chức năng nâng cao UC-20 Phân quyền và UC-21 Quét quá hạn tự động) để đáp ứng trọn vẹn tiêu chí đánh giá khắt khe nhất.

### 1.2 Bảng phân định phạm vi (Scope Boundary)
| Trong phạm vi (In Scope) | Ngoài phạm vi phiên bản này (Out of Scope) |
| :--- | :--- |
| **UC-01 → 04**: Quản lý Thể loại (Thêm, sửa, xóa mềm, xem danh sách, chặn xóa nếu còn sách). | Thu tiền phạt trực tiếp qua cổng thanh toán online. |
| **UC-05 → 08**: Quản lý Sách (Thêm, sửa, xóa mềm, tìm kiếm đa tiêu chí, phân trang). | Gia hạn phiếu mượn tự động qua cổng portal độc giả. |
| **UC-09 → 13**: Quản lý Thành viên (Thêm, sửa, khóa/mở khóa tài khoản độc giả). | Đặt trước sách (Book reservation queue). |
| **UC-14 → 16**: Nghiệp vụ Mượn - Trả sách (Tạo phiếu nhiều sách, trả từng phần/toàn bộ, tính phạt 5.000 ₫/ngày). | Quét mã vạch phần cứng qua máy quét chuyên dụng. |
| **UC-17 → 19**: Báo cáo thống kê (Sách quá hạn kèm số ngày trễ, Top 5 mượn nhiều nhất, thống kê theo thành viên). | Đa chi nhánh thư viện, đồng bộ liên chi nhánh. |
| **UC-20**: Đăng nhập & phân quyền Spring Security (`ADMIN`, `LIBRARIAN`). | Ứng dụng Mobile Native (iOS / Android). |
| **UC-21**: Scheduled Task (`@Scheduled`) quét định kỳ tự động gắn cờ phiếu mượn `OVERDUE`. | Nhập / Xuất dữ liệu hàng loạt qua file Excel. |

### 1.3 Bảng quản trị Lỗi & Khắc phục (Error & Rescue Registry)
| Mã lỗi | Tình huống phát sinh | Cơ chế xử lý & Cứu vãn dữ liệu (Rescue) |
| :--- | :--- | :--- |
| **MSG-01 / MSG-02** | Tạo / sửa sách trùng ISBN hoặc dữ liệu trống | Báo lỗi validation viền đỏ tại trường nhập liệu, giữ nguyên dữ liệu đã gõ trên form. |
| **MSG-08** | Mượn vượt quá số lượng tồn kho khả dụng (`availableQuantity`) | Báo lỗi flash message trực tiếp trên dòng sách; kiểm tra khóa bi quan (`Pessimistic Lock`) tránh Race Condition khi 2 thủ thư mượn cùng lúc cuốn cuối cùng. |
| **MSG-09** | Thành viên đang bị khóa (`BLOCKED`) nhưng cố lập phiếu mượn | Chặn nút tạo phiếu, hiển thị banner cảnh báo trạng thái thẻ của thành viên. |
| **MSG-12** | Nhập số lượng sách trả lớn hơn số sách đang nợ | Validate phía client và server, tự động điền giá trị trần là số lượng nợ thực tế. |
| **MSG-18** | Cố xóa thể loại vẫn còn sách đang liên kết | Chặn xóa mềm, hiển thị hộp thoại cảnh báo danh sách sách đang thuộc thể loại này. |
| **MSG-403** | Thủ thư truy cập trang cấu hình của Admin | Bắt qua AccessDeniedHandler, render trang lỗi chuẩn `S7.2b — Lỗi 403` trong App Shell. |

---

## 2. Phase 2: Design Review (Giao diện Catalogue Ledger & Thymeleaf)

### 2.1 Ma trận ánh xạ 16 Màn hình Stitch sang Thymeleaf Templates
| # | Mã Artboard | Tên màn hình Stitch | File Template Thymeleaf (`src/main/resources/templates/`) |
| :---: | :---: | :--- | :--- |
| 1 | `S0.1` | Bảng thành phần Design System | `fragments/design-system.html` |
| 2 | `S7.1` | Đăng nhập hệ thống | `auth/login.html` |
| 3 | `S7.1-V1` | Đăng nhập sai thông tin | `auth/login.html` (kèm trạng thái Flash error) |
| 4 | `S0.2` | Danh sách Sách (Master Data) | `books/index.html` |
| 5 | `S1.1` | Danh sách Thể loại | `categories/index.html` |
| 6 | `S2.1` | Thêm sách mới | `books/create.html` |
| 7 | `S1.4-R` | Xác nhận khôi phục thể loại | `fragments/confirm-dialog.html` (Modal component) |
| 8 | `S3.1` | Danh sách Thành viên | `members/index.html` |
| 9 | `S3.4` | Xác nhận khóa thành viên | `members/lock-dialog.html` |
| 10 | `S4.1` | Lập phiếu mượn sách | `borrowings/create.html` |
| 11 | `S4.4` | Chi tiết phiếu mượn | `borrowings/detail.html` |
| 12 | `S5.1` | Trả sách (nhập số lượng) | `borrowings/return.html` |
| 13 | `S5.3` | Phiếu mượn đã hoàn tất | `borrowings/detail.html` (Badge "Đã trả") |
| 14 | `S6.1` | Báo cáo sách quá hạn | `reports/overdue.html` |
| 15 | `S6.2` | Báo cáo Top 5 sách mượn | `reports/top-borrowed.html` |
| 16 | `S7.2b` | Lỗi 403 Truy cập bị từ chối | `error/403.html` |

### 2.2 Tiêu chuẩn Thiết kế (Design Tokens)
* **Bảng màu (Ledger Palette)**:
  * Nền chính: `Parchment` (`#F4F1E8`), Bề mặt thẻ/bảng: `Paper` (`#FCFBF7`), Nền tiêu đề: `Linen` (`#EBE6D8`).
  * Đường viền: `Rule` (`#D8D2C2`), `Rule Strong` (`#A89F8A`).
  * Chữ: `Ink` (`#1B2230`), Phụ: `Ink Muted` (`#555C6B`).
  * Hành động chính / Sidebar: `Ink Blue` (`#1F4E8C`), `Sidebar Navy` (`#17233A`), Vạch active: `Brass` (`#C9A24A`).
  * Trạng thái nghiệp vụ: `Moss` (Hoạt động / Đã trả), `Ochre` (Cảnh báo / Tồn kho thấp), `Oxblood` (Quá hạn / Bị khóa / Ngừng).
* **Typography**:
  * Tiêu đề trang (H1, H2, Dialog): `Source Serif 4`, font-weight: 600.
  * Nội dung dữ liệu & nhãn: `IBM Plex Sans` (hỗ trợ 100% tiếng Việt có dấu xếp tầng).
  * Mã số ISBN, Mã phiếu mượn `#1044`: `IBM Plex Mono`.

---

## 3. Phase 2.5: DX Review (Developer Experience & Chất lượng Mã nguồn)

1. **Khởi chạy tức thì (Zero-friction Setup)**:
   * File cấu hình `application.properties` định hình sẵn kết nối MySQL chuẩn (`localhost:3306/library_db`).
   * Sử dụng Flyway tự động nạp bảng và dữ liệu mẫu (10 thể loại, 20 đầu sách, 15 độc giả, 10 phiếu mượn mẫu) để có thể test ngay mà không cần nhập liệu thủ công.
2. **Kiến trúc phân tầng rành mạch**:
   * Controller chỉ điều phối view, chuyển đổi DTO và flash message.
   * Toàn bộ nghiệp vụ kiểm tra tồn kho, tính ngày trễ, phạt tiền được đóng gói trọn vẹn trong Service layer với `@Transactional`.
3. **Phân tách DTO & Form Model**:
   * Không bind trực tiếp JPA Entity ra view form nhằm ngăn chặn Mass Assignment Vulnerability.
   * Form validation sử dụng `jakarta.validation.constraints` (`@NotBlank`, `@Size`, `@Min`, `@Pattern`).

---

## 4. Phase 3: Eng Review (Kiến trúc & Hạ tầng Kỹ thuật)

### 4.1 Sơ đồ Kiến trúc Phân tầng (ASCII Dependency Graph)
```text
[ Browser / Thymeleaf Web Client ]
               │
               ▼
[ Spring Security Filter Chain (UC-20) ] ── (Xác thực Session & Role ADMIN/LIBRARIAN)
               │
               ▼
   [ Controller Layer ] ◄── GlobalExceptionHandler (@ControllerAdvice)
         │           ▲
(Form DTOs)     (View Models)
         ▼           │
    [ Service Layer ] ◄── Scheduled Task (@Scheduled cron for UC-21)
         │
(Entities / Business Rules: BR-01 to BR-24)
         ▼
  [ Repository Layer (Spring Data JPA) ]
         │
(Pessimistic Write Lock on Book.availableQuantity)
         ▼
[ MySQL 8.x Database ] ◄── Flyway Migrations (V1__schema.sql, V2__seed.sql)
```

### 4.2 Xử lý Đồng thời & Giao dịch (Concurrency & Transactions)
* **Luồng Mượn sách**: Khi lập phiếu mượn gồm nhiều cuốn sách:
  * Áp dụng `@Transactional(isolation = Isolation.READ_COMMITTED)`.
  * Truy vấn sách với khóa bi quan: `@Lock(LockModeType.PESSIMISTIC_WRITE)` trên `BookRepository.findByIdWithLock(...)`.
  * Đảm bảo `availableQuantity = availableQuantity - borrowQty >= 0`. Nếu có bất kỳ dòng sách nào thiếu tồn kho, toàn bộ giao dịch được Rollback an toàn.
* **Luồng Trả sách**:
  * Cập nhật số lượng trả thực tế trên từng dòng chi tiết `BorrowingDetail`.
  * Hoàn trả tồn kho `availableQuantity = availableQuantity + returnQty`.
  * Tự động tính số ngày quá hạn: `so_ngay_tre = Math.max(0, ChronoUnit.DAYS.between(dueDate, returnDate))`.
  * Tự động tính tiền phạt: `tien_phat = so_ngay_tre * returnQty * 5.000 ₫`.
  * Nếu toàn bộ các cuốn trong phiếu đã được trả đủ -> cập nhật trạng thái phiếu mượn sang `RETURNED`.

### 4.3 Kế hoạch Kiểm thử Đơn vị (Test Plan)
```text
Test Cases
 ├── BookServiceTest
 │    ├── testCreateBook_Success()
 │    ├── testCreateBook_DuplicateIsbn_ThrowsException()
 │    └── testSoftDeleteBook_Success()
 ├── CategoryServiceTest
 │    ├── testDeleteCategory_HasActiveBooks_Blocked()
 │    └── testRestoreCategory_Success()
 ├── BorrowingServiceTest
 │    ├── testCreateBorrowing_Success()
 │    ├── testCreateBorrowing_BlockedMember_ThrowsException()
 │    └── testCreateBorrowing_InsufficientStock_Rollback()
 └── ReturnServiceTest
      ├── testReturnBook_OnTime_NoFine()
      ├── testReturnBook_Late_CalculatesFineCorrectly()
      └── testReturnBook_ExceedBorrowQuantity_Blocked()
```

---

## 5. Phase 4: Kế hoạch Triển khai Chi tiết (Implementation Roadmap)

### Task 1: Khởi tạo Project & Cấu hình Căn bản
- [x] Tạo file `pom.xml` với đầy đủ dependencies: `spring-boot-starter-web`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `mysql-connector-j`, `flyway-core`, `flyway-mysql`, `thymeleaf-extras-springsecurity6`.
- [x] Cấu hình `src/main/resources/application.properties` (Datasource MySQL, JPA Hibernate, Flyway, Thymeleaf cache false khi dev).

### Task 2: Cơ sở Dữ liệu & Flyway Migrations
- [x] Viết `V1__init_schema.sql`: Tạo các bảng `categories`, `books`, `members`, `borrowings`, `borrowing_details`, `users`, `roles`, `user_roles`.
- [x] Viết `V2__seed_sample_data.sql`: Nạp sẵn dữ liệu mẫu chuẩn theo Mục 6.6 - 6.8 của SRS (Sách, Thể loại, Thành viên, Tài khoản Admin/Thủ thư).

### Task 3: Entity, Enum & Repository Layer
- [x] Định nghĩa các Enum: `BookStatus`, `MemberStatus`, `BorrowingStatus`.
- [x] Xây dựng các Entity JPA với mối quan hệ chuẩn (`@ManyToOne`, `@OneToMany`, `@ManyToMany`).
- [x] Xây dựng các interface Spring Data JPA Repository với query methods và Custom Query (tìm kiếm sách, báo cáo top sách, báo cáo quá hạn).

### Task 4: DTO, Validation & Service Layer
- [x] Viết các DTO: `CategoryForm`, `BookForm`, `MemberForm`, `BorrowingForm`, `BorrowingItemForm`, `ReturnForm`.
- [x] Cài đặt `CategoryService`, `BookService`, `MemberService`.
- [x] Cài đặt `BorrowingService` & `ReturnService` với logic tính phạt và quản lý kho.
- [x] Cài đặt `ReportService` cho 3 nghiệp vụ thống kê.
- [x] Cài đặt `OverdueCheckScheduler` tự động quét phiếu mượn vào mỗi nửa đêm (`@Scheduled(cron = "0 0 0 * * ?")`).

### Task 5: Security & Xác thực Phân quyền (UC-20)
- [x] Cài đặt `SecurityConfig`, cấu hình mã hóa mật khẩu `BCryptPasswordEncoder`.
- [x] Phân quyền truy cập các đường dẫn theo role: `/admin/**` cho `ADMIN`, các nghiệp vụ chung cho `LIBRARIAN`.
- [x] Xử lý `CustomAccessDeniedHandler` điều hướng tới trang lỗi 403.

### Task 6: Giao diện Thymeleaf & Tích hợp 16 Màn hình Mẫu
- [x] Tạo layout chính `templates/layout.html` kết hợp các fragment `sidebar.html`, `header.html`, `flash.html`.
- [x] Ghép nối các file HTML từ [figma_16_screens](file:///d:/Springboot/figma_16_screens/html) vào Thymeleaf templates:
  - Form & Bảng thể loại (`categories/`)
  - Form & Bảng sách (`books/`)
  - Form & Bảng thành viên (`members/`)
  - Giao diện mượn và trả sách (`borrowings/`)
  - 3 màn hình báo cáo thống kê (`reports/`)
  - Màn hình đăng nhập & màn hình lỗi 403 (`auth/`, `error/`).

### Task 7: Unit Testing & Hoàn tất Nghiệm thu
- [x] Viết bộ Unit Test JUnit 5 & Mockito cho các Service.
- [x] Chạy `mvn test` đảm bảo 100% test cases pass màu xanh.
- [x] Kiểm tra xác nhận toàn bộ 21 Use Cases.

---

## Decision Audit Trail
| # | Phase | Quyết định | Phân loại | Nguyên tắc áp dụng | Lý do lựa chọn |
|---|---|---|---|---|---|
| D1 | CEO | Triển khai đủ 21 Use Cases | Scope | 1. Choose completeness | Đảm bảo tính hoàn chỉnh và đáp ứng tối đa tiêu chí chấm |
| D2 | Eng | Sử dụng MySQL 8.x + Flyway | Hạ tầng | 3. Pragmatic | Tuân thủ ràng buộc kỹ thuật của đề bài (không dùng in-memory cho bản nộp) |
| D3 | Design | Tích hợp 1:1 bộ 16 mẫu Catalogue Ledger từ Stitch | Frontend | 5. Explicit over clever | Giữ tính thẩm mỹ chuyên nghiệp, đồng bộ 100% với artboard Figma |
| D4 | Eng | Áp dụng Pessimistic Lock khi trừ kho sách | Kỹ thuật | 1. Choose completeness | Loại bỏ triệt để lỗi xung đột đồng thời (Race Condition) khi mượn sách |
| D5 | CEO | Chấp nhận toàn bộ 11 đề xuất nghiệp vụ của BA | Nghiệp vụ | 6. Bias toward action | Rõ ràng, thống nhất, không tạo ra điểm nghẽn nghiệp vụ |
