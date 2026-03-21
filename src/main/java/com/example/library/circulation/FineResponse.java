package com.example.library.circulation;

import java.math.BigDecimal;
import java.time.Instant;

public record FineResponse(
        Long id,
        Long userId,
        String username,
        Long transactionId,
        Long bookId,
        String bookTitle,
        BigDecimal amount,
        String reason,
        FineStatus status,
        Instant createdAt,
        Instant resolvedAt,
        String resolvedByUsername,
        String resolutionNote) {

    static FineResponse from(FineRecord fine) {
        BorrowTransaction transaction = fine.getBorrowTransaction();
        return new FineResponse(
                fine.getId(),
                fine.getUser().getId(),
                fine.getUser().getUsername(),
                transaction != null ? transaction.getId() : null,
                transaction != null ? transaction.getBook().getId() : null,
                transaction != null ? transaction.getBook().getTitle() : null,
                fine.getAmount(),
                fine.getReason(),
                fine.getStatus(),
                fine.getCreatedAt(),
                fine.getResolvedAt(),
                fine.getResolvedByUser() != null ? fine.getResolvedByUser().getUsername() : null,
                fine.getResolutionNote());
    }
}
