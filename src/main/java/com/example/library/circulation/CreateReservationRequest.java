package com.example.library.circulation;

import jakarta.validation.constraints.NotNull;

public record CreateReservationRequest(@NotNull Long bookId, Long pickupBranchId) {
}
