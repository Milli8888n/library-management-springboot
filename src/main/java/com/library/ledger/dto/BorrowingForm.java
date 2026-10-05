package com.library.ledger.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class BorrowingForm {

    @NotNull(message = "Vui lòng chọn thành viên mượn sách")
    private Long memberId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate borrowDate = LocalDate.now();

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate = LocalDate.now().plusDays(14);

    @NotEmpty(message = "Phiếu mượn phải có ít nhất 1 cuốn sách")
    @Valid
    private List<BorrowingItemForm> items = new ArrayList<>();
}
