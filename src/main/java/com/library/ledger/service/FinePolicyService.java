package com.library.ledger.service;

import com.library.ledger.dto.FinePolicyForm;
import com.library.ledger.entity.FinePolicy;

import java.util.List;

public interface FinePolicyService {
    FinePolicy getActivePolicy();
    List<FinePolicy> getAllPolicies();
    FinePolicy updatePolicy(FinePolicyForm form);
}
