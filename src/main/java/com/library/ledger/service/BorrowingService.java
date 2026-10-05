package com.library.ledger.service;

import com.library.ledger.dto.BorrowingForm;
import com.library.ledger.dto.ReturnForm;
import com.library.ledger.entity.Borrowing;
import com.library.ledger.enums.BorrowingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface BorrowingService {
    Page<Borrowing> search(BorrowingStatus status, Long memberId, Pageable pageable);
    Borrowing findById(Long id);
    Borrowing create(BorrowingForm form);
    Borrowing processReturn(ReturnForm form);
    Borrowing returnAll(Long borrowingId, LocalDate returnDate);
    BigDecimal calculateEstimatedFine(Borrowing borrowing, LocalDate asOfDate);
    void checkAndUpdateOverdueBorrowings();
}
