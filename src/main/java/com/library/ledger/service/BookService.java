package com.library.ledger.service;

import com.library.ledger.dto.BookForm;
import com.library.ledger.entity.Book;
import com.library.ledger.enums.BookStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BookService {
    Page<Book> search(String keyword, Long categoryId, BookStatus status, Pageable pageable);
    List<Book> findAvailableBooks();
    Book findById(Long id);
    Book create(BookForm form);
    Book update(Long id, BookForm form);
    void softDelete(Long id);
}
