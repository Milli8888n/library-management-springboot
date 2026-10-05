package com.library.ledger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "borrowing_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BorrowingDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrowing_id", nullable = false)
    private Borrowing borrowing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "returned_quantity", nullable = false)
    @Builder.Default
    private Integer returnedQuantity = 0;

    @Column(name = "fine_amount", nullable = false)
    @Builder.Default
    private BigDecimal fineAmount = BigDecimal.ZERO;

    // === SRS v1.2 §12 snapshot columns ===
    /** Số ngày trễ tích luĩ qua các lần trả */
    @Column(name = "late_days", nullable = false)
    @Builder.Default
    private Long lateDays = 0L;

    /** Snapshot daily fine rate tại thời điểm trả — để policy thay đổi không hồi tố */
    @Column(name = "daily_fine_snapshot", nullable = false)
    @Builder.Default
    private BigDecimal dailyFineSnapshot = new BigDecimal("5000");

    /** Snapshot grace days tại thời điểm trả */
    @Column(name = "grace_days_snapshot", nullable = false)
    @Builder.Default
    private Integer graceDaysSnapshot = 0;

    public Integer getRemainingQuantity() {
        if (quantity == null) return 0;
        int returned = returnedQuantity != null ? returnedQuantity : 0;
        return Math.max(0, quantity - returned);
    }

    public boolean isFullyReturned() {
        return getRemainingQuantity() == 0;
    }
}
