package com.example.library.circulation;

import java.time.Instant;

import com.example.library.inventory.HoldingFormat;

public record BorrowTransactionResponse(
        Long id,
        Long bookId,
        String bookTitle,
        Long holdingId,
        Long copyId,
        String copyBarcode,
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
        boolean lastRenewalOverride,
        String lastRenewalReason,
        Instant exceptionRecordedAt,
        String exceptionNote,
        Instant returnedAt,
        BorrowStatus status) {

    static BorrowTransactionResponse from(BorrowTransaction transaction) {
        return new BorrowTransactionResponse(
                transaction.getId(),
                transaction.getBook().getId(),
                transaction.getBook().getTitle(),
                transaction.getHolding() != null ? transaction.getHolding().getId() : null,
                transaction.getCopy() != null ? transaction.getCopy().getId() : null,
                transaction.getCopy() != null ? transaction.getCopy().getBarcode() : null,
                transaction.getHolding() != null ? transaction.getHolding().getFormat() : null,
                transaction.getCopy() != null && transaction.getCopy().getCurrentBranch() != null
                        ? transaction.getCopy().getCurrentBranch().getName()
                        : transaction.getHolding() != null && transaction.getHolding().getBranch() != null
                                ? transaction.getHolding().getBranch().getName()
                                : null,
                transaction.getCopy() != null && transaction.getCopy().getCurrentLocation() != null
                        ? transaction.getCopy().getCurrentLocation().getName()
                        : transaction.getHolding() != null && transaction.getHolding().getLocation() != null
                                ? transaction.getHolding().getLocation().getName()
                        : null,
                transaction.getHolding() != null && transaction.getHolding().hasOnlineAccess(),
                transaction.getUser().getId(),
                transaction.getUser().getUsername(),
                transaction.getBorrowedAt(),
                transaction.getDueAt(),
                transaction.getLastRenewedAt(),
                transaction.getRenewalCount(),
                transaction.isLastRenewalOverride(),
                transaction.getLastRenewalReason(),
                transaction.getExceptionRecordedAt(),
                transaction.getExceptionNote(),
                transaction.getReturnedAt(),
                transaction.getStatus());
    }
}
