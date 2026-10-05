package com.library.ledger.service;

import com.library.ledger.dto.MemberForm;
import com.library.ledger.entity.Member;
import com.library.ledger.enums.MemberStatus;
import com.library.ledger.exception.BusinessRuleException;
import com.library.ledger.repository.MemberRepository;
import com.library.ledger.service.impl.MemberServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberServiceImpl memberService;

    private Member sampleMember;

    @BeforeEach
    void setUp() {
        sampleMember = Member.builder()
                .id(1L)
                .fullName("Lê Văn Cường")
                .email("cuong.le@library.vn")
                .phone("0912345678")
                .status(MemberStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Tạo thành viên thành công khi email chưa tồn tại")
    void testCreateMember_Success() {
        MemberForm form = new MemberForm();
        form.setFullName("Lê Văn Cường");
        form.setEmail("cuong.le@library.vn");
        form.setPhone("0912345678");

        when(memberRepository.existsByEmailIgnoreCase("cuong.le@library.vn")).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenAnswer(i -> {
            Member m = i.getArgument(0);
            m.setId(10L);
            return m;
        });

        Member created = memberService.create(form);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(10L);
        assertThat(created.getEmail()).isEqualTo("cuong.le@library.vn");
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("Ném ngoại lệ khi tạo thành viên với email đã tồn tại")
    void testCreateMember_DuplicateEmail_ThrowsException() {
        MemberForm form = new MemberForm();
        form.setFullName("Nguyễn Văn A");
        form.setEmail("cuong.le@library.vn");

        when(memberRepository.existsByEmailIgnoreCase("cuong.le@library.vn")).thenReturn(true);

        assertThatThrownBy(() -> memberService.create(form))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đã được sử dụng");

        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("Chuyển đổi trạng thái khóa/mở khóa thành viên")
    void testToggleStatus_Success() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(sampleMember));

        // ACTIVE -> BLOCKED
        memberService.toggleStatus(1L);
        assertThat(sampleMember.getStatus()).isEqualTo(MemberStatus.BLOCKED);

        // BLOCKED -> ACTIVE
        memberService.toggleStatus(1L);
        assertThat(sampleMember.getStatus()).isEqualTo(MemberStatus.ACTIVE);

        verify(memberRepository, times(2)).save(sampleMember);
    }
}
