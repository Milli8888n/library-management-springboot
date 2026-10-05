package com.library.ledger.service;

import com.library.ledger.dto.BookForm;
import com.library.ledger.entity.Book;
import com.library.ledger.entity.Category;
import com.library.ledger.enums.BookStatus;
import com.library.ledger.exception.BusinessRuleException;
import com.library.ledger.mapper.BookMapper;
import com.library.ledger.repository.BookRepository;
import com.library.ledger.repository.CategoryRepository;
import com.library.ledger.service.impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookServiceImpl bookService;

    private Category activeCategory;
    private Category inactiveCategory;
    private Book sampleBook;

    @BeforeEach
    void setUp() {
        activeCategory = Category.builder()
                .id(1L)
                .name("Công nghệ thông tin")
                .description("Sách CNTT")
                .active(true)
                .build();

        inactiveCategory = Category.builder()
                .id(2L)
                .name("Thể loại cũ")
                .active(false)
                .build();

        sampleBook = Book.builder()
                .id(100L)
                .isbn("978-604-0-12345-6")
                .title("Clean Architecture")
                .author("Robert C. Martin")
                .category(activeCategory)
                .totalQuantity(10)
                .availableQuantity(8)
                .status(BookStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Tạo sách thành công khi dữ liệu hợp lệ")
    void testCreateBook_Success() {
        BookForm form = new BookForm();
        form.setIsbn("978-604-0-12345-6");
        form.setTitle("Clean Architecture");
        form.setAuthor("Robert C. Martin");
        form.setCategoryId(1L);
        form.setTotalQuantity(10);
        form.setAvailableQuantity(10);
        form.setStatus(BookStatus.ACTIVE);

        Book mappedBook = Book.builder()
                .isbn("978-604-0-12345-6")
                .title("Clean Architecture")
                .author("Robert C. Martin")
                .category(activeCategory)
                .totalQuantity(10)
                .availableQuantity(10)
                .status(BookStatus.ACTIVE)
                .build();

        when(bookRepository.existsByIsbn("978-604-0-12345-6")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory));
        when(bookMapper.toEntity(eq(form), eq(activeCategory), eq(10))).thenReturn(mappedBook);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book b = invocation.getArgument(0);
            b.setId(101L);
            return b;
        });

        Book created = bookService.create(form);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(101L);
        assertThat(created.getTitle()).isEqualTo("Clean Architecture");
        assertThat(created.getAvailableQuantity()).isEqualTo(10);
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    @DisplayName("Ném ngoại lệ khi tạo sách với ISBN đã tồn tại")
    void testCreateBook_DuplicateIsbn_ThrowsException() {
        BookForm form = new BookForm();
        form.setIsbn("978-604-0-12345-6");
        form.setTitle("Clean Code");
        form.setAuthor("Robert C. Martin");
        form.setCategoryId(1L);
        form.setTotalQuantity(5);

        when(bookRepository.existsByIsbn("978-604-0-12345-6")).thenReturn(true);

        assertThatThrownBy(() -> bookService.create(form))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đã tồn tại trong hệ thống");

        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    @DisplayName("Ném ngoại lệ khi gán sách vào thể loại đang ngừng hoạt động")
    void testCreateBook_InactiveCategory_ThrowsException() {
        BookForm form = new BookForm();
        form.setIsbn("978-1-23456-789-0");
        form.setTitle("Test Book");
        form.setAuthor("Author");
        form.setCategoryId(2L);
        form.setTotalQuantity(5);

        when(bookRepository.existsByIsbn("978-1-23456-789-0")).thenReturn(false);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(inactiveCategory));

        assertThatThrownBy(() -> bookService.create(form))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("thể loại đang ngừng hoạt động");
    }

    @Test
    @DisplayName("Xóa mềm sách thành công khi không còn sách nào đang mượn")
    void testSoftDeleteBook_Success() {
        sampleBook.setTotalQuantity(10);
        sampleBook.setAvailableQuantity(10); // currently borrowed = 0
        when(bookRepository.findById(100L)).thenReturn(Optional.of(sampleBook));

        bookService.softDelete(100L);

        assertThat(sampleBook.getStatus()).isEqualTo(BookStatus.DELETED);
        verify(bookRepository).save(sampleBook);
    }

    @Test
    @DisplayName("Chặn xóa mềm sách khi vẫn còn sách đang được độc giả mượn")
    void testSoftDeleteBook_HasBorrowedCopies_ThrowsException() {
        sampleBook.setTotalQuantity(10);
        sampleBook.setAvailableQuantity(8); // 2 copies currently borrowed
        when(bookRepository.findById(100L)).thenReturn(Optional.of(sampleBook));

        assertThatThrownBy(() -> bookService.softDelete(100L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("chưa được trả");

        verify(bookRepository, never()).save(any(Book.class));
    }
}
