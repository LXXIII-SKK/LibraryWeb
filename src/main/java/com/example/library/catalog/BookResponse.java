package com.example.library.catalog;

import java.util.List;

import com.example.library.inventory.BookHolding;
import com.example.library.inventory.BookHoldingResponse;

public record BookResponse(
        Long id,
        String title,
        String author,
        String category,
        String isbn,
        int totalQuantity,
        int availableQuantity,
        List<String> tags,
        String coverImageUrl,
        boolean hasOnlineAccess,
        List<BookHoldingResponse> availability) {

    static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getCategory(),
                book.getIsbn(),
                book.getTotalQuantity(),
                book.getAvailableQuantity(),
                book.getTags(),
                book.hasCoverImage() ? "/api/books/%d/cover".formatted(book.getId()) : null,
                book.getHoldings().stream().anyMatch(holding -> holding.isActive() && holding.hasOnlineAccess()),
                book.getHoldings().stream()
                        .filter(BookHolding::isActive)
                        .map(BookHoldingResponse::from)
                        .toList());
    }
}
