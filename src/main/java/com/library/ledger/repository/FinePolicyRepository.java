package com.library.ledger.repository;

import com.library.ledger.entity.FinePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FinePolicyRepository extends JpaRepository<FinePolicy, Long> {
    Optional<FinePolicy> findByActiveTrue();
}
