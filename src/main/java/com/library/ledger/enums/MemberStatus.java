package com.library.ledger.enums;

public enum MemberStatus {
    ACTIVE("Đang hoạt động"),
    BLOCKED("Bị khóa");

    private final String displayName;

    MemberStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
