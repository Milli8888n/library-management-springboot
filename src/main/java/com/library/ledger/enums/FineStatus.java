package com.library.ledger.enums;

public enum FineStatus {
    NONE,           // No fine — returned on time
    FINE_PENDING,   // Fine exists but not fully paid
    FINE_PAID       // Fine fully settled (paid or waived)
}
