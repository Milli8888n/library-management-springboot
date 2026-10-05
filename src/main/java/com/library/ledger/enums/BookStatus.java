package com.library.ledger.enums;

public enum BookStatus {
    ACTIVE("Đang hoạt động"),
    INACTIVE("Ngừng cho mượn"),
    DELETED("Đã xóa");

    private final String displayName;

    BookStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
