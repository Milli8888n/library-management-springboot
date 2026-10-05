package com.library.ledger.repository;

import com.library.ledger.entity.Borrowing;
import com.library.ledger.enums.BorrowingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowingRepository extends JpaRepository<Borrowing, Long> {

    @Query("SELECT DISTINCT b FROM Borrowing b " +
           "LEFT JOIN FETCH b.member " +
           "LEFT JOIN FETCH b.details d " +
           "LEFT JOIN FETCH d.book " +
           "WHERE b.id = :id")
    Optional<Borrowing> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT b FROM Borrowing b WHERE " +
           "(:status IS NULL OR b.status = :status) AND " +
           "(:memberId IS NULL OR b.member.id = :memberId)")
    Page<Borrowing> searchBorrowings(
        @Param("status") BorrowingStatus status,
        @Param("memberId") Long memberId,
        Pageable pageable
    );

    @Query("SELECT b FROM Borrowing b WHERE " +
           "b.status != 'RETURNED' AND b.dueDate < :currentDate " +
           "ORDER BY b.dueDate ASC")
    List<Borrowing> findOverdueBorrowings(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT b FROM Borrowing b WHERE " +
           "b.status = 'BORROWING' AND b.dueDate < :currentDate")
    List<Borrowing> findPendingOverdue(@Param("currentDate") LocalDate currentDate);

    // SQL-02 (UC-18): Top 5 sách mượn nhiều nhất
    @Query("SELECT bd.book.id, bd.book.isbn, bd.book.title, bd.book.author, " +
           "c.name, SUM(bd.quantity) " +
           "FROM BorrowingDetail bd " +
           "JOIN bd.book b " +
           "JOIN b.category c " +
           "GROUP BY bd.book.id, bd.book.isbn, bd.book.title, bd.book.author, c.name " +
           "ORDER BY SUM(bd.quantity) DESC")
    List<Object[]> findTopBorrowedBooks(Pageable pageable);

    // SQL-03 (UC-19): Lượt mượn theo thành viên
    @Query("SELECT m.id, m.fullName, m.email, m.status, COUNT(b.id), " +
           "SUM(CASE WHEN b.status = 'BORROWING' OR b.status = 'OVERDUE' THEN 1 ELSE 0 END), " +
           "SUM(b.totalFine) " +
           "FROM Member m " +
           "LEFT JOIN Borrowing b ON b.member.id = m.id " +
           "GROUP BY m.id, m.fullName, m.email, m.status " +
           "ORDER BY COUNT(b.id) DESC")
    List<Object[]> findBorrowingStatsByMember();

    /** §12.5 — check if member has any unpaid fine across all borrowings */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN TRUE ELSE FALSE END " +
           "FROM Borrowing b " +
           "WHERE b.member.id = :memberId AND b.unpaidFineAmount > 0")
    boolean hasAnyUnpaidFine(@Param("memberId") Long memberId);

    /** §12.5 — check if member has any overdue borrowing not fully returned */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN TRUE ELSE FALSE END " +
           "FROM Borrowing b " +
           "WHERE b.member.id = :memberId AND b.status = 'OVERDUE'")
    boolean hasAnyOverdueUnreturned(@Param("memberId") Long memberId);

    /** §12.6 — count total books currently borrowed (not yet returned) by member */
    @Query("SELECT COALESCE(SUM(bd.quantity - bd.returnedQuantity), 0) " +
           "FROM BorrowingDetail bd " +
           "WHERE bd.borrowing.member.id = :memberId " +
           "AND bd.borrowing.status != 'RETURNED'")
    Long countCurrentlyBorrowedBooks(@Param("memberId") Long memberId);
}
