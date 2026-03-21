package com.example.library.circulation;

import java.time.Instant;

public record RenewBorrowingRequest(Instant dueAt, String reason) {
}
