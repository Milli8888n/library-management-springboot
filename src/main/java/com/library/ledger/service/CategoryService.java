package com.library.ledger.service;

import com.library.ledger.dto.CategoryForm;
import com.library.ledger.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    Page<Category> findAll(Pageable pageable);
    Page<Category> search(String keyword, Boolean active, Pageable pageable);
    long countTotal();
    long countActive();
    long countInactive();
    List<Category> findActiveCategories();
    Category findById(Long id);
    Category create(CategoryForm form);
    Category update(Long id, CategoryForm form);
    void softDelete(Long id);
    void restore(Long id);
    long countActiveBooks(Long categoryId);
}
