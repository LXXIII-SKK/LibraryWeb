package com.example.library.circulation;

public enum BorrowStatus {
    BORROWED,
    CLAIMED_RETURNED,
    LOST,
    DAMAGED,
    RETURNED;

    public boolean canReturnToInventory() {
        return this == BORROWED || this == CLAIMED_RETURNED;
    }

    public boolean isExceptionState() {
        return this == CLAIMED_RETURNED || this == LOST || this == DAMAGED;
    }

    public boolean isClosed() {
        return this == RETURNED || this == LOST || this == DAMAGED;
    }

    public boolean isRenewable() {
        return this == BORROWED;
    }
}
