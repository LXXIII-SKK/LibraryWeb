package com.example.library.inventory;

import com.example.library.branch.BranchSummaryResponse;

public record BookHoldingResponse(
        Long id,
        Long bookId,
        String bookTitle,
        HoldingFormat format,
        BranchSummaryResponse branch,
        LocationSummaryResponse location,
        int totalQuantity,
        int availableQuantity,
        long trackedCopyCount,
        boolean active,
        boolean onlineAccess) {

    public static BookHoldingResponse from(BookHolding holding) {
        return new BookHoldingResponse(
                holding.getId(),
                holding.getBook().getId(),
                holding.getBook().getTitle(),
                holding.getFormat(),
                BranchSummaryResponse.from(holding.getBranch()),
                LocationSummaryResponse.from(holding.getLocation()),
                holding.getTotalQuantity(),
                holding.getAvailableQuantity(),
                holding.getFormat() == HoldingFormat.PHYSICAL ? holding.getTotalQuantity() : 0,
                holding.isActive(),
                holding.hasOnlineAccess());
    }
}
