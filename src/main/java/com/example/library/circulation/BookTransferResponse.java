package com.example.library.circulation;

import java.time.Instant;

import com.example.library.branch.BranchSummaryResponse;
import com.example.library.inventory.LocationSummaryResponse;

public record BookTransferResponse(
        Long id,
        Long copyId,
        String copyBarcode,
        Long bookId,
        String bookTitle,
        Long sourceHoldingId,
        BranchSummaryResponse sourceBranch,
        LocationSummaryResponse sourceLocation,
        BranchSummaryResponse destinationBranch,
        BookTransferStatus status,
        Instant requestedAt,
        Instant dispatchedAt,
        Instant readyAt,
        Instant completedAt,
        Instant closedAt) {

    public static BookTransferResponse from(BookTransfer transfer) {
        return new BookTransferResponse(
                transfer.getId(),
                transfer.getCopy().getId(),
                transfer.getCopy().getBarcode(),
                transfer.getSourceHolding().getBook().getId(),
                transfer.getSourceHolding().getBook().getTitle(),
                transfer.getSourceHolding().getId(),
                BranchSummaryResponse.from(transfer.getSourceHolding().getBranch()),
                LocationSummaryResponse.from(transfer.getSourceHolding().getLocation()),
                BranchSummaryResponse.from(transfer.getDestinationBranch()),
                transfer.getStatus(),
                transfer.getRequestedAt(),
                transfer.getDispatchedAt(),
                transfer.getReadyAt(),
                transfer.getCompletedAt(),
                transfer.getClosedAt());
    }
}
