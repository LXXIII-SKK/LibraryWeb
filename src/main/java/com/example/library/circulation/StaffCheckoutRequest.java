package com.example.library.circulation;

import jakarta.validation.constraints.NotNull;

public record StaffCheckoutRequest(
        @NotNull Long userId,
        @NotNull Long bookId,
        Long holdingId,
        Long reservationId) {
}
