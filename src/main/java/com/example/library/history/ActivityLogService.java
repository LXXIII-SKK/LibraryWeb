package com.example.library.history;

import java.time.Instant;
import java.util.List;

import com.example.library.catalog.Book;
import com.example.library.common.OperationalActivityEvent;
import com.example.library.circulation.BookBorrowedEvent;
import com.example.library.circulation.BorrowingExceptionRecordedEvent;
import com.example.library.circulation.BorrowingRenewedEvent;
import com.example.library.circulation.BookReturnedEvent;
import com.example.library.circulation.FineWaivedEvent;
import com.example.library.circulation.PolicyUpdatedEvent;
import com.example.library.circulation.ReservationCancelledEvent;
import com.example.library.circulation.ReservationCreatedEvent;
import com.example.library.circulation.ReservationNoShowEvent;
import com.example.library.identity.AppPermission;
import com.example.library.identity.AppUser;
import com.example.library.identity.CurrentUser;
import com.example.library.identity.CurrentUserService;
import com.example.library.identity.UserDisciplineRecordedEvent;
import com.example.library.identity.UserAccessUpdatedEvent;
import jakarta.persistence.EntityManager;
import org.springframework.context.event.EventListener;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ActivityLogService {

    private static final String VIEWED_ONCE_INDEX = "ux_activity_log_viewed_user_book_once_per_reset";

    private final ActivityLogRepository activityLogRepository;
    private final CurrentUserService currentUserService;
    private final EntityManager entityManager;

    public ActivityLogService(
            ActivityLogRepository activityLogRepository,
            CurrentUserService currentUserService,
            EntityManager entityManager) {
        this.activityLogRepository = activityLogRepository;
        this.currentUserService = currentUserService;
        this.entityManager = entityManager;
    }

    public List<ActivityLogResponse> listForCurrentUser() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return activityLogRepository.findAllByUserIdOrderByOccurredAtDesc(currentUser.id()).stream()
                .map(ActivityLogResponse::from)
                .toList();
    }

    public List<ActivityLogResponse> listAll() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        List<ActivityLog> logs = switch (currentUser.scope()) {
            case GLOBAL -> activityLogRepository.findAllByOrderByOccurredAtDesc();
            case BRANCH -> activityLogRepository.findAllByUser_Branch_IdOrderByOccurredAtDesc(resolveBranchScope(currentUser));
            case SELF -> throw new org.springframework.security.access.AccessDeniedException(
                    "Self-scoped users cannot view branch or global activity logs");
        };
        return logs.stream()
                .map(ActivityLogResponse::from)
                .toList();
    }

    @Transactional
    public BookViewRecordResponse recordView(Long bookId) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Book book = bookReference(bookId);
        boolean counted = false;
        if (!activityLogRepository.existsByUserIdAndBookIdAndActivityType(currentUser.id(), bookId, ActivityType.VIEWED)) {
            try {
                activityLogRepository.saveAndFlush(new ActivityLog(
                        userReference(currentUser.id()),
                        book,
                        ActivityType.VIEWED,
                        "%s viewed \"%s\"".formatted(currentUser.username(), book.getTitle()),
                        Instant.now()));
                counted = true;
            } catch (DataIntegrityViolationException exception) {
                if (!isDuplicateViewedLogViolation(exception)) {
                    throw exception;
                }
            }
        }

        return new BookViewRecordResponse(
                bookId,
                activityLogRepository.countByBookIdAndActivityType(bookId, ActivityType.VIEWED),
                counted);
    }

    @EventListener
    @Transactional
    public void onBookBorrowed(BookBorrowedEvent event) {
        activityLogRepository.save(new ActivityLog(
                userReference(event.actorUserId()),
                bookReference(event.bookId()),
                ActivityType.BORROWED,
                borrowMessage(event),
                event.occurredAt()));
    }

    @EventListener
    @Transactional
    public void onBookReturned(BookReturnedEvent event) {
        activityLogRepository.save(new ActivityLog(
                userReference(event.actorUserId()),
                bookReference(event.bookId()),
                ActivityType.RETURNED,
                returnMessage(event),
                event.occurredAt()));
    }

    @EventListener
    @Transactional
    public void onBorrowingRenewed(BorrowingRenewedEvent event) {
        activityLogRepository.save(new ActivityLog(
                userReference(event.actorUserId()),
                bookReference(event.bookId()),
                ActivityType.RENEWED,
                renewalMessage(event),
                event.occurredAt()));
    }

    @EventListener
    @Transactional
    public void onBorrowingExceptionRecorded(BorrowingExceptionRecordedEvent event) {
        activityLogRepository.save(new ActivityLog(
                userReference(event.actorUserId()),
                bookReference(event.bookId()),
                ActivityType.BORROWING_EXCEPTION_RECORDED,
                "%s recorded %s on \"%s\" for %s. Note: %s".formatted(
                        event.actorUsername(),
                        event.status().name().toLowerCase().replace('_', ' '),
                        event.bookTitle(),
                        event.targetUsername(),
                        event.note()),
                event.occurredAt()));
    }

    @EventListener
    @Transactional
    public void onReservationCreated(ReservationCreatedEvent event) {
        activityLogRepository.save(new ActivityLog(
                userReference(event.actorUserId()),
                bookReference(event.bookId()),
                ActivityType.RESERVED,
                "%s reserved \"%s\"".formatted(event.actorUsername(), event.bookTitle()),
                event.occurredAt()));
    }

    @EventListener
    @Transactional
    public void onReservationCancelled(ReservationCancelledEvent event) {
        activityLogRepository.save(new ActivityLog(
                userReference(event.actorUserId()),
                bookReference(event.bookId()),
                ActivityType.RESERVATION_CANCELLED,
                reservationMessage("cancelled", event.actorUsername(), event.targetUsername(), event.bookTitle()),
                event.occurredAt()));
    }

    @EventListener
    @Transactional
    public void onReservationNoShow(ReservationNoShowEvent event) {
        activityLogRepository.save(new ActivityLog(
                userReference(event.actorUserId()),
                bookReference(event.bookId()),
                ActivityType.RESERVATION_NO_SHOW,
                reservationMessage("marked no-show", event.actorUsername(), event.targetUsername(), event.bookTitle()),
                event.occurredAt()));
    }

    @EventListener
    @Transactional
    public void onFineWaived(FineWaivedEvent event) {
        activityLogRepository.save(new ActivityLog(
                userReference(event.actorUserId()),
                bookReference(event.bookId()),
                ActivityType.FINE_WAIVED,
                fineWaivedMessage(event),
                event.occurredAt()));
    }

    @EventListener
    @Transactional
    public void onPolicyUpdated(PolicyUpdatedEvent event) {
        activityLogRepository.save(new ActivityLog(
                userReference(event.actorUserId()),
                null,
                ActivityType.POLICY_UPDATED,
                "%s updated circulation policy".formatted(event.actorUsername()),
                event.occurredAt()));
    }

    @EventListener
    @Transactional
    public void onOperationalActivity(OperationalActivityEvent event) {
        activityLogRepository.save(new ActivityLog(
                userReference(event.actorUserId()),
                null,
                ActivityType.valueOf(event.activityType()),
                event.message(),
                event.occurredAt()));
    }

    @EventListener
    @Transactional
    public void onUserAccessUpdated(UserAccessUpdatedEvent event) {
        activityLogRepository.save(new ActivityLog(
                userReference(event.actorUserId()),
                null,
                ActivityType.ACCESS_UPDATED,
                event.message(),
                event.occurredAt()));
    }

    @EventListener
    @Transactional
    public void onUserDisciplined(UserDisciplineRecordedEvent event) {
        activityLogRepository.save(new ActivityLog(
                userReference(event.actorUserId()),
                null,
                ActivityType.ACCESS_DISCIPLINE,
                event.message(),
                event.occurredAt()));
    }

    private Long resolveBranchScope(CurrentUser currentUser) {
        if (currentUser.role() == com.example.library.identity.AppRole.BRANCH_MANAGER
                && currentUser.hasPermission(AppPermission.REPORT_BRANCH_READ)
                && currentUser.branchId() != null) {
            return currentUser.branchId();
        }
        throw new org.springframework.security.access.AccessDeniedException("This role cannot view activity logs");
    }

    private AppUser userReference(Long userId) {
        return entityManager.getReference(AppUser.class, userId);
    }

    private Book bookReference(Long bookId) {
        return bookId == null ? null : entityManager.getReference(Book.class, bookId);
    }

    private String reservationMessage(String action, String actorUsername, String targetUsername, String bookTitle) {
        if (actorUsername.equals(targetUsername)) {
            return "%s %s reservation for \"%s\"".formatted(actorUsername, action, bookTitle);
        }
        return "%s %s reservation for \"%s\" held by %s".formatted(actorUsername, action, bookTitle, targetUsername);
    }

    private String fineWaivedMessage(FineWaivedEvent event) {
        String bookContext = event.bookTitle() != null ? "\"%s\"".formatted(event.bookTitle()) : "a manual fine";
        return "%s waived %s for %s on %s. Note: %s".formatted(
                event.actorUsername(),
                event.amount(),
                event.targetUsername(),
                bookContext,
                event.note());
    }

    private String returnMessage(BookReturnedEvent event) {
        if (event.actorUsername().equals(event.targetUsername())) {
            return "%s returned \"%s\"".formatted(event.actorUsername(), event.bookTitle());
        }
        return "%s returned \"%s\" for %s".formatted(
                event.actorUsername(),
                event.bookTitle(),
                event.targetUsername());
    }

    private String borrowMessage(BookBorrowedEvent event) {
        if (event.fromReadyReservation()) {
            if (event.actorUsername().equals(event.targetUsername())) {
                return "%s collected ready hold \"%s\"".formatted(event.actorUsername(), event.bookTitle());
            }
            return "%s checked out ready hold \"%s\" for %s".formatted(
                    event.actorUsername(),
                    event.bookTitle(),
                    event.targetUsername());
        }
        if (event.actorUsername().equals(event.targetUsername())) {
            return "%s borrowed \"%s\"".formatted(event.actorUsername(), event.bookTitle());
        }
        return "%s checked out \"%s\" for %s".formatted(
                event.actorUsername(),
                event.bookTitle(),
                event.targetUsername());
    }

    private String renewalMessage(BorrowingRenewedEvent event) {
        String base = "%s renewed \"%s\" for %s until %s".formatted(
                event.actorUsername(),
                event.bookTitle(),
                event.targetUsername(),
                event.newDueAt());
        if (!event.overrideApplied()) {
            return base;
        }
        return "%s with override. Reason: %s".formatted(base, event.reason());
    }

    private boolean isDuplicateViewedLogViolation(DataIntegrityViolationException exception) {
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(exception);
        return cause != null
                && cause.getMessage() != null
                && cause.getMessage().contains(VIEWED_ONCE_INDEX);
    }
}
