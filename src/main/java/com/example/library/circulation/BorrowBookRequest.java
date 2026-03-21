package com.example.library.circulation;

import jakarta.validation.constraints.NotNull;

public record BorrowBookRequest(@NotNull Long bookId, Long holdingId) {
}
