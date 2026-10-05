package com.library.ledger.entity;

import com.library.ledger.enums.BorrowingStatus;
import com.library.ledger.enums.FineStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "borrowing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Borrowing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "borrow_date", nullable = false)
    private LocalDate borrowDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "returned_date")
    private LocalDate returnedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BorrowingStatus status = BorrowingStatus.BORROWING;

    /** Legacy column — kept for backward compat; prefer totalFineAmount */
    @Column(name = "total_fine", nullable = false)
    @Builder.Default
    private BigDecimal totalFine = BigDecimal.ZERO;

    // === SRS v1.2 §12 Fine tracking columns ===

    @Column(name = "total_fine_amount", nullable = false)
    @Builder.Default
    private BigDecimal totalFineAmount = BigDecimal.ZERO;

    @Column(name = "paid_fine_amount", nullable = false)
    @Builder.Default
    private BigDecimal paidFineAmount = BigDecimal.ZERO;

    @Column(name = "waived_fine_amount", nullable = false)
    @Builder.Default
    private BigDecimal waivedFineAmount = BigDecimal.ZERO;

    @Column(name = "unpaid_fine_amount", nullable = false)
    @Builder.Default
    private BigDecimal unpaidFineAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "fine_status", nullable = false, length = 20)
    @Builder.Default
    private FineStatus fineStatus = FineStatus.NONE;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "borrowing", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BorrowingDetail> details = new ArrayList<>();

    @OneToMany(mappedBy = "borrowing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FinePayment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "borrowing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FineWaiver> waivers = new ArrayList<>();

    // === Helpers ===

    /**
     * Recalculate unpaidFineAmount and fineStatus after any payment/waiver change.
     * Must be called in the same transaction as the payment/waiver save.
     */
    public void recalculateDebt() {
        BigDecimal unpaid = totalFineAmount
                .subtract(paidFineAmount)
                .subtract(waivedFineAmount);
        this.unpaidFineAmount = unpaid.max(BigDecimal.ZERO);
        // Sync legacy column
        this.totalFine = this.totalFineAmount;
        if (this.totalFineAmount.compareTo(BigDecimal.ZERO) == 0) {
            this.fineStatus = FineStatus.NONE;
        } else if (this.unpaidFineAmount.compareTo(BigDecimal.ZERO) == 0) {
            this.fineStatus = FineStatus.FINE_PAID;
        } else {
            this.fineStatus = FineStatus.FINE_PENDING;
        }
    }

    public void addDetail(BorrowingDetail detail) {
        details.add(detail);
        detail.setBorrowing(this);
    }

    public void removeDetail(BorrowingDetail detail) {
        details.remove(detail);
        detail.setBorrowing(null);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = BorrowingStatus.BORROWING;
        if (fineStatus == null) fineStatus = FineStatus.NONE;
        if (totalFine == null) totalFine = BigDecimal.ZERO;
        if (totalFineAmount == null) totalFineAmount = BigDecimal.ZERO;
        if (paidFineAmount == null) paidFineAmount = BigDecimal.ZERO;
        if (waivedFineAmount == null) waivedFineAmount = BigDecimal.ZERO;
        if (unpaidFineAmount == null) unpaidFineAmount = BigDecimal.ZERO;
    }
}
