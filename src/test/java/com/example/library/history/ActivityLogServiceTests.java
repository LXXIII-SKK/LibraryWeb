package com.example.library.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import com.example.library.catalog.Book;
import com.example.library.common.OperationalActivityEvent;
import com.example.library.circulation.BookBorrowedEvent;
import com.example.library.circulation.BookReturnedEvent;
import com.example.library.identity.AccountStatus;
import com.example.library.identity.CurrentUser;
import com.example.library.identity.MembershipStatus;
import com.example.library.identity.AppRole;
import com.example.library.identity.AppUser;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTests {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private com.example.library.identity.CurrentUserService currentUserService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ActivityLogService activityLogService;

    @Test
    void recordViewPersistsFirstViewAndReturnsUpdatedCount() {
        CurrentUser currentUser = new CurrentUser(
                12L,
                "user-1",
                "reader",
                "reader@library.local",
                AppRole.MEMBER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                null,
                null);
        AppUser user = new AppUser("user-1", "reader", "reader@library.local", AppRole.MEMBER);
        Book book = new Book("DDD", "Eric Evans", "Architecture", "9780321125217", 3);
        org.springframework.test.util.ReflectionTestUtils.setField(book, "id", 9L);

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(entityManager.getReference(Book.class, 9L)).thenReturn(book);
        when(entityManager.getReference(AppUser.class, 12L)).thenReturn(user);
        when(activityLogRepository.existsByUserIdAndBookIdAndActivityType(12L, 9L, ActivityType.VIEWED)).thenReturn(false);
        when(activityLogRepository.saveAndFlush(any(ActivityLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(activityLogRepository.countByBookIdAndActivityType(9L, ActivityType.VIEWED)).thenReturn(4L);

        BookViewRecordResponse response = activityLogService.recordView(9L);

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getActivityType()).isEqualTo(ActivityType.VIEWED);
        assertThat(captor.getValue().getMessage()).contains("reader viewed");
        assertThat(response.bookId()).isEqualTo(9L);
        assertThat(response.viewCount()).isEqualTo(4L);
        assertThat(response.counted()).isTrue();
    }

    @Test
    void recordViewDoesNotPersistDuplicateViewForSameUserAndBook() {
        CurrentUser currentUser = new CurrentUser(
                12L,
                "user-1",
                "reader",
                "reader@library.local",
                AppRole.MEMBER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                null,
                null);
        Book book = new Book("DDD", "Eric Evans", "Architecture", "9780321125217", 3);
        org.springframework.test.util.ReflectionTestUtils.setField(book, "id", 9L);

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(entityManager.getReference(Book.class, 9L)).thenReturn(book);
        when(activityLogRepository.existsByUserIdAndBookIdAndActivityType(12L, 9L, ActivityType.VIEWED)).thenReturn(true);
        when(activityLogRepository.countByBookIdAndActivityType(9L, ActivityType.VIEWED)).thenReturn(4L);

        BookViewRecordResponse response = activityLogService.recordView(9L);

        verify(activityLogRepository, never()).saveAndFlush(any(ActivityLog.class));
        assertThat(response.bookId()).isEqualTo(9L);
        assertThat(response.viewCount()).isEqualTo(4L);
        assertThat(response.counted()).isFalse();
    }

    @Test
    void borrowedEventCreatesBorrowedActivityLog() {
        AppUser user = new AppUser("user-1", "reader", "reader@library.local", AppRole.MEMBER);
        Book book = new Book("DDD", "Eric Evans", "Architecture", "9780321125217", 3);
        BookBorrowedEvent event = new BookBorrowedEvent(12L, "reader", 12L, "reader", 9L, "DDD", false, Instant.now());

        when(entityManager.getReference(AppUser.class, 12L)).thenReturn(user);
        when(entityManager.getReference(Book.class, 9L)).thenReturn(book);

        activityLogService.onBookBorrowed(event);

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        assertThat(captor.getValue().getActivityType()).isEqualTo(ActivityType.BORROWED);
        assertThat(captor.getValue().getMessage()).contains("reader borrowed");
    }

    @Test
    void readyHoldBorrowEventCreatesPickupMessage() {
        AppUser user = new AppUser("manager-1", "branch.manager", "manager@library.local", AppRole.BRANCH_MANAGER);
        Book book = new Book("DDD", "Eric Evans", "Architecture", "9780321125217", 3);
        BookBorrowedEvent event = new BookBorrowedEvent(7L, "branch.manager", 12L, "reader", 9L, "DDD", true, Instant.now());

        when(entityManager.getReference(AppUser.class, 7L)).thenReturn(user);
        when(entityManager.getReference(Book.class, 9L)).thenReturn(book);

        activityLogService.onBookBorrowed(event);

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        assertThat(captor.getValue().getMessage()).contains("ready hold");
    }

    @Test
    void returnedEventCreatesReturnedActivityLog() {
        AppUser user = new AppUser("user-2", "admin", "admin@library.local", AppRole.ADMIN);
        Book book = new Book("Refactoring", "Martin Fowler", "Engineering", "9780134757599", 5);
        BookReturnedEvent event = new BookReturnedEvent(3L, "admin", 4L, "Refactoring", "reader", Instant.now());

        when(entityManager.getReference(AppUser.class, 3L)).thenReturn(user);
        when(entityManager.getReference(Book.class, 4L)).thenReturn(book);
        when(activityLogRepository.save(any(ActivityLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        activityLogService.onBookReturned(event);

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        assertThat(captor.getValue().getActivityType()).isEqualTo(ActivityType.RETURNED);
        assertThat(captor.getValue().getMessage()).contains("for reader");
    }

    @Test
    void operationalActivityEventCreatesAuditLogWithoutBook() {
        AppUser user = new AppUser("manager-1", "branch.manager", "manager@library.local", AppRole.BRANCH_MANAGER);
        OperationalActivityEvent event = new OperationalActivityEvent(
                7L,
                "BRANCH_UPDATED",
                "branch.manager updated branch [code=CENTRAL]",
                Instant.now());

        when(entityManager.getReference(AppUser.class, 7L)).thenReturn(user);

        activityLogService.onOperationalActivity(event);

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        assertThat(captor.getValue().getActivityType()).isEqualTo(ActivityType.BRANCH_UPDATED);
        assertThat(captor.getValue().getBook()).isNull();
        assertThat(captor.getValue().getMessage()).contains("updated branch");
    }
}
