package com.library.ledger.service.impl;

import com.library.ledger.dto.CategoryForm;
import com.library.ledger.entity.Category;
import com.library.ledger.exception.BusinessRuleException;
import com.library.ledger.exception.ResourceNotFoundException;
import com.library.ledger.repository.CategoryRepository;
import com.library.ledger.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Category> findAll(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thể loại với ID: " + id));
    }

    @Override
    public Category create(CategoryForm form) {
        String trimmedName = form.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new BusinessRuleException("Tên thể loại '" + trimmedName + "' đã tồn tại trong hệ thống");
        }

        Category category = Category.builder()
                .name(trimmedName)
                .description(form.getDescription() != null ? form.getDescription().trim() : null)
                .active(form.getActive() != null ? form.getActive() : true)
                .build();

        return categoryRepository.save(category);
    }

    @Override
    public Category update(Long id, CategoryForm form) {
        Category category = findById(id);
        String trimmedName = form.getName().trim();

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new BusinessRuleException("Tên thể loại '" + trimmedName + "' đã được sử dụng bởi thể loại khác");
        }

        category.setName(trimmedName);
        category.setDescription(form.getDescription() != null ? form.getDescription().trim() : null);
        if (form.getActive() != null) {
            category.setActive(form.getActive());
        }

        return categoryRepository.save(category);
    }

    @Override
    public void softDelete(Long id) {
        Category category = findById(id);
        long activeBooks = categoryRepository.countActiveBooksByCategoryId(id);
        if (activeBooks > 0) {
            throw new BusinessRuleException("Không thể ngừng hoạt động thể loại '" + category.getName() +
                    "' vì vẫn còn " + activeBooks + " đầu sách đang liên kết.");
        }

        category.setActive(false);
        categoryRepository.save(category);
    }

    @Override
    public void restore(Long id) {
        Category category = findById(id);
        category.setActive(true);
        categoryRepository.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveBooks(Long categoryId) {
        return categoryRepository.countActiveBooksByCategoryId(categoryId);
    }
}
