package com.library.ledger.enums;

public enum BorrowingStatus {
    BORROWING("Đang mượn"),
    OVERDUE("Quá hạn"),
    RETURNED("Đã trả");

    private final String displayName;

    BorrowingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
