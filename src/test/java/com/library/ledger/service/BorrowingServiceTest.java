package com.library.ledger.service;

import com.library.ledger.dto.BorrowingForm;
import com.library.ledger.dto.BorrowingItemForm;
import com.library.ledger.dto.ReturnForm;
import com.library.ledger.dto.ReturnItemForm;
import com.library.ledger.entity.Book;
import com.library.ledger.entity.Borrowing;
import com.library.ledger.entity.BorrowingDetail;
import com.library.ledger.entity.Member;
import com.library.ledger.enums.BorrowingStatus;
import com.library.ledger.enums.MemberStatus;
import com.library.ledger.exception.BusinessRuleException;
import com.library.ledger.repository.BookRepository;
import com.library.ledger.repository.BorrowingDetailRepository;
import com.library.ledger.repository.BorrowingRepository;
import com.library.ledger.repository.MemberRepository;
import com.library.ledger.service.impl.BorrowingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowingServiceTest {

    @Mock
    private BorrowingRepository borrowingRepository;

    @Mock
    private BorrowingDetailRepository detailRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private com.library.ledger.repository.FinePolicyRepository finePolicyRepository;

    @Spy
    private com.library.ledger.service.FineCalculationService fineCalculationService
            = new com.library.ledger.service.FineCalculationService();

    @InjectMocks
    private BorrowingServiceImpl borrowingService;

    /** Default policy used in return tests: 5,000 ₫/day, 0 grace days */
    private com.library.ledger.entity.FinePolicy defaultPolicy;

    private Member activeMember;
    private Member blockedMember;
    private Book book1;
    private Book book2;

    @BeforeEach
    void setUp() {
        defaultPolicy = com.library.ledger.entity.FinePolicy.builder()
                .dailyFineAmount(new BigDecimal("5000"))
                .graceDays(0)
                .maxFineAmount(null)
                .active(true)
                .effectiveFrom(java.time.LocalDate.now().minusDays(30))
                .build();
        lenient().when(finePolicyRepository.findByActiveTrue())
                .thenReturn(java.util.Optional.of(defaultPolicy));

        activeMember = Member.builder()
                .id(1L)
                .fullName("Nguyễn Văn An")
                .status(MemberStatus.ACTIVE)
                .build();

        blockedMember = Member.builder()
                .id(2L)
                .fullName("Trần Thị Khóa")
                .status(MemberStatus.BLOCKED)
                .build();

        book1 = Book.builder()
                .id(10L)
                .isbn("978-604-0-00001-1")
                .title("Lập trình Java căn bản")
                .totalQuantity(5)
                .availableQuantity(3)
                .build();

        book2 = Book.builder()
                .id(20L)
                .isbn("978-604-0-00002-2")
                .title("Cấu trúc dữ liệu & Giải thuật")
                .totalQuantity(4)
                .availableQuantity(0) // Out of stock
                .build();
    }

    @Test
    @DisplayName("Lập phiếu mượn thành công khi độc giả hợp lệ và còn đủ tồn kho")
    void testCreateBorrowing_Success() {
        BorrowingForm form = new BorrowingForm();
        form.setMemberId(1L);
        form.setBorrowDate(LocalDate.now());
        form.setDueDate(LocalDate.now().plusDays(14));

        BorrowingItemForm item = new BorrowingItemForm();
        item.setBookId(10L);
        item.setQuantity(2);
        form.setItems(List.of(item));

        when(memberRepository.findById(1L)).thenReturn(Optional.of(activeMember));
        when(bookRepository.findByIdWithLock(10L)).thenReturn(Optional.of(book1));
        when(borrowingRepository.save(any(Borrowing.class))).thenAnswer(i -> {
            Borrowing b = i.getArgument(0);
            b.setId(500L);
            return b;
        });

        Borrowing created = borrowingService.create(form);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(500L);
        assertThat(book1.getAvailableQuantity()).isEqualTo(1); // 3 - 2 = 1
        verify(bookRepository).save(book1);
        verify(borrowingRepository).save(any(Borrowing.class));
    }

    @Test
    @DisplayName("Chặn tạo phiếu mượn khi độc giả đang bị khóa (BLOCKED)")
    void testCreateBorrowing_BlockedMember_ThrowsException() {
        BorrowingForm form = new BorrowingForm();
        form.setMemberId(2L);
        form.setItems(List.of(new BorrowingItemForm(10L, 1)));

        when(memberRepository.findById(2L)).thenReturn(Optional.of(blockedMember));

        assertThatThrownBy(() -> borrowingService.create(form))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("hiện đang bị KHÓA");

        verify(bookRepository, never()).findByIdWithLock(any());
        verify(borrowingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném ngoại lệ và Rollback khi sách không đủ số lượng tồn kho")
    void testCreateBorrowing_InsufficientStock_ThrowsException() {
        BorrowingForm form = new BorrowingForm();
        form.setMemberId(1L);
        BorrowingItemForm item = new BorrowingItemForm(20L, 1); // book2 has 0 available
        form.setItems(List.of(item));

        when(memberRepository.findById(1L)).thenReturn(Optional.of(activeMember));
        when(bookRepository.findByIdWithLock(20L)).thenReturn(Optional.of(book2));

        assertThatThrownBy(() -> borrowingService.create(form))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không đủ số lượng tồn kho khả dụng");

        verify(borrowingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Trả sách đúng hạn: không phát sinh tiền phạt và hoàn tồn kho")
    void testReturnBook_OnTime_NoFine() {
        LocalDate borrowDate = LocalDate.now().minusDays(10);
        LocalDate dueDate = LocalDate.now().plusDays(4);

        Borrowing borrowing = Borrowing.builder()
                .id(600L)
                .member(activeMember)
                .borrowDate(borrowDate)
                .dueDate(dueDate)
                .status(BorrowingStatus.BORROWING)
                .totalFine(BigDecimal.ZERO)
                .details(new ArrayList<>())
                .build();

        BorrowingDetail detail = BorrowingDetail.builder()
                .id(1001L)
                .borrowing(borrowing)
                .book(book1)
                .quantity(2)
                .returnedQuantity(0)
                .fineAmount(BigDecimal.ZERO)
                .build();
        borrowing.addDetail(detail);

        when(borrowingRepository.findByIdWithDetails(600L)).thenReturn(Optional.of(borrowing));
        when(bookRepository.findByIdWithLock(10L)).thenReturn(Optional.of(book1));
        when(borrowingRepository.save(any(Borrowing.class))).thenAnswer(i -> i.getArgument(0));

        ReturnForm form = new ReturnForm();
        form.setBorrowingId(600L);
        form.setReturnDate(LocalDate.now()); // On time
        ReturnItemForm returnItem = new ReturnItemForm();
        returnItem.setDetailId(1001L);
        returnItem.setBookId(10L);
        returnItem.setReturnQuantity(2);
        form.setItems(List.of(returnItem));

        Borrowing result = borrowingService.processReturn(form);

        assertThat(result.getStatus()).isEqualTo(BorrowingStatus.RETURNED);
        assertThat(result.getTotalFine()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(book1.getAvailableQuantity()).isEqualTo(5); // 3 + 2 = 5
        verify(detailRepository).save(detail);
    }

    @Test
    @DisplayName("Trả sách quá hạn: tính đúng tiền phạt theo công thức (5.000 ₫/ngày trễ)")
    void testReturnBook_Late_CalculatesFineCorrectly() {
        LocalDate borrowDate = LocalDate.now().minusDays(20);
        LocalDate dueDate = LocalDate.now().minusDays(5); // 5 days late
        LocalDate returnDate = LocalDate.now();

        Borrowing borrowing = Borrowing.builder()
                .id(601L)
                .member(activeMember)
                .borrowDate(borrowDate)
                .dueDate(dueDate)
                .status(BorrowingStatus.OVERDUE)
                .totalFine(BigDecimal.ZERO)
                .details(new ArrayList<>())
                .build();

        BorrowingDetail detail = BorrowingDetail.builder()
                .id(1002L)
                .borrowing(borrowing)
                .book(book1)
                .quantity(2)
                .returnedQuantity(0)
                .fineAmount(BigDecimal.ZERO)
                .build();
        borrowing.addDetail(detail);

        when(borrowingRepository.findByIdWithDetails(601L)).thenReturn(Optional.of(borrowing));
        when(bookRepository.findByIdWithLock(10L)).thenReturn(Optional.of(book1));
        when(borrowingRepository.save(any(Borrowing.class))).thenAnswer(i -> i.getArgument(0));

        ReturnForm form = new ReturnForm();
        form.setBorrowingId(601L);
        form.setReturnDate(returnDate);
        ReturnItemForm returnItem = new ReturnItemForm();
        returnItem.setDetailId(1002L);
        returnItem.setBookId(10L);
        returnItem.setReturnQuantity(2);
        form.setItems(List.of(returnItem));

        Borrowing result = borrowingService.processReturn(form);

        // Expected fine: 5 days late * 2 books * 5000 = 50,000 VND
        BigDecimal expectedFine = new BigDecimal("50000");
        assertThat(result.getTotalFine()).isEqualByComparingTo(expectedFine);
        assertThat(result.getStatus()).isEqualTo(BorrowingStatus.RETURNED);
    }

    @Test
    @DisplayName("Chặn trả số lượng lớn hơn số lượng sách còn nợ")
    void testReturnBook_ExceedBorrowQuantity_Blocked() {
        Borrowing borrowing = Borrowing.builder()
                .id(602L)
                .member(activeMember)
                .borrowDate(LocalDate.now().minusDays(5))
                .dueDate(LocalDate.now().plusDays(9))
                .status(BorrowingStatus.BORROWING)
                .details(new ArrayList<>())
                .build();

        BorrowingDetail detail = BorrowingDetail.builder()
                .id(1003L)
                .borrowing(borrowing)
                .book(book1)
                .quantity(2)
                .returnedQuantity(1) // 1 copy remaining
                .fineAmount(BigDecimal.ZERO)
                .build();
        borrowing.addDetail(detail);

        when(borrowingRepository.findByIdWithDetails(602L)).thenReturn(Optional.of(borrowing));

        ReturnForm form = new ReturnForm();
        form.setBorrowingId(602L);
        form.setReturnDate(LocalDate.now());
        ReturnItemForm returnItem = new ReturnItemForm();
        returnItem.setDetailId(1003L);
        returnItem.setBookId(10L);
        returnItem.setReturnQuantity(2); // Try to return 2 while only 1 remaining!
        form.setItems(List.of(returnItem));

        assertThatThrownBy(() -> borrowingService.processReturn(form))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("vượt quá số lượng còn nợ");

        verify(borrowingRepository, never()).save(any());
    }
}
