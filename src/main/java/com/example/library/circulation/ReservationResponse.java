package com.example.library.circulation;

import java.time.Instant;

import com.example.library.branch.BranchSummaryResponse;

public record ReservationResponse(
        Long id,
        Long bookId,
        String bookTitle,
        Long userId,
        String username,
        BranchSummaryResponse pickupBranch,
        Long reservedHoldingId,
        Long reservedCopyId,
        String reservedHoldingBranchName,
        Long transferId,
        BookTransferStatus transferStatus,
        String transferDestinationBranchName,
        Instant reservedAt,
        Instant transferRequestedAt,
        Instant readyAt,
        Instant expiresAt,
        Instant updatedAt,
        ReservationStatus status) {

    static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getBook().getId(),
                reservation.getBook().getTitle(),
                reservation.getUser().getId(),
                reservation.getUser().getUsername(),
                BranchSummaryResponse.from(reservation.getPickupBranch()),
                reservation.getReservedHolding() != null ? reservation.getReservedHolding().getId() : null,
                reservation.getReservedCopy() != null ? reservation.getReservedCopy().getId() : null,
                reservation.getReservedHolding() != null && reservation.getReservedHolding().getBranch() != null
                        ? reservation.getReservedHolding().getBranch().getName()
                        : null,
                reservation.getTransfer() != null ? reservation.getTransfer().getId() : null,
                reservation.getTransfer() != null ? reservation.getTransfer().getStatus() : null,
                reservation.getTransfer() != null && reservation.getTransfer().getDestinationBranch() != null
                        ? reservation.getTransfer().getDestinationBranch().getName()
                        : null,
                reservation.getReservedAt(),
                reservation.getTransferRequestedAt(),
                reservation.getReadyAt(),
                reservation.getExpiresAt(),
                reservation.getUpdatedAt(),
                reservation.getStatus());
    }
}
