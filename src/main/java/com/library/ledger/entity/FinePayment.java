package com.library.ledger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records a fine payment transaction — immutable after creation.
 * Multiple payments can exist per Borrowing until unpaidFineAmount = 0.
 */
@Entity
@Table(name = "fine_payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrowing_id", nullable = false)
    private Borrowing borrowing;

    /** Số tiền thanh toán — phải > 0 và ≤ unpaidFineAmount */
    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    @Builder.Default
    private LocalDateTime paymentDate = LocalDateTime.now();

    /** CASH, TRANSFER, … */
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String method = "CASH";

    @Column(length = 500)
    private String note;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (paymentDate == null) paymentDate = LocalDateTime.now();
        if (method == null) method = "CASH";
    }
}
