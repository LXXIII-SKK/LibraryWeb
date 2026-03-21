package com.example.library.circulation;

import java.time.Instant;

import com.example.library.inventory.HoldingFormat;

public record BorrowTransactionResponse(
        Long id,
        Long bookId,
        String bookTitle,
        Long holdingId,
        HoldingFormat holdingFormat,
        String branchName,
        String locationName,
        boolean digitalAccessAvailable,
        Long userId,
        String username,
        Instant borrowedAt,
        Instant dueAt,
        Instant lastRenewedAt,
        int renewalCount,
        Instant returnedAt,
        BorrowStatus status) {

    static BorrowTransactionResponse from(BorrowTransaction transaction) {
        return new BorrowTransactionResponse(
                transaction.getId(),
                transaction.getBook().getId(),
                transaction.getBook().getTitle(),
                transaction.getHolding() != null ? transaction.getHolding().getId() : null,
                transaction.getHolding() != null ? transaction.getHolding().getFormat() : null,
                transaction.getHolding() != null && transaction.getHolding().getBranch() != null
                        ? transaction.getHolding().getBranch().getName()
                        : null,
                transaction.getHolding() != null && transaction.getHolding().getLocation() != null
                        ? transaction.getHolding().getLocation().getName()
                        : null,
                transaction.getHolding() != null && transaction.getHolding().hasOnlineAccess(),
                transaction.getUser().getId(),
                transaction.getUser().getUsername(),
                transaction.getBorrowedAt(),
                transaction.getDueAt(),
                transaction.getLastRenewedAt(),
                transaction.getRenewalCount(),
                transaction.getReturnedAt(),
                transaction.getStatus());
    }
}
