package com.example.library.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import com.example.library.catalog.Book;
import com.example.library.catalog.BookRepository;
import com.example.library.circulation.BookBorrowCount;
import com.example.library.circulation.BorrowTransactionRepository;
import com.example.library.history.ActivityLogRepository;
import com.example.library.history.ActivityType;
import com.example.library.history.BookViewCount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiscoveryServiceTests {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BorrowTransactionRepository borrowTransactionRepository;

    @Mock
    private ActivityLogRepository activityLogRepository;

    @InjectMocks
    private DiscoveryService discoveryService;

    @Test
    void getDiscoveryBuildsBorrowedViewedAndRecommendationLists() {
        Book ddd = new Book("Domain-Driven Design", "Eric Evans", "Architecture", "111", 6);
        Book releaseIt = new Book("Release It!", "Michael Nygard", "Operations", "222", 4);
        setId(ddd, 1L);
        setId(releaseIt, 2L);

        when(bookRepository.findTopRecommendedBooks(any(java.time.Instant.class))).thenReturn(List.of(ddd, releaseIt));
        when(bookRepository.findAllById(org.mockito.ArgumentMatchers.anyIterable())).thenReturn(List.of(ddd, releaseIt));
        when(borrowTransactionRepository.countUniqueBorrowersByBookSince(any(java.time.Instant.class)))
                .thenReturn(List.of(borrowCount(1L, 1L)));
        when(activityLogRepository.countByBookSinceAndActivityType(any(java.time.Instant.class), eq(ActivityType.VIEWED)))
                .thenReturn(List.of(viewCount(2L, 1L)));

        DiscoveryResponse response = discoveryService.getDiscovery();

        assertThat(response.recommendations()).hasSize(2);
        assertThat(response.mostBorrowedThisWeek()).extracting(DiscoveryBookResponse::id).containsExactly(1L);
        assertThat(response.mostViewedThisWeek()).extracting(DiscoveryBookResponse::id).containsExactly(2L);
    }

    @Test
    void discoveryCountsUniqueBorrowersPerBookForWeeklyBorrowRanking() {
        Book ddd = new Book("Domain-Driven Design", "Eric Evans", "Architecture", "111", 6);
        setId(ddd, 1L);

        when(bookRepository.findTopRecommendedBooks(any(java.time.Instant.class))).thenReturn(List.of(ddd));
        when(bookRepository.findAllById(org.mockito.ArgumentMatchers.anyIterable())).thenReturn(List.of(ddd));
        when(borrowTransactionRepository.countUniqueBorrowersByBookSince(any(java.time.Instant.class)))
                .thenReturn(List.of(borrowCount(1L, 1L)));
        when(activityLogRepository.countByBookSinceAndActivityType(any(java.time.Instant.class), eq(ActivityType.VIEWED)))
                .thenReturn(List.of());

        DiscoveryResponse response = discoveryService.getDiscovery();

        assertThat(response.mostBorrowedThisWeek()).singleElement()
                .satisfies(book -> assertThat(book.weeklyCount()).isEqualTo(1L));
    }

    private void setId(Object target, Long id) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private BookBorrowCount borrowCount(Long bookId, long weeklyCount) {
        return new BookBorrowCount() {
            @Override
            public Long getBookId() {
                return bookId;
            }

            @Override
            public long getWeeklyCount() {
                return weeklyCount;
            }
        };
    }

    private BookViewCount viewCount(Long bookId, long weeklyCount) {
        return new BookViewCount() {
            @Override
            public Long getBookId() {
                return bookId;
            }

            @Override
            public long getWeeklyCount() {
                return weeklyCount;
            }
        };
    }
}
