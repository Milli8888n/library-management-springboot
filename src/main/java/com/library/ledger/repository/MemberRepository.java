package com.library.ledger.repository;

import com.library.ledger.entity.Member;
import com.library.ledger.enums.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN TRUE ELSE FALSE END FROM Member m WHERE LOWER(m.email) = LOWER(:email) AND m.id != :id")
    boolean existsByEmailIgnoreCaseAndIdNot(@Param("email") String email, @Param("id") Long id);

    @Query("SELECT m FROM Member m WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(m.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(m.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:status IS NULL OR m.status = :status)")
    Page<Member> searchMembers(
        @Param("keyword") String keyword,
        @Param("status") MemberStatus status,
        Pageable pageable
    );

    List<Member> findByStatusOrderByFullNameAsc(MemberStatus status);
}
