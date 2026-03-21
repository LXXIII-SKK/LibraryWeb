package com.example.library.identity;

public enum MembershipStatus {
    GOOD_STANDING,
    OVERDUE_RESTRICTED,
    BORROW_BLOCKED,
    EXPIRED;

    public boolean allowsBorrowing() {
        return this == GOOD_STANDING;
    }
}
