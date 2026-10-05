package com.library.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TopBookDto {

    private Long bookId;
    private String isbn;
    private String title;
    private String author;
    private String categoryName;
    private Long totalBorrowed;
}
