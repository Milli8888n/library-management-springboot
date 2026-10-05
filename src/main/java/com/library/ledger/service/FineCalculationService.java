package com.library.ledger.service;

import com.library.ledger.entity.FinePolicy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Pure calculation logic — no DB calls, fully unit-testable.
 *
 * Formula (SRS v1.2 §12.2):
 *   lateDays = max(0, actualReturnDate − dueDate − graceDays)
 *   fineAmount = lateDays × returnedQtyThisTime × dailyFineAmount
 *   if maxFineAmount != null → fineAmount = min(fineAmount, maxFineAmount)
 */
@Service
public class FineCalculationService {

    /**
     * Calculate fine for a single return event on one BorrowingDetail line.
     *
     * @param dueDate              the due date of the borrowing
     * @param actualReturnDate     the actual return date
     * @param returnedQty          number of books returned in this transaction
     * @param policy               the active FinePolicy at return time
     * @return calculated fine amount (never negative)
     */
    public BigDecimal calculate(LocalDate dueDate,
                                LocalDate actualReturnDate,
                                int returnedQty,
                                FinePolicy policy) {
        if (dueDate == null || actualReturnDate == null || returnedQty <= 0 || policy == null) {
            return BigDecimal.ZERO;
        }

        long graceDays = policy.getGraceDays() != null ? policy.getGraceDays() : 0;
        long rawLateDays = ChronoUnit.DAYS.between(dueDate, actualReturnDate);
        long lateDays = Math.max(0L, rawLateDays - graceDays);

        if (lateDays == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal daily = policy.getDailyFineAmount() != null
                ? policy.getDailyFineAmount()
                : BigDecimal.ZERO;

        BigDecimal fine = BigDecimal.valueOf(lateDays)
                .multiply(BigDecimal.valueOf(returnedQty))
                .multiply(daily);

        // Apply per-line cap if configured
        if (policy.getMaxFineAmount() != null
                && fine.compareTo(policy.getMaxFineAmount()) > 0) {
            fine = policy.getMaxFineAmount();
        }

        return fine;
    }

    /**
     * Compute lateDays (net of grace) without calculating monetary amount.
     * Useful for storing the snapshot on BorrowingDetail.
     */
    public long computeLateDays(LocalDate dueDate, LocalDate actualReturnDate, int graceDays) {
        if (dueDate == null || actualReturnDate == null) return 0;
        long raw = ChronoUnit.DAYS.between(dueDate, actualReturnDate);
        return Math.max(0L, raw - graceDays);
    }
}
