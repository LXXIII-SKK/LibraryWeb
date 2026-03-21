package com.example.library.upcoming;

import java.time.Instant;
import java.util.List;

import com.example.library.branch.BranchSummaryResponse;

public record UpcomingBookResponse(
        Long id,
        String title,
        String author,
        String category,
        String isbn,
        String summary,
        Instant expectedAt,
        BranchSummaryResponse branch,
        List<String> tags) {

    static UpcomingBookResponse from(UpcomingBook upcomingBook) {
        return new UpcomingBookResponse(
                upcomingBook.getId(),
                upcomingBook.getTitle(),
                upcomingBook.getAuthor(),
                upcomingBook.getCategory(),
                upcomingBook.getIsbn(),
                upcomingBook.getSummary(),
                upcomingBook.getExpectedAt(),
                BranchSummaryResponse.from(upcomingBook.getBranch()),
                upcomingBook.getTags());
    }
}
