package com.example.library.inventory;

import com.example.library.branch.BranchSummaryResponse;

public record BookCopyResponse(
        Long id,
        Long holdingId,
        Long bookId,
        String bookTitle,
        String barcode,
        BookCopyStatus status,
        BranchSummaryResponse currentBranch,
        LocationSummaryResponse currentLocation) {

    public static BookCopyResponse from(BookCopy copy) {
        return new BookCopyResponse(
                copy.getId(),
                copy.getHolding().getId(),
                copy.getHolding().getBook().getId(),
                copy.getHolding().getBook().getTitle(),
                copy.getBarcode(),
                copy.getStatus(),
                BranchSummaryResponse.from(copy.getCurrentBranch()),
                LocationSummaryResponse.from(copy.getCurrentLocation()));
    }
}
