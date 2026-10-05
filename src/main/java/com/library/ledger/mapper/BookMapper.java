package com.library.ledger.mapper;

import com.library.ledger.dto.BookForm;
import com.library.ledger.entity.Book;
import com.library.ledger.entity.Category;
import com.library.ledger.enums.BookStatus;
import org.mapstruct.*;

@Mapper
public interface BookMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "availableQuantity", source = "availableQuantity")
    @Mapping(target = "status", expression = "java(form.getStatus() != null ? form.getStatus() : com.library.ledger.enums.BookStatus.ACTIVE)")
    Book toEntity(BookForm form, Category category, int availableQuantity);

    @Mapping(target = "categoryId", source = "category.id")
    BookForm toForm(Book book);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "availableQuantity", source = "newAvailableQuantity")
    void updateEntityFromForm(BookForm form, Category category, int newAvailableQuantity,
                              @MappingTarget Book book);

    @BeforeMapping
    default void trimBookForm(BookForm form) {
        if (form == null) return;
        if (form.getIsbn() != null) form.setIsbn(form.getIsbn().trim());
        if (form.getTitle() != null) form.setTitle(form.getTitle().trim());
        if (form.getAuthor() != null) form.setAuthor(form.getAuthor().trim());
    }
}
