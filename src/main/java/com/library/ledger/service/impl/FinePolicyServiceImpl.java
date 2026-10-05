package com.library.ledger.service.impl;

import com.library.ledger.dto.FinePolicyForm;
import com.library.ledger.entity.FinePolicy;
import com.library.ledger.mapper.FinePolicyMapper;
import com.library.ledger.repository.FinePolicyRepository;
import com.library.ledger.service.FinePolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinePolicyServiceImpl implements FinePolicyService {

    private final FinePolicyRepository finePolicyRepository;
    private final FinePolicyMapper finePolicyMapper;

    @Override
    @Transactional(readOnly = true)
    public FinePolicy getActivePolicy() {
        return finePolicyRepository.findByActiveTrue()
                .orElseGet(() -> FinePolicy.builder()
                        .dailyFineAmount(new BigDecimal("5000"))
                        .maxFineAmount(null)
                        .graceDays(0)
                        .active(true)
                        .effectiveFrom(LocalDate.now())
                        .createdAt(LocalDateTime.now())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinePolicy> getAllPolicies() {
        return finePolicyRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional
    public FinePolicy updatePolicy(FinePolicyForm form) {
        log.info("Updating fine policy: dailyFineAmount={}, maxFineAmount={}, graceDays={}, effectiveFrom={}",
                form.getDailyFineAmount(), form.getMaxFineAmount(), form.getGraceDays(), form.getEffectiveFrom());

        // Deactivate all currently active policies
        List<FinePolicy> activePolicies = finePolicyRepository.findAll();
        for (FinePolicy p : activePolicies) {
            if (Boolean.TRUE.equals(p.getActive())) {
                p.setActive(false);
                finePolicyRepository.save(p);
            }
        }

        // Create new active policy via mapper
        FinePolicy newPolicy = finePolicyMapper.toEntity(form);
        return finePolicyRepository.save(newPolicy);
    }
}
