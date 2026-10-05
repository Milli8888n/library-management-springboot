package com.library.ledger.service;

import com.library.ledger.entity.FinePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for FineCalculationService — 12 mandatory test cases per SRS v1.2 §12.7
 *
 * Formula: lateDays = max(0, actualReturn − dueDate − graceDays)
 *          fine = lateDays × returnedQty × dailyFineAmount
 *          if maxFineAmount set → fine = min(fine, maxFineAmount)
 */
class FineCalculationServiceTest {

    private FineCalculationService service;
    private FinePolicy defaultPolicy;

    @BeforeEach
    void setUp() {
        service = new FineCalculationService();
        // Standard policy: 5,000 ₫/ngày, 0 grace days, no cap
        defaultPolicy = FinePolicy.builder()
                .dailyFineAmount(new BigDecimal("5000"))
                .graceDays(0)
                .maxFineAmount(null)
                .active(true)
                .effectiveFrom(LocalDate.now().minusDays(30))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-01: Mượn thành công, trả đúng hạn → tồn kho trừ đúng, không phát sinh phạt
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-01: Trả đúng hạn → phí phạt = 0")
    void tc01_onTimeReturn_fineIsZero() {
        LocalDate due = LocalDate.of(2026, 10, 1);
        LocalDate actual = LocalDate.of(2026, 10, 1); // same day
        BigDecimal fine = service.calculate(due, actual, 2, defaultPolicy);
        assertThat(fine).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-02: Một sách không đủ số lượng → hệ thống từ chối ngay từ đầu (không âm tồn kho)
    //        Responsibility of BorrowingService; here we verify the calc returns 0 for non-positive qty
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-02: returnedQty <= 0 → fine = 0")
    void tc02_zeroQty_fineIsZero() {
        LocalDate due = LocalDate.of(2026, 10, 1);
        LocalDate actual = LocalDate.of(2026, 10, 10); // 9 days late
        BigDecimal fine = service.calculate(due, actual, 0, defaultPolicy);
        assertThat(fine).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-03: Trả trước hạn → phí phạt = 0
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-03: Trả trước hạn → phí phạt = 0")
    void tc03_returnBeforeDue_fineIsZero() {
        LocalDate due = LocalDate.of(2026, 10, 15);
        LocalDate actual = LocalDate.of(2026, 10, 10); // 5 days early
        BigDecimal fine = service.calculate(due, actual, 3, defaultPolicy);
        assertThat(fine).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-04: Trả trễ 5 ngày, 1 quyển → phạt = 5 × 1 × 5000 = 25,000 ₫
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-04: Trả trễ 5 ngày, 1 quyển → phạt = 25,000 ₫")
    void tc04_lateFiveDaysOneBook() {
        LocalDate due = LocalDate.of(2026, 10, 1);
        LocalDate actual = LocalDate.of(2026, 10, 6); // 5 days late
        BigDecimal fine = service.calculate(due, actual, 1, defaultPolicy);
        assertThat(fine).isEqualByComparingTo(new BigDecimal("25000"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-05: Trả trễ 3 ngày, 2 quyển → phạt = 3 × 2 × 5000 = 30,000 ₫
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-05: Trả trễ 3 ngày, 2 quyển → phạt = 30,000 ₫")
    void tc05_lateThreeDaysTwoBooks() {
        LocalDate due = LocalDate.of(2026, 10, 1);
        LocalDate actual = LocalDate.of(2026, 10, 4); // 3 days late
        BigDecimal fine = service.calculate(due, actual, 2, defaultPolicy);
        assertThat(fine).isEqualByComparingTo(new BigDecimal("30000"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-06: Grace period = 3 ngày; trả trễ 2 ngày → lateDays = max(0, 2-3) = 0 → fine = 0
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-06: Trả trễ 2 ngày, grace 3 ngày → phạt = 0")
    void tc06_withinGracePeriod_fineIsZero() {
        FinePolicy policy = FinePolicy.builder()
                .dailyFineAmount(new BigDecimal("5000"))
                .graceDays(3)
                .maxFineAmount(null)
                .active(true)
                .effectiveFrom(LocalDate.now().minusDays(30))
                .build();
        LocalDate due = LocalDate.of(2026, 10, 1);
        LocalDate actual = LocalDate.of(2026, 10, 3); // 2 days late
        BigDecimal fine = service.calculate(due, actual, 2, policy);
        assertThat(fine).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-07: Grace period = 3 ngày; trả trễ 5 ngày → lateDays = 5-3 = 2 → fine = 2 × 2 × 5000 = 20,000
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-07: Trả trễ 5 ngày, grace 3 ngày, 2 quyển → phạt = 20,000 ₫")
    void tc07_beyondGracePeriod() {
        FinePolicy policy = FinePolicy.builder()
                .dailyFineAmount(new BigDecimal("5000"))
                .graceDays(3)
                .maxFineAmount(null)
                .active(true)
                .effectiveFrom(LocalDate.now().minusDays(30))
                .build();
        LocalDate due = LocalDate.of(2026, 10, 1);
        LocalDate actual = LocalDate.of(2026, 10, 6); // 5 days late
        BigDecimal fine = service.calculate(due, actual, 2, policy);
        assertThat(fine).isEqualByComparingTo(new BigDecimal("20000"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-08: Max cap áp dụng: phạt tính ra = 500,000; cap = 200,000 → fine = 200,000
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-08: maxFineAmount áp dụng đúng — fine bị giới hạn")
    void tc08_maxCapApplied() {
        FinePolicy policy = FinePolicy.builder()
                .dailyFineAmount(new BigDecimal("5000"))
                .graceDays(0)
                .maxFineAmount(new BigDecimal("200000"))
                .active(true)
                .effectiveFrom(LocalDate.now().minusDays(30))
                .build();
        LocalDate due = LocalDate.of(2026, 10, 1);
        // 20 days × 5 books × 5000 = 500,000 → capped at 200,000
        LocalDate actual = LocalDate.of(2026, 10, 21); // 20 days late
        BigDecimal fine = service.calculate(due, actual, 5, policy);
        assertThat(fine).isEqualByComparingTo(new BigDecimal("200000"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-09: Max cap không bị áp dụng khi fine < cap
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-09: fine < maxFineAmount → không bị giới hạn")
    void tc09_fineBelowCap_notCapped() {
        FinePolicy policy = FinePolicy.builder()
                .dailyFineAmount(new BigDecimal("5000"))
                .graceDays(0)
                .maxFineAmount(new BigDecimal("200000"))
                .active(true)
                .effectiveFrom(LocalDate.now().minusDays(30))
                .build();
        LocalDate due = LocalDate.of(2026, 10, 1);
        // 2 days × 1 book × 5000 = 10,000 < 200,000
        LocalDate actual = LocalDate.of(2026, 10, 3);
        BigDecimal fine = service.calculate(due, actual, 1, policy);
        assertThat(fine).isEqualByComparingTo(new BigDecimal("10000"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-10: policy == null → fine = 0 (null safety)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-10: policy null → fine = 0")
    void tc10_nullPolicy_fineIsZero() {
        LocalDate due = LocalDate.of(2026, 10, 1);
        LocalDate actual = LocalDate.of(2026, 10, 10);
        BigDecimal fine = service.calculate(due, actual, 2, null);
        assertThat(fine).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-11: computeLateDays — trả đúng hạn → lateDays = 0
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-11: computeLateDays — trả đúng hạn → 0")
    void tc11_computeLateDays_onTime_zero() {
        long late = service.computeLateDays(
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 10),
                0);
        assertThat(late).isEqualTo(0L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TC-12: computeLateDays — trả trễ 7 ngày, grace 2 → lateDays = 5
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("TC-12: computeLateDays — trả trễ 7 ngày, grace 2 → lateDays = 5")
    void tc12_computeLateDays_lateMinus_grace() {
        long late = service.computeLateDays(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 8), // 7 days late
                2);
        assertThat(late).isEqualTo(5L);
    }
}
