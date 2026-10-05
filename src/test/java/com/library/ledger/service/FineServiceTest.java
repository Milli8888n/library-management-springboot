package com.library.ledger.service;

import com.library.ledger.dto.FinePaymentForm;
import com.library.ledger.dto.FineWaiverForm;
import com.library.ledger.entity.Borrowing;
import com.library.ledger.entity.FinePayment;
import com.library.ledger.entity.FineWaiver;
import com.library.ledger.enums.BorrowingStatus;
import com.library.ledger.enums.FineStatus;
import com.library.ledger.exception.BusinessRuleException;
import com.library.ledger.exception.ResourceNotFoundException;
import com.library.ledger.repository.BorrowingRepository;
import com.library.ledger.repository.FinePaymentRepository;
import com.library.ledger.repository.FineWaiverRepository;
import com.library.ledger.service.impl.FineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UC-22 (Ghi nhận thanh toán) & UC-23 (Miễn/giảm) test cases per SRS v1.2 §12.7 (TC-13 – TC-22)
 */
@ExtendWith(MockitoExtension.class)
class FineServiceTest {

    @Mock private BorrowingRepository borrowingRepository;
    @Mock private FinePaymentRepository finePaymentRepository;
    @Mock private FineWaiverRepository fineWaiverRepository;

    @InjectMocks private FineServiceImpl fineService;

    private Borrowing borrowingWithFine;

    @BeforeEach
    void setUp() {
        borrowingWithFine = Borrowing.builder()
                .id(100L)
                .status(BorrowingStatus.RETURNED)
                .totalFineAmount(new BigDecimal("100000"))
                .paidFineAmount(BigDecimal.ZERO)
                .waivedFineAmount(BigDecimal.ZERO)
                .unpaidFineAmount(new BigDecimal("100000"))
                .fineStatus(FineStatus.FINE_PENDING)
                .totalFine(new BigDecimal("100000"))
                .details(new ArrayList<>())
                .payments(new ArrayList<>())
                .waivers(new ArrayList<>())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-13 (UC-22): Thanh toán một phần → paidFineAmount tăng, unpaidFineAmount giảm
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-13 UC-22: Thanh toán một phần → paidFineAmount tăng, still FINE_PENDING")
    void tc13_partialPayment_debtDecreases() {
        FinePaymentForm form = new FinePaymentForm();
        form.setAmount(new BigDecimal("40000"));
        form.setMethod("CASH");

        when(borrowingRepository.findById(100L)).thenReturn(Optional.of(borrowingWithFine));
        FinePayment saved = FinePayment.builder().id(1L)
                .amount(new BigDecimal("40000"))
                .borrowing(borrowingWithFine)
                .build();
        when(finePaymentRepository.save(any())).thenReturn(saved);
        when(borrowingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        fineService.recordPayment(100L, form);

        assertThat(borrowingWithFine.getPaidFineAmount()).isEqualByComparingTo("40000");
        assertThat(borrowingWithFine.getUnpaidFineAmount()).isEqualByComparingTo("60000");
        assertThat(borrowingWithFine.getFineStatus()).isEqualTo(FineStatus.FINE_PENDING);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-14 (UC-22): Thanh toán toàn bộ → fineStatus = FINE_PAID, unpaid = 0
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-14 UC-22: Thanh toán toàn bộ → FINE_PAID, unpaid = 0")
    void tc14_fullPayment_statusBecomesFinePaid() {
        FinePaymentForm form = new FinePaymentForm();
        form.setAmount(new BigDecimal("100000"));
        form.setMethod("TRANSFER");

        when(borrowingRepository.findById(100L)).thenReturn(Optional.of(borrowingWithFine));
        when(finePaymentRepository.save(any())).thenAnswer(i -> {
            FinePayment p = i.getArgument(0);
            p.setId(2L);
            return p;
        });
        when(borrowingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        fineService.recordPayment(100L, form);

        assertThat(borrowingWithFine.getUnpaidFineAmount()).isEqualByComparingTo("0");
        assertThat(borrowingWithFine.getFineStatus()).isEqualTo(FineStatus.FINE_PAID);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-15 (UC-22): Thanh toán vượt quá số nợ → BusinessRuleException
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-15 UC-22: Thanh toán vượt quá số nợ → BusinessRuleException")
    void tc15_overpayment_throwsException() {
        FinePaymentForm form = new FinePaymentForm();
        form.setAmount(new BigDecimal("200000")); // > 100,000 unpaid
        form.setMethod("CASH");

        when(borrowingRepository.findById(100L)).thenReturn(Optional.of(borrowingWithFine));

        assertThatThrownBy(() -> fineService.recordPayment(100L, form))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("vượt quá");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-16 (UC-22): Phiếu mượn không có phạt → BusinessRuleException
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-16 UC-22: Phiếu mượn không có phạt → BusinessRuleException")
    void tc16_noPendingFine_throwsException() {
        borrowingWithFine.setUnpaidFineAmount(BigDecimal.ZERO);
        FinePaymentForm form = new FinePaymentForm();
        form.setAmount(new BigDecimal("10000"));
        form.setMethod("CASH");

        when(borrowingRepository.findById(100L)).thenReturn(Optional.of(borrowingWithFine));

        assertThatThrownBy(() -> fineService.recordPayment(100L, form))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không có tiền phạt");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-17 (UC-22): Phiếu mượn không tồn tại → ResourceNotFoundException
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-17 UC-22: Phiếu mượn không tồn tại → ResourceNotFoundException")
    void tc17_borrowingNotFound_throwsException() {
        when(borrowingRepository.findById(999L)).thenReturn(Optional.empty());

        FinePaymentForm form = new FinePaymentForm();
        form.setAmount(new BigDecimal("10000"));
        form.setMethod("CASH");

        assertThatThrownBy(() -> fineService.recordPayment(999L, form))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-18 (UC-23): Miễn một phần → waivedFineAmount tăng, unpaid giảm, FINE_PENDING
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-18 UC-23: Miễn một phần → waivedFineAmount tăng, still FINE_PENDING")
    void tc18_partialWaiver() {
        FineWaiverForm form = new FineWaiverForm();
        form.setAmount(new BigDecimal("30000"));
        form.setReason("Độc giả gặp hoàn cảnh khó khăn");
        form.setApprovedBy("Nguyễn Thư Viện Trưởng");

        when(borrowingRepository.findById(100L)).thenReturn(Optional.of(borrowingWithFine));
        when(fineWaiverRepository.save(any())).thenAnswer(i -> {
            FineWaiver w = i.getArgument(0);
            w.setId(1L);
            return w;
        });
        when(borrowingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        fineService.recordWaiver(100L, form);

        assertThat(borrowingWithFine.getWaivedFineAmount()).isEqualByComparingTo("30000");
        assertThat(borrowingWithFine.getUnpaidFineAmount()).isEqualByComparingTo("70000");
        assertThat(borrowingWithFine.getFineStatus()).isEqualTo(FineStatus.FINE_PENDING);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-19 (UC-23): Miễn toàn bộ → fineStatus = FINE_PAID
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-19 UC-23: Miễn toàn bộ → FINE_PAID, unpaid = 0")
    void tc19_fullWaiver_statusBecomesFinePaid() {
        FineWaiverForm form = new FineWaiverForm();
        form.setAmount(new BigDecimal("100000"));
        form.setReason("Miễn toàn bộ theo quyết định ban giám đốc");
        form.setApprovedBy("Giám đốc");

        when(borrowingRepository.findById(100L)).thenReturn(Optional.of(borrowingWithFine));
        when(fineWaiverRepository.save(any())).thenAnswer(i -> {
            FineWaiver w = i.getArgument(0);
            w.setId(2L);
            return w;
        });
        when(borrowingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        fineService.recordWaiver(100L, form);

        assertThat(borrowingWithFine.getUnpaidFineAmount()).isEqualByComparingTo("0");
        assertThat(borrowingWithFine.getFineStatus()).isEqualTo(FineStatus.FINE_PAID);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-20 (UC-23): Miễn vượt quá số nợ → BusinessRuleException
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-20 UC-23: Miễn vượt quá số nợ → BusinessRuleException")
    void tc20_waiverExceedsDebt_throwsException() {
        FineWaiverForm form = new FineWaiverForm();
        form.setAmount(new BigDecimal("150000")); // > 100,000 unpaid
        form.setReason("Test");
        form.setApprovedBy("Admin");

        when(borrowingRepository.findById(100L)).thenReturn(Optional.of(borrowingWithFine));

        assertThatThrownBy(() -> fineService.recordWaiver(100L, form))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("vượt quá");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-21 (UC-23): Miễn thiếu lý do → BusinessRuleException
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-21 UC-23: Thiếu lý do miễn/giảm → BusinessRuleException")
    void tc21_missingReason_throwsException() {
        FineWaiverForm form = new FineWaiverForm();
        form.setAmount(new BigDecimal("10000"));
        form.setReason("   "); // blank
        form.setApprovedBy("Admin");

        when(borrowingRepository.findById(100L)).thenReturn(Optional.of(borrowingWithFine));

        assertThatThrownBy(() -> fineService.recordWaiver(100L, form))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Lý do");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-22: Rollback — nếu finePaymentRepository.save() ném exception,
    //         borrowing không được persist (transactional rollback simulated)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-22: finePaymentRepository.save() lỗi → borrowingRepository.save() KHÔNG được gọi")
    void tc22_rollback_ifPaymentSaveFails() {
        FinePaymentForm form = new FinePaymentForm();
        form.setAmount(new BigDecimal("50000"));
        form.setMethod("CASH");

        when(borrowingRepository.findById(100L)).thenReturn(Optional.of(borrowingWithFine));
        when(finePaymentRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> fineService.recordPayment(100L, form))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB error");

        // borrowingRepository.save must NOT have been called
        verify(borrowingRepository, never()).save(any());
    }
}
