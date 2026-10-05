package com.library.ledger.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ReturnForm {

    private Long borrowingId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate returnDate = LocalDate.now();

    private List<ReturnItemForm> items = new ArrayList<>();
}
