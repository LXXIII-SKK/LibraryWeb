package com.example.library.circulation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BorrowingExceptionRequest(
        @NotNull BorrowingExceptionAction action,
        @NotBlank @Size(max = 500) String note) {
}
