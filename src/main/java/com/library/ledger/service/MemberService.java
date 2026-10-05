package com.library.ledger.service;

import com.library.ledger.dto.MemberForm;
import com.library.ledger.entity.Member;
import com.library.ledger.enums.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MemberService {
    Page<Member> search(String keyword, MemberStatus status, Pageable pageable);
    List<Member> findActiveMembers();
    Member findById(Long id);
    Member create(MemberForm form);
    Member update(Long id, MemberForm form);
    void toggleStatus(Long id);
}
