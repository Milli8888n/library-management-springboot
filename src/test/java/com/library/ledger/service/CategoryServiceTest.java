package com.library.ledger.service;

import com.library.ledger.dto.CategoryForm;
import com.library.ledger.entity.Category;
import com.library.ledger.exception.BusinessRuleException;
import com.library.ledger.exception.ResourceNotFoundException;
import com.library.ledger.repository.CategoryRepository;
import com.library.ledger.service.impl.CategoryServiceImpl;
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
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category activeCategory;

    @BeforeEach
    void setUp() {
        activeCategory = Category.builder()
                .id(1L)
                .name("Kinh tế")
                .description("Sách kinh tế")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Tạo thể loại thành công khi tên chưa tồn tại")
    void testCreateCategory_Success() {
        CategoryForm form = new CategoryForm();
        form.setName("Khoa học viễn tưởng");
        form.setDescription("Sách sci-fi");
        form.setActive(true);

        when(categoryRepository.existsByNameIgnoreCase("Khoa học viễn tưởng")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> {
            Category c = i.getArgument(0);
            c.setId(2L);
            return c;
        });

        Category created = categoryService.create(form);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(2L);
        assertThat(created.getName()).isEqualTo("Khoa học viễn tưởng");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Ném ngoại lệ khi tạo thể loại với tên trùng")
    void testCreateCategory_DuplicateName_ThrowsException() {
        CategoryForm form = new CategoryForm();
        form.setName("Kinh tế");

        when(categoryRepository.existsByNameIgnoreCase("Kinh tế")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(form))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đã tồn tại trong hệ thống");
    }

    @Test
    @DisplayName("Chặn ngừng hoạt động (xóa mềm) thể loại khi còn sách đang liên kết")
    void testDeleteCategory_HasActiveBooks_Blocked() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory));
        when(categoryRepository.countActiveBooksByCategoryId(1L)).thenReturn(5L);

        assertThatThrownBy(() -> categoryService.softDelete(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("vẫn còn 5 đầu sách đang liên kết");

        assertThat(activeCategory.getActive()).isTrue();
        verify(categoryRepository, never()).save(activeCategory);
    }

    @Test
    @DisplayName("Ngừng hoạt động thể loại thành công khi không còn sách nào liên kết")
    void testDeleteCategory_NoActiveBooks_Success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory));
        when(categoryRepository.countActiveBooksByCategoryId(1L)).thenReturn(0L);

        categoryService.softDelete(1L);

        assertThat(activeCategory.getActive()).isFalse();
        verify(categoryRepository).save(activeCategory);
    }

    @Test
    @DisplayName("Khôi phục thể loại thành công")
    void testRestoreCategory_Success() {
        activeCategory.setActive(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(activeCategory));

        categoryService.restore(1L);

        assertThat(activeCategory.getActive()).isTrue();
        verify(categoryRepository).save(activeCategory);
    }
}
