package com.library.ledger.service.impl;

import com.library.ledger.dto.BookForm;
import com.library.ledger.entity.Book;
import com.library.ledger.entity.Category;
import com.library.ledger.enums.BookStatus;
import com.library.ledger.exception.BusinessRuleException;
import com.library.ledger.exception.ResourceNotFoundException;
import com.library.ledger.repository.BookRepository;
import com.library.ledger.repository.CategoryRepository;
import com.library.ledger.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final com.library.ledger.mapper.BookMapper bookMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<Book> search(String keyword, Long categoryId, BookStatus status, Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return bookRepository.searchBooks(cleanKeyword, categoryId, status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Book> findAvailableBooks() {
        return bookRepository.findByStatusAndAvailableQuantityGreaterThan(BookStatus.ACTIVE, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public Book findById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sách với ID: " + id));
    }

    @Override
    public Book create(BookForm form) {
        String cleanIsbn = form.getIsbn().trim();
        if (bookRepository.existsByIsbn(cleanIsbn)) {
            throw new BusinessRuleException("Mã ISBN '" + cleanIsbn + "' đã tồn tại trong hệ thống");
        }

        Category category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thể loại đã chọn"));

        if (!category.getActive()) {
            throw new BusinessRuleException("Không thể gán sách vào thể loại đang ngừng hoạt động");
        }

        int total = form.getTotalQuantity();
        int available = form.getAvailableQuantity() != null ? form.getAvailableQuantity() : total;

        if (available > total || available < 0) {
            throw new BusinessRuleException("Số lượng khả dụng không được lớn hơn tổng số lượng hoặc nhỏ hơn 0");
        }

        Book book = bookMapper.toEntity(form, category, available);
        return bookRepository.save(book);
    }

    @Override
    public Book update(Long id, BookForm form) {
        Book book = findById(id);
        String cleanIsbn = form.getIsbn().trim();

        if (bookRepository.existsByIsbnAndIdNot(cleanIsbn, id)) {
            throw new BusinessRuleException("Mã ISBN '" + cleanIsbn + "' đã được sử dụng bởi cuốn sách khác");
        }

        Category category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thể loại đã chọn"));

        int currentlyBorrowed = book.getTotalQuantity() - book.getAvailableQuantity();
        if (form.getTotalQuantity() < currentlyBorrowed) {
            throw new BusinessRuleException("Tổng số lượng mới (" + form.getTotalQuantity() +
                    ") không được nhỏ hơn số lượng đang được mượn (" + currentlyBorrowed + ")");
        }

        int newAvailable = form.getTotalQuantity() - currentlyBorrowed;
        bookMapper.updateEntityFromForm(form, category, newAvailable, book);

        return bookRepository.save(book);
    }

    @Override
    public void softDelete(Long id) {
        Book book = findById(id);
        int currentlyBorrowed = book.getTotalQuantity() - book.getAvailableQuantity();
        if (currentlyBorrowed > 0) {
            throw new BusinessRuleException("Không thể xóa sách '" + book.getTitle() +
                    "' vì đang có " + currentlyBorrowed + " cuốn chưa được trả");
        }

        book.setStatus(BookStatus.DELETED);
        bookRepository.save(book);
    }
}
