package com.example.library.inventory;

public record DigitalAccessResponse(
        Long transactionId,
        Long bookId,
        String bookTitle,
        String url) {
}
