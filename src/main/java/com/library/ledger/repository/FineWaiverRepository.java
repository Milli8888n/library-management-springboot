package com.library.ledger.repository;

import com.library.ledger.entity.FineWaiver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FineWaiverRepository extends JpaRepository<FineWaiver, Long> {
    List<FineWaiver> findByBorrowingIdOrderByApprovedDateAsc(Long borrowingId);
}
