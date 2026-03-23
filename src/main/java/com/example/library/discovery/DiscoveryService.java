package com.example.library.discovery;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.library.catalog.Book;
import com.example.library.catalog.BookRepository;
import com.example.library.circulation.BookBorrowCount;
import com.example.library.circulation.BorrowTransactionRepository;
import com.example.library.history.ActivityLogRepository;
import com.example.library.history.ActivityType;
import com.example.library.history.BookViewCount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DiscoveryService {

    private final BookRepository bookRepository;
    private final BorrowTransactionRepository borrowTransactionRepository;
    private final ActivityLogRepository activityLogRepository;

    public DiscoveryService(
            BookRepository bookRepository,
            BorrowTransactionRepository borrowTransactionRepository,
            ActivityLogRepository activityLogRepository) {
        this.bookRepository = bookRepository;
        this.borrowTransactionRepository = borrowTransactionRepository;
        this.activityLogRepository = activityLogRepository;
    }

    public DiscoveryResponse getDiscovery() {
        Instant threshold = Instant.now().minus(7, ChronoUnit.DAYS);
        Map<Long, Long> weeklyBorrowCounts = borrowTransactionRepository.countUniqueBorrowersByBookSince(threshold).stream()
                .collect(Collectors.toMap(BookBorrowCount::getBookId, BookBorrowCount::getWeeklyCount));

        Map<Long, Long> weeklyViewCounts = activityLogRepository.countByBookSinceAndActivityType(threshold, ActivityType.VIEWED).stream()
                .collect(Collectors.toMap(BookViewCount::getBookId, BookViewCount::getWeeklyCount));

        List<Long> mostBorrowedIds = topBookIds(weeklyBorrowCounts);
        List<Long> mostViewedIds = topBookIds(weeklyViewCounts);
        List<Book> recommendedBooks = bookRepository.findTopRecommendedBooks(threshold);

        LinkedHashSet<Long> candidateBookIds = new LinkedHashSet<>();
        candidateBookIds.addAll(mostBorrowedIds);
        candidateBookIds.addAll(mostViewedIds);
        recommendedBooks.stream().map(Book::getId).forEach(candidateBookIds::add);

        Map<Long, Book> booksById = candidateBookIds.isEmpty()
                ? Map.of()
                : bookRepository.findAllById(candidateBookIds).stream()
                        .collect(Collectors.toMap(Book::getId, book -> book));

        List<DiscoveryBookResponse> mostBorrowed = mostBorrowedIds.stream()
                .map(entry -> toDiscoveryBook(
                        booksById.get(entry),
                        weeklyBorrowCounts.getOrDefault(entry, 0L),
                        "Borrowed by %d reader%s in the last 7 days"
                                .formatted(weeklyBorrowCounts.getOrDefault(entry, 0L), weeklyBorrowCounts.getOrDefault(entry, 0L) == 1 ? "" : "s")))
                .filter(book -> book != null)
                .toList();

        List<DiscoveryBookResponse> mostViewed = mostViewedIds.stream()
                .map(entry -> toDiscoveryBook(
                        booksById.get(entry),
                        weeklyViewCounts.getOrDefault(entry, 0L),
                        "Viewed %d time%s in the last 7 days"
                                .formatted(weeklyViewCounts.getOrDefault(entry, 0L), weeklyViewCounts.getOrDefault(entry, 0L) == 1 ? "" : "s")))
                .filter(book -> book != null)
                .toList();

        List<DiscoveryBookResponse> recommendations = recommendedBooks.stream()
                .map(book -> {
                    long borrowed = weeklyBorrowCounts.getOrDefault(book.getId(), 0L);
                    long viewed = weeklyViewCounts.getOrDefault(book.getId(), 0L);
                    String spotlight = borrowed > 0
                            ? "Trending with %d borrowing reader%s this week"
                                    .formatted(borrowed, borrowed == 1 ? "" : "s")
                            : viewed > 0
                                    ? "Drawing attention with %d view%s this week".formatted(viewed, viewed == 1 ? "" : "s")
                                    : "Strong availability for immediate checkout";
                    return DiscoveryBookResponse.from(book, Math.max(borrowed, viewed), spotlight);
                })
                .toList();

        return new DiscoveryResponse(recommendations, mostBorrowed, mostViewed);
    }

    private DiscoveryBookResponse toDiscoveryBook(Book book, long weeklyCount, String spotlight) {
        if (book == null) {
            return null;
        }

        return DiscoveryBookResponse.from(book, weeklyCount, spotlight);
    }

    private List<Long> topBookIds(Map<Long, Long> counts) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry::getKey))
                .limit(4)
                .map(Map.Entry::getKey)
                .toList();
    }
}
