package com.library.ledger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records a fine waiver (miễn/giảm) — immutable after creation.
 * Requires reason and approvedBy; only authorized roles may create.
 */
@Entity
@Table(name = "fine_waiver")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FineWaiver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrowing_id", nullable = false)
    private Borrowing borrowing;

    /** Số tiền được miễn/giảm — phải > 0 và ≤ unpaidFineAmount */
    @Column(nullable = false)
    private BigDecimal amount;

    /** Lý do miễn/giảm — bắt buộc */
    @Column(nullable = false, length = 500)
    private String reason;

    /** Người duyệt — bắt buộc */
    @Column(name = "approved_by", nullable = false, length = 100)
    private String approvedBy;

    @Column(name = "approved_date", nullable = false)
    @Builder.Default
    private LocalDateTime approvedDate = LocalDateTime.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (approvedDate == null) approvedDate = LocalDateTime.now();
    }
}
