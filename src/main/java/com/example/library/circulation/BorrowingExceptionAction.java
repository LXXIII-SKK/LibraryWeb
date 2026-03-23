package com.example.library.circulation;

public enum BorrowingExceptionAction {
    CLAIM_RETURNED(BorrowStatus.CLAIMED_RETURNED),
    MARK_LOST(BorrowStatus.LOST),
    MARK_DAMAGED(BorrowStatus.DAMAGED);

    private final BorrowStatus status;

    BorrowingExceptionAction(BorrowStatus status) {
        this.status = status;
    }

    public BorrowStatus status() {
        return status;
    }
}
