package com.example.library.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookHoldingUpsertRequest(
        @NotNull Long bookId,
        @NotNull Long branchId,
        Long locationId,
        @NotNull HoldingFormat format,
        @Min(0) int totalQuantity,
        @Min(0) int availableQuantity,
        @Size(max = 500) String accessUrl,
        boolean active) {
}
