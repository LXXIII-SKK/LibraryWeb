package com.example.library.discovery;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.library.catalog.Book;
import com.example.library.catalog.BookRepository;
import com.example.library.circulation.BorrowTransaction;
import com.example.library.circulation.BorrowTransactionRepository;
import com.example.library.history.ActivityLog;
import com.example.library.history.ActivityLogRepository;
import com.example.library.history.ActivityType;
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
        List<Book> books = bookRepository.findAll();
        Map<Long, Book> booksById = books.stream().collect(Collectors.toMap(Book::getId, Function.identity()));

        Instant threshold = Instant.now().minus(7, ChronoUnit.DAYS);
        Map<Long, Long> weeklyBorrowCounts = borrowTransactionRepository.findAll().stream()
                .filter(transaction -> transaction.getBorrowedAt().isAfter(threshold))
                .collect(Collectors.groupingBy(
                        transaction -> transaction.getBook().getId(),
                        Collectors.counting()));

        Map<Long, Long> weeklyViewCounts = activityLogRepository.findAll().stream()
                .filter(log -> log.getActivityType() == ActivityType.VIEWED)
                .filter(log -> log.getOccurredAt().isAfter(threshold))
                .filter(log -> log.getBook() != null)
                .collect(Collectors.groupingBy(
                        log -> log.getBook().getId(),
                        Collectors.counting()));

        List<DiscoveryBookResponse> mostBorrowed = weeklyBorrowCounts.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(4)
                .map(entry -> toDiscoveryBook(
                        booksById.get(entry.getKey()),
                        entry.getValue(),
                        "Borrowed %d time%s in the last 7 days".formatted(entry.getValue(), entry.getValue() == 1 ? "" : "s")))
                .filter(book -> book != null)
                .toList();

        List<DiscoveryBookResponse> mostViewed = weeklyViewCounts.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(4)
                .map(entry -> toDiscoveryBook(
                        booksById.get(entry.getKey()),
                        entry.getValue(),
                        "Viewed %d time%s in the last 7 days".formatted(entry.getValue(), entry.getValue() == 1 ? "" : "s")))
                .filter(book -> book != null)
                .toList();

        List<DiscoveryBookResponse> recommendations = books.stream()
                .sorted(recommendationComparator(weeklyBorrowCounts, weeklyViewCounts))
                .limit(4)
                .map(book -> {
                    long borrowed = weeklyBorrowCounts.getOrDefault(book.getId(), 0L);
                    long viewed = weeklyViewCounts.getOrDefault(book.getId(), 0L);
                    String spotlight = borrowed > 0
                            ? "Trending with %d borrow%s this week".formatted(borrowed, borrowed == 1 ? "" : "s")
                            : viewed > 0
                                    ? "Drawing attention with %d view%s this week".formatted(viewed, viewed == 1 ? "" : "s")
                                    : "Strong availability for immediate checkout";
                    return DiscoveryBookResponse.from(book, Math.max(borrowed, viewed), spotlight);
                })
                .toList();

        return new DiscoveryResponse(recommendations, mostBorrowed, mostViewed);
    }

    private Comparator<Book> recommendationComparator(Map<Long, Long> weeklyBorrowCounts, Map<Long, Long> weeklyViewCounts) {
        return Comparator
                .comparingLong((Book book) -> weeklyBorrowCounts.getOrDefault(book.getId(), 0L) * 5
                        + weeklyViewCounts.getOrDefault(book.getId(), 0L) * 3
                        + book.getAvailableQuantity() * 2L)
                .reversed()
                .thenComparing(Book::getTitle);
    }

    private DiscoveryBookResponse toDiscoveryBook(Book book, long weeklyCount, String spotlight) {
        if (book == null) {
            return null;
        }

        return DiscoveryBookResponse.from(book, weeklyCount, spotlight);
    }
}
