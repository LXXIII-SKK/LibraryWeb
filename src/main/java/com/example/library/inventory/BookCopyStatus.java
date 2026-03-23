package com.example.library.inventory;

public enum BookCopyStatus {
    AVAILABLE,
    UNAVAILABLE,
    RESERVED_FOR_PICKUP,
    IN_TRANSIT,
    BORROWED,
    CLAIMED_RETURNED,
    LOST,
    DAMAGED;

    public boolean countsAsAvailable() {
        return this == AVAILABLE;
    }

    public boolean canBeRemovedFromHolding() {
        return this == AVAILABLE || this == UNAVAILABLE;
    }

    public boolean canBeAdjustedManually() {
        return this == AVAILABLE || this == UNAVAILABLE;
    }
}
