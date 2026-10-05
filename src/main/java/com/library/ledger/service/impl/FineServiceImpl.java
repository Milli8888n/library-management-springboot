package com.library.ledger.service.impl;

import com.library.ledger.dto.FinePaymentForm;
import com.library.ledger.dto.FineWaiverForm;
import com.library.ledger.entity.Borrowing;
import com.library.ledger.entity.FinePayment;
import com.library.ledger.entity.FineWaiver;
import com.library.ledger.exception.BusinessRuleException;
import com.library.ledger.exception.ResourceNotFoundException;
import com.library.ledger.repository.BorrowingRepository;
import com.library.ledger.repository.FinePaymentRepository;
import com.library.ledger.repository.FineWaiverRepository;
import com.library.ledger.service.FineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FineServiceImpl implements FineService {

    private final BorrowingRepository borrowingRepository;
    private final FinePaymentRepository finePaymentRepository;
    private final FineWaiverRepository fineWaiverRepository;

    @Override
    @Transactional
    public FinePayment recordPayment(Long borrowingId, FinePaymentForm form) {
        Borrowing borrowing = borrowingRepository.findById(borrowingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu mượn với ID: " + borrowingId));

        BigDecimal unpaid = borrowing.getUnpaidFineAmount() != null
                ? borrowing.getUnpaidFineAmount()
                : BigDecimal.ZERO;

        if (unpaid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Phiếu mượn không có tiền phạt cần thanh toán.");
        }

        BigDecimal paymentAmount = form.getAmount();
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Số tiền thanh toán phải lớn hơn 0.");
        }

        if (paymentAmount.compareTo(unpaid) > 0) {
            throw new BusinessRuleException(String.format(
                    "Số tiền thanh toán (%s) vượt quá số tiền phạt còn nợ (%s).",
                    paymentAmount, unpaid));
        }

        FinePayment payment = FinePayment.builder()
                .borrowing(borrowing)
                .amount(paymentAmount)
                .paymentDate(form.getPaymentDate() != null ? form.getPaymentDate() : LocalDateTime.now())
                .method(form.getMethod() != null ? form.getMethod() : "CASH")
                .note(form.getNote())
                .build();

        FinePayment savedPayment = finePaymentRepository.save(payment);

        BigDecimal currentPaid = borrowing.getPaidFineAmount() != null ? borrowing.getPaidFineAmount() : BigDecimal.ZERO;
        borrowing.setPaidFineAmount(currentPaid.add(paymentAmount));
        borrowing.recalculateDebt();
        borrowingRepository.save(borrowing);

        log.info("Recorded fine payment ID {} of {} VND for borrowing ID {}. Remaining debt: {}",
                savedPayment.getId(), paymentAmount, borrowingId, borrowing.getUnpaidFineAmount());

        return savedPayment;
    }

    @Override
    @Transactional
    public FineWaiver recordWaiver(Long borrowingId, FineWaiverForm form) {
        Borrowing borrowing = borrowingRepository.findById(borrowingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu mượn với ID: " + borrowingId));

        BigDecimal unpaid = borrowing.getUnpaidFineAmount() != null
                ? borrowing.getUnpaidFineAmount()
                : BigDecimal.ZERO;

        if (unpaid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Phiếu mượn không có tiền phạt cần miễn/giảm.");
        }

        BigDecimal waiverAmount = form.getAmount();
        if (waiverAmount == null || waiverAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Số tiền miễn/giảm phải lớn hơn 0.");
        }

        if (waiverAmount.compareTo(unpaid) > 0) {
            throw new BusinessRuleException(String.format(
                    "Số tiền miễn/giảm (%s) vượt quá số tiền phạt còn nợ (%s).",
                    waiverAmount, unpaid));
        }

        if (form.getReason() == null || form.getReason().trim().isEmpty()) {
            throw new BusinessRuleException("Lý do miễn/giảm không được để trống.");
        }

        if (form.getApprovedBy() == null || form.getApprovedBy().trim().isEmpty()) {
            throw new BusinessRuleException("Người duyệt không được để trống.");
        }

        FineWaiver waiver = FineWaiver.builder()
                .borrowing(borrowing)
                .amount(waiverAmount)
                .reason(form.getReason().trim())
                .approvedBy(form.getApprovedBy().trim())
                .approvedDate(LocalDateTime.now())
                .build();

        FineWaiver savedWaiver = fineWaiverRepository.save(waiver);

        BigDecimal currentWaived = borrowing.getWaivedFineAmount() != null ? borrowing.getWaivedFineAmount() : BigDecimal.ZERO;
        borrowing.setWaivedFineAmount(currentWaived.add(waiverAmount));
        borrowing.recalculateDebt();
        borrowingRepository.save(borrowing);

        log.info("Recorded fine waiver ID {} of {} VND for borrowing ID {} approved by {}. Remaining debt: {}",
                savedWaiver.getId(), waiverAmount, borrowingId, form.getApprovedBy(), borrowing.getUnpaidFineAmount());

        return savedWaiver;
    }
}
