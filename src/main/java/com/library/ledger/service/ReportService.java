package com.library.ledger.service;

import com.library.ledger.dto.MemberStatDto;
import com.library.ledger.dto.TopBookDto;
import com.library.ledger.entity.Borrowing;

import java.util.List;

public interface ReportService {
    List<Borrowing> getOverdueBorrowings();
    List<TopBookDto> getTop5BorrowedBooks();
    List<MemberStatDto> getMemberBorrowingStats();
}
