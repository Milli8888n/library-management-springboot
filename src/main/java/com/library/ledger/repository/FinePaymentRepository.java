package com.library.ledger.repository;

import com.library.ledger.entity.FinePayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinePaymentRepository extends JpaRepository<FinePayment, Long> {
    List<FinePayment> findByBorrowingIdOrderByPaymentDateAsc(Long borrowingId);
}
