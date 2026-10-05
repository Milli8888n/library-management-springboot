package com.library.ledger.service;

import com.library.ledger.dto.MemberStatDto;
import com.library.ledger.dto.TopBookDto;
import com.library.ledger.entity.Borrowing;
import com.library.ledger.enums.MemberStatus;
import com.library.ledger.repository.BorrowingRepository;
import com.library.ledger.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private BorrowingRepository borrowingRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    @DisplayName("Lấy danh sách phiếu mượn quá hạn")
    void testGetOverdueBorrowings() {
        Borrowing overdue = Borrowing.builder()
                .id(1L)
                .dueDate(LocalDate.now().minusDays(3))
                .build();

        when(borrowingRepository.findOverdueBorrowings(any(LocalDate.class)))
                .thenReturn(List.of(overdue));

        List<Borrowing> list = reportService.getOverdueBorrowings();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo(1L);
        verify(borrowingRepository).findOverdueBorrowings(any(LocalDate.class));
    }

    @Test
    @DisplayName("Lấy danh sách Top 5 sách mượn nhiều nhất")
    void testGetTop5BorrowedBooks() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{1L, "978-604-0-12345-6", "Clean Code", "Robert C. Martin", "CNTT", 25L});

        when(borrowingRepository.findTopBorrowedBooks(PageRequest.of(0, 5)))
                .thenReturn(rows);

        List<TopBookDto> topBooks = reportService.getTop5BorrowedBooks();

        assertThat(topBooks).hasSize(1);
        TopBookDto dto = topBooks.get(0);
        assertThat(dto.getBookId()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("Clean Code");
        assertThat(dto.getTotalBorrowed()).isEqualTo(25L);
    }

    @Test
    @DisplayName("Lấy thống kê mượn sách theo thành viên")
    void testGetMemberBorrowingStats() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{10L, "Nguyễn Văn A", "a@library.vn", "ACTIVE", 5L, 2L, new BigDecimal("15000")});

        when(borrowingRepository.findBorrowingStatsByMember())
                .thenReturn(rows);

        List<MemberStatDto> stats = reportService.getMemberBorrowingStats();

        assertThat(stats).hasSize(1);
        MemberStatDto dto = stats.get(0);
        assertThat(dto.getMemberId()).isEqualTo(10L);
        assertThat(dto.getFullName()).isEqualTo("Nguyễn Văn A");
        assertThat(dto.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(dto.getTotalBorrowings()).isEqualTo(5L);
        assertThat(dto.getActiveBorrowings()).isEqualTo(2L);
        assertThat(dto.getTotalFine()).isEqualByComparingTo(new BigDecimal("15000"));
    }
}
