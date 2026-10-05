package com.library.ledger.service.impl;

import com.library.ledger.dto.MemberStatDto;
import com.library.ledger.dto.TopBookDto;
import com.library.ledger.entity.Borrowing;
import com.library.ledger.enums.MemberStatus;
import com.library.ledger.repository.BorrowingRepository;
import com.library.ledger.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final BorrowingRepository borrowingRepository;

    @Override
    public List<Borrowing> getOverdueBorrowings() {
        return borrowingRepository.findOverdueBorrowings(LocalDate.now());
    }

    @Override
    public List<TopBookDto> getTop5BorrowedBooks() {
        List<Object[]> rows = borrowingRepository.findTopBorrowedBooks(PageRequest.of(0, 5));
        return rows.stream().map(row -> new TopBookDto(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                ((Number) row[5]).longValue()
        )).collect(Collectors.toList());
    }

    @Override
    public List<MemberStatDto> getMemberBorrowingStats() {
        List<Object[]> rows = borrowingRepository.findBorrowingStatsByMember();
        return rows.stream().map(row -> {
            MemberStatus status;
            if (row[3] instanceof MemberStatus ms) {
                status = ms;
            } else if (row[3] != null) {
                status = MemberStatus.valueOf(row[3].toString());
            } else {
                status = MemberStatus.ACTIVE;
            }
            return new MemberStatDto(
                ((Number) row[0]).longValue(),
                row[1] != null ? row[1].toString() : "",
                row[2] != null ? row[2].toString() : "",
                status,
                row[4] != null ? ((Number) row[4]).longValue() : 0L,
                row[5] != null ? ((Number) row[5]).longValue() : 0L,
                row[6] != null ? new BigDecimal(row[6].toString()) : BigDecimal.ZERO
            );
        }).collect(Collectors.toList());
    }
}
