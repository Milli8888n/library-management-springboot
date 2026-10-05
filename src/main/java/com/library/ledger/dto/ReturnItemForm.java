package com.library.ledger.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReturnItemForm {

    private Long detailId;
    private Long bookId;
    private String bookTitle;
    private String isbn;
    private Integer quantity;
    private Integer remainingQuantity;
    private Integer returnQuantity = 0;
}
