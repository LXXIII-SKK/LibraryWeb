package com.example.library.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.example.library.catalog.Book;
import com.example.library.catalog.BookRepository;
import com.example.library.circulation.BorrowTransaction;
import com.example.library.circulation.BorrowTransactionRepository;
import com.example.library.history.ActivityLog;
import com.example.library.history.ActivityLogRepository;
import com.example.library.history.ActivityType;
import com.example.library.identity.AppRole;
import com.example.library.identity.AppUser;
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

        AppUser reader = new AppUser("kc-1", "reader", "reader@library.local", AppRole.MEMBER);
        setId(reader, 10L);

        BorrowTransaction borrow = new BorrowTransaction(reader, ddd, Instant.now().minus(2, ChronoUnit.DAYS), Instant.now().plus(7, ChronoUnit.DAYS));
        ActivityLog viewed = new ActivityLog(reader, releaseIt, ActivityType.VIEWED, "reader viewed Release It!", Instant.now().minus(1, ChronoUnit.DAYS));

        when(bookRepository.findAll()).thenReturn(List.of(ddd, releaseIt));
        when(borrowTransactionRepository.findAll()).thenReturn(List.of(borrow));
        when(activityLogRepository.findAll()).thenReturn(List.of(viewed));

        DiscoveryResponse response = discoveryService.getDiscovery();

        assertThat(response.recommendations()).hasSize(2);
        assertThat(response.mostBorrowedThisWeek()).extracting(DiscoveryBookResponse::id).containsExactly(1L);
        assertThat(response.mostViewedThisWeek()).extracting(DiscoveryBookResponse::id).containsExactly(2L);
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
}
