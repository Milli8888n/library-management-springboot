package com.library.ledger.repository;

import com.library.ledger.entity.BorrowingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BorrowingDetailRepository extends JpaRepository<BorrowingDetail, Long> {
    List<BorrowingDetail> findByBorrowingId(Long borrowingId);
}
