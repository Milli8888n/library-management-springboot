package com.library.ledger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Stores the fine policy — daily rate, grace period, optional cap.
 * Only one policy can be active at a time (enforced by unique index in DB).
 * Policy is snapshot'd onto BorrowingDetail at return time so changes are non-retroactive.
 */
@Entity
@Table(name = "fine_policy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Phí phạt mỗi cuốn / mỗi ngày trễ (VND) */
    @Column(name = "daily_fine_amount", nullable = false)
    private BigDecimal dailyFineAmount;

    /** Mức trần phí tối đa mỗi dòng detail — null = không giới hạn */
    @Column(name = "max_fine_amount")
    private BigDecimal maxFineAmount;

    /** Số ngày được miễn phí phạt (grace period) */
    @Column(name = "grace_days", nullable = false)
    @Builder.Default
    private Integer graceDays = 0;

    /** Chỉ một policy được active tại một thời điểm */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = false;

    /** Ngày bắt đầu áp dụng */
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (graceDays == null) graceDays = 0;
        if (active == null) active = false;
    }
}
