package com.library.ledger.mapper;

import com.library.ledger.dto.BorrowingForm;
import com.library.ledger.entity.Book;
import com.library.ledger.entity.Borrowing;
import com.library.ledger.entity.BorrowingDetail;
import com.library.ledger.entity.Member;
import com.library.ledger.enums.BorrowingStatus;
import org.mapstruct.*;

import java.time.LocalDate;

@Mapper
public interface BorrowingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "returnedDate", ignore = true)
    @Mapping(target = "totalFine", ignore = true)
    @Mapping(target = "totalFineAmount", ignore = true)
    @Mapping(target = "paidFineAmount", ignore = true)
    @Mapping(target = "waivedFineAmount", ignore = true)
    @Mapping(target = "unpaidFineAmount", ignore = true)
    @Mapping(target = "fineStatus", ignore = true)
    @Mapping(target = "payments", ignore = true)
    @Mapping(target = "waivers", ignore = true)
    @Mapping(target = "member", source = "member")
    @Mapping(target = "dueDate", source = "dueDate")
    @Mapping(target = "borrowDate", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "status", expression = "java(com.library.ledger.enums.BorrowingStatus.BORROWING)")
    @Mapping(target = "details", expression = "java(new java.util.ArrayList<>())")
    Borrowing toEntity(BorrowingForm form, Member member, LocalDate dueDate);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "returnedQuantity", constant = "0")
    @Mapping(target = "fineAmount", ignore = true)
    @Mapping(target = "lateDays", ignore = true)
    @Mapping(target = "dailyFineSnapshot", ignore = true)
    @Mapping(target = "graceDaysSnapshot", ignore = true)
    @Mapping(target = "borrowing", source = "borrowing")
    @Mapping(target = "book", source = "book")
    @Mapping(target = "quantity", source = "quantity")
    BorrowingDetail toDetailEntity(Borrowing borrowing, Book book, int quantity);
}
