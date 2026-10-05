package com.library.ledger.mapper;

import com.library.ledger.dto.CategoryForm;
import com.library.ledger.entity.Category;
import org.mapstruct.*;

@Mapper
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "books", ignore = true)
    @Mapping(target = "active", expression = "java(form.getActive() != null ? form.getActive() : true)")
    Category toEntity(CategoryForm form);

    CategoryForm toForm(Category category);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "books", ignore = true)
    void updateEntityFromForm(CategoryForm form, @MappingTarget Category category);

    @BeforeMapping
    default void trimCategoryForm(CategoryForm form) {
        if (form == null) return;
        if (form.getName() != null) form.setName(form.getName().trim());
        if (form.getDescription() != null) form.setDescription(form.getDescription().trim());
    }
}
