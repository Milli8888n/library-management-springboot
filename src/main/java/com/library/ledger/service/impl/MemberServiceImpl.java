package com.library.ledger.service.impl;

import com.library.ledger.dto.MemberForm;
import com.library.ledger.entity.Member;
import com.library.ledger.enums.MemberStatus;
import com.library.ledger.exception.BusinessRuleException;
import com.library.ledger.exception.ResourceNotFoundException;
import com.library.ledger.mapper.MemberMapper;
import com.library.ledger.repository.MemberRepository;
import com.library.ledger.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<Member> search(String keyword, MemberStatus status, Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        return memberRepository.searchMembers(cleanKeyword, status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Member> findActiveMembers() {
        return memberRepository.findByStatusOrderByFullNameAsc(MemberStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên với ID: " + id));
    }

    @Override
    public Member create(MemberForm form) {
        String cleanEmail = form.getEmail().trim().toLowerCase();
        if (memberRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new BusinessRuleException("Địa chỉ email '" + cleanEmail + "' đã được sử dụng");
        }

        // Normalise email before mapping
        form.setEmail(cleanEmail);
        Member member = memberMapper.toEntity(form);
        return memberRepository.save(member);
    }

    @Override
    public Member update(Long id, MemberForm form) {
        Member member = findById(id);
        String cleanEmail = form.getEmail().trim().toLowerCase();

        if (memberRepository.existsByEmailIgnoreCaseAndIdNot(cleanEmail, id)) {
            throw new BusinessRuleException("Địa chỉ email '" + cleanEmail + "' đã được sử dụng bởi thành viên khác");
        }

        // Normalise email before mapping
        form.setEmail(cleanEmail);
        memberMapper.updateEntityFromForm(form, member);
        return memberRepository.save(member);
    }

    @Override
    public void toggleStatus(Long id) {
        Member member = findById(id);
        if (member.getStatus() == MemberStatus.ACTIVE) {
            member.setStatus(MemberStatus.BLOCKED);
        } else {
            member.setStatus(MemberStatus.ACTIVE);
        }
        memberRepository.save(member);
    }
}
