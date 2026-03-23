package com.example.library.history;

public record BookViewRecordResponse(
        Long bookId,
        long viewCount,
        boolean counted) {
}
