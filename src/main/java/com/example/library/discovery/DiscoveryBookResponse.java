package com.example.library.discovery;

import java.util.List;

import com.example.library.catalog.Book;

public record DiscoveryBookResponse(
        Long id,
        String title,
        String author,
        String category,
        String isbn,
        int totalQuantity,
        int availableQuantity,
        List<String> tags,
        String coverImageUrl,
        long weeklyCount,
        String spotlight) {

    static DiscoveryBookResponse from(Book book, long weeklyCount, String spotlight) {
        return new DiscoveryBookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getCategory(),
                book.getIsbn(),
                book.getTotalQuantity(),
                book.getAvailableQuantity(),
                book.getTags(),
                book.hasCoverImage() ? "/api/books/%d/cover".formatted(book.getId()) : null,
                weeklyCount,
                spotlight);
    }
}
