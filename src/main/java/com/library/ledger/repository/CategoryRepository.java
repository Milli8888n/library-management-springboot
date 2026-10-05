package com.library.ledger.repository;

import com.library.ledger.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND c.id != :id")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") Long id);

    List<Category> findByActiveTrueOrderByNameAsc();

    Page<Category> findAll(Pageable pageable);

    @Query("SELECT c FROM Category c WHERE " +
           "(:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:active IS NULL OR c.active = :active)")
    Page<Category> search(@Param("keyword") String keyword, @Param("active") Boolean active, Pageable pageable);

    long countByActiveTrue();

    long countByActiveFalse();

    @Query("SELECT COUNT(b) FROM Book b WHERE b.category.id = :categoryId AND b.status != 'DELETED'")
    long countActiveBooksByCategoryId(@Param("categoryId") Long categoryId);
}
