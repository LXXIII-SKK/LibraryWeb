package com.example.library.circulation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.example.library.catalog.Book;
import com.example.library.catalog.CatalogService;
import com.example.library.identity.AppUser;
import com.example.library.identity.AppUserRepository;
import com.example.library.identity.AppPermission;
import com.example.library.identity.AuthorizationService;
import com.example.library.identity.CurrentUser;
import com.example.library.identity.CurrentUserService;
import com.example.library.inventory.BookHolding;
import com.example.library.inventory.InventoryService;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CirculationService {

    private final BorrowTransactionRepository borrowTransactionRepository;
    private final CatalogService catalogService;
    private final AppUserRepository appUserRepository;
    private final CurrentUserService currentUserService;
    private final AuthorizationService authorizationService;
    private final PolicyService policyService;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final FineService fineService;
    private final InventoryService inventoryService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObservationRegistry observationRegistry;

    public CirculationService(
            BorrowTransactionRepository borrowTransactionRepository,
            CatalogService catalogService,
            AppUserRepository appUserRepository,
            CurrentUserService currentUserService,
            AuthorizationService authorizationService,
            PolicyService policyService,
            ReservationRepository reservationRepository,
            ReservationService reservationService,
            FineService fineService,
            InventoryService inventoryService,
            ApplicationEventPublisher applicationEventPublisher,
            ObservationRegistry observationRegistry) {
        this.borrowTransactionRepository = borrowTransactionRepository;
        this.catalogService = catalogService;
        this.appUserRepository = appUserRepository;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
        this.policyService = policyService;
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
        this.fineService = fineService;
        this.inventoryService = inventoryService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.observationRegistry = observationRegistry;
    }

    @Transactional
    public BorrowTransactionResponse borrow(BorrowBookRequest request) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        authorizationService.assertBorrowAllowed();

        return Observation.createNotStarted("circulation.borrow", observationRegistry)
                .lowCardinalityKeyValue("book.id", String.valueOf(request.bookId()))
                .lowCardinalityKeyValue("user.role", currentUser.role().name())
                .observe(() -> doBorrow(request, currentUser));
    }

    @Transactional
    public BorrowTransactionResponse staffCheckout(StaffCheckoutRequest request) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (!authorizationService.canCheckoutForMember()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "This role cannot check out books to members");
        }

        return Observation.createNotStarted("circulation.staff-checkout", observationRegistry)
                .lowCardinalityKeyValue("book.id", String.valueOf(request.bookId()))
                .lowCardinalityKeyValue("user.role", currentUser.role().name())
                .observe(() -> doStaffCheckout(request, currentUser));
    }

    @Transactional
    public BorrowTransactionResponse returnBook(Long transactionId) {
        CurrentUser currentUser = currentUserService.getCurrentUser();

        return Observation.createNotStarted("circulation.return", observationRegistry)
                .lowCardinalityKeyValue("transaction.id", String.valueOf(transactionId))
                .lowCardinalityKeyValue("user.role", currentUser.role().name())
                .observe(() -> doReturn(transactionId, currentUser));
    }

    @Transactional
    public BorrowTransactionResponse renewBorrowing(Long transactionId, RenewBorrowingRequest request) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return Observation.createNotStarted("circulation.renew", observationRegistry)
                .lowCardinalityKeyValue("transaction.id", String.valueOf(transactionId))
                .lowCardinalityKeyValue("user.role", currentUser.role().name())
                .observe(() -> doRenew(transactionId, currentUser, request));
    }

    @Transactional
    public BorrowTransactionResponse recordBorrowingException(Long transactionId, BorrowingExceptionRequest request) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return Observation.createNotStarted("circulation.exception", observationRegistry)
                .lowCardinalityKeyValue("transaction.id", String.valueOf(transactionId))
                .lowCardinalityKeyValue("exception.action", request.action().name())
                .lowCardinalityKeyValue("user.role", currentUser.role().name())
                .observe(() -> doRecordException(transactionId, currentUser, request));
    }

    public List<BorrowTransactionResponse> listForCurrentUser() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return borrowTransactionRepository.findAllByUserIdOrderByBorrowedAtDesc(currentUser.id()).stream()
                .map(BorrowTransactionResponse::from)
                .toList();
    }

    public List<BorrowTransactionResponse> listAll() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        List<BorrowTransaction> transactions = switch (currentUser.scope()) {
            case GLOBAL -> borrowTransactionRepository.findAllByOrderByBorrowedAtDesc();
            case BRANCH -> {
                if (!currentUser.hasPermission(AppPermission.REPORT_BRANCH_READ) || currentUser.branchId() == null) {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "Branch-scoped user requires a branch assignment");
                }
                yield borrowTransactionRepository.findAllByUser_Branch_IdOrderByBorrowedAtDesc(currentUser.branchId());
            }
            case SELF -> throw new org.springframework.security.access.AccessDeniedException(
                    "Self-scoped users cannot list operational borrowings");
        };
        return transactions.stream()
                .map(BorrowTransactionResponse::from)
                .toList();
    }

    private BorrowTransactionResponse doBorrow(BorrowBookRequest request, CurrentUser currentUser) {
        Book book = catalogService.findEntity(request.bookId());
        reservationService.assertNoReadyReservationBlocksDirectBorrow(currentUser.id(), book.getId());
        assertReservationQueueAllowsBorrowing(book.getId(), currentUser.id());
        BookHolding holding = inventoryService.resolveBorrowableHolding(book, request.holdingId());
        holding.borrowOne();
        inventoryService.synchronizeBookInventory(book);
        BorrowTransaction saved = createBorrowTransaction(
                currentUserService.getCurrentUserEntity(),
                book,
                holding,
                Instant.now());
        reservationService.markFulfilledForBookAndUser(book, saved.getUser());

        applicationEventPublisher.publishEvent(new BookBorrowedEvent(
                currentUser.id(),
                currentUser.username(),
                saved.getUser().getId(),
                saved.getUser().getUsername(),
                book.getId(),
                book.getTitle(),
                false,
                saved.getBorrowedAt()));

        return BorrowTransactionResponse.from(saved);
    }

    private BorrowTransactionResponse doReturn(Long transactionId, CurrentUser currentUser) {
        BorrowTransaction transaction = borrowTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Borrow transaction %d was not found".formatted(transactionId)));

        authorizationService.assertCanReturnBorrowingForUser(transaction.getUser());
        if (transaction.getStatus() == BorrowStatus.RETURNED) {
            throw new IllegalArgumentException("This book has already been returned");
        }
        if (!transaction.getStatus().canReturnToInventory()) {
            throw new IllegalArgumentException(
                    "Lost or damaged borrowings must be resolved through an item exception workflow");
        }

        if (transaction.getHolding() != null) {
            transaction.getHolding().returnOne();
            inventoryService.synchronizeBookInventory(transaction.getBook());
        } else {
            transaction.getBook().returnOne();
        }
        Instant returnedAt = Instant.now();
        transaction.markReturned(returnedAt);
        fineService.createOverdueFineIfRequired(transaction, returnedAt);

        applicationEventPublisher.publishEvent(new BookReturnedEvent(
                currentUser.id(),
                currentUser.username(),
                transaction.getBook().getId(),
                transaction.getBook().getTitle(),
                transaction.getUser().getUsername(),
                returnedAt));

        return BorrowTransactionResponse.from(transaction);
    }

    private BorrowTransactionResponse doStaffCheckout(StaffCheckoutRequest request, CurrentUser currentUser) {
        AppUser targetUser = currentUserService.getCurrentUserEntity();
        if (request.userId() != null && (targetUser.getId() == null || !targetUser.getId().equals(request.userId()))) {
            targetUser = appUserRepository.findById(request.userId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "User %d was not found".formatted(request.userId())));
        }
        authorizationService.assertCanReadUser(targetUser);
        if (currentUser.scope() == com.example.library.identity.AccessScope.BRANCH
                && currentUser.branchId() != null
                && !currentUser.belongsToBranch(targetUser.getBranchId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Branch-scoped staff can only check out books for users inside their branch");
        }
        if (targetUser.getRole() != com.example.library.identity.AppRole.MEMBER
                || targetUser.getAccountStatus() != com.example.library.identity.AccountStatus.ACTIVE
                || !targetUser.getMembershipStatus().allowsBorrowing()) {
            throw new IllegalArgumentException("Selected user is not eligible for borrowing");
        }

        Book book = catalogService.findEntity(request.bookId());
        Instant now = Instant.now();
        BorrowTransaction saved;
        if (request.reservationId() != null) {
            Reservation reservation = reservationRepository.findById(request.reservationId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Reservation %d was not found".formatted(request.reservationId())));
            if (reservation.getUser().getId() == null || !reservation.getUser().getId().equals(targetUser.getId())) {
                throw new IllegalArgumentException("Reservation does not belong to the selected user");
            }
            if (!reservation.getBook().getId().equals(book.getId())) {
                throw new IllegalArgumentException("Reservation does not belong to the selected book");
            }
            if (reservation.getStatus() != ReservationStatus.READY_FOR_PICKUP || reservation.getReservedHolding() == null) {
                throw new IllegalArgumentException("Only ready reservations can be checked out by staff");
            }
            saved = createBorrowTransaction(targetUser, book, reservation.getReservedHolding(), now);
            reservation.fulfill(now);
        } else {
            reservationService.assertNoReadyReservationBlocksDirectBorrow(targetUser.getId(), book.getId());
            BookHolding holding = resolveStaffCheckoutHolding(book, request, currentUser);
            holding.borrowOne();
            inventoryService.synchronizeBookInventory(book);
            saved = createBorrowTransaction(targetUser, book, holding, now);
            reservationService.markFulfilledForBookAndUser(book, targetUser);
        }

        applicationEventPublisher.publishEvent(new BookBorrowedEvent(
                currentUser.id(),
                currentUser.username(),
                saved.getUser().getId(),
                saved.getUser().getUsername(),
                book.getId(),
                book.getTitle(),
                request.reservationId() != null,
                saved.getBorrowedAt()));

        return BorrowTransactionResponse.from(saved);
    }

    private BorrowTransactionResponse doRenew(Long transactionId, CurrentUser currentUser, RenewBorrowingRequest request) {
        BorrowTransaction transaction = borrowTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Borrow transaction %d was not found".formatted(transactionId)));
        authorizationService.assertCanRenewBorrowingForUser(transaction.getUser());
        if (!transaction.getStatus().isRenewable()) {
            throw new IllegalArgumentException("Only borrowed items can be renewed");
        }

        LibraryPolicy policy = policyService.getCurrentPolicyEntity();
        boolean privilegedActor = currentUser.role() == com.example.library.identity.AppRole.ADMIN
                || (currentUser.hasPermission(AppPermission.LOAN_OVERRIDE_BRANCH)
                        && currentUser.branchId() != null
                        && currentUser.belongsToBranch(transaction.getUser().getBranchId()));
        boolean actorOwnsBorrowing = transaction.getUser().getId() != null
                && transaction.getUser().getId().equals(currentUser.id());
        boolean reachedRenewalLimit = transaction.getRenewalCount() >= policy.getMaxRenewals();
        boolean hasBlockingReservations = !policy.isAllowRenewalWithActiveReservations()
                && reservationRepository.existsByBookIdAndStatusAndUserIdNot(
                        transaction.getBook().getId(),
                        ReservationStatus.ACTIVE,
                        transaction.getUser().getId());
        boolean customDueDateRequested = request != null && request.dueAt() != null;
        boolean overrideApplied = !actorOwnsBorrowing || customDueDateRequested || reachedRenewalLimit || hasBlockingReservations;

        if (!privilegedActor) {
            if (reachedRenewalLimit) {
                throw new IllegalArgumentException("This borrowing has reached the renewal limit");
            }
            if (hasBlockingReservations) {
                throw new IllegalArgumentException("This borrowing cannot be renewed while other reservations are active");
            }
            if (customDueDateRequested) {
                throw new IllegalArgumentException("Only staff overrides can set a manual due date");
            }
        } else if (overrideApplied && (request == null || request.reason() == null || request.reason().isBlank())) {
            throw new IllegalArgumentException("Override renewals require a reason");
        }

        Instant baseDueAt = transaction.getDueAt().isAfter(Instant.now()) ? transaction.getDueAt() : Instant.now();
        Instant nextDueAt = privilegedActor && customDueDateRequested
                ? request.dueAt()
                : baseDueAt.plus(policy.getRenewalDays(), ChronoUnit.DAYS);
        if (!nextDueAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("Renewed due date must be in the future");
        }

        String renewalReason = overrideApplied && request != null && request.reason() != null
                ? request.reason().trim()
                : null;
        transaction.renewTo(nextDueAt, Instant.now(), overrideApplied, renewalReason);
        applicationEventPublisher.publishEvent(new BorrowingRenewedEvent(
                currentUser.id(),
                currentUser.username(),
                transaction.getBook().getId(),
                transaction.getBook().getTitle(),
                transaction.getUser().getUsername(),
                transaction.getDueAt(),
                overrideApplied,
                renewalReason,
                transaction.getLastRenewedAt()));
        return BorrowTransactionResponse.from(transaction);
    }

    private BorrowTransactionResponse doRecordException(
            Long transactionId,
            CurrentUser currentUser,
            BorrowingExceptionRequest request) {
        BorrowTransaction transaction = borrowTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Borrow transaction %d was not found".formatted(transactionId)));
        authorizationService.assertCanManageBorrowingExceptionsForUser(transaction.getUser());

        String note = request.note().trim();
        if (note.isEmpty()) {
            throw new IllegalArgumentException("Item exception notes cannot be blank");
        }

        transaction.recordException(request.action().status(), note, Instant.now());
        applicationEventPublisher.publishEvent(new BorrowingExceptionRecordedEvent(
                currentUser.id(),
                currentUser.username(),
                transaction.getBook().getId(),
                transaction.getBook().getTitle(),
                transaction.getUser().getUsername(),
                transaction.getStatus(),
                note,
                transaction.getExceptionRecordedAt()));
        return BorrowTransactionResponse.from(transaction);
    }

    private void assertReservationQueueAllowsBorrowing(Long bookId, Long currentUserId) {
        reservationRepository.findFirstByBookIdAndStatusOrderByReservedAtAsc(bookId, ReservationStatus.ACTIVE)
                .filter(reservation -> reservation.getUser().getId() != null)
                .filter(reservation -> !reservation.getUser().getId().equals(currentUserId))
                .ifPresent(reservation -> {
                    throw new IllegalArgumentException("This title is currently reserved for another member");
                });
    }

    private BorrowTransaction createBorrowTransaction(AppUser user, Book book, BookHolding holding, Instant borrowedAt) {
        LibraryPolicy policy = policyService.getCurrentPolicyEntity();
        BorrowTransaction transaction = new BorrowTransaction(
                user,
                book,
                holding,
                borrowedAt,
                borrowedAt.plus(policy.getStandardLoanDays(), ChronoUnit.DAYS));
        return borrowTransactionRepository.save(transaction);
    }

    private BookHolding resolveStaffCheckoutHolding(Book book, StaffCheckoutRequest request, CurrentUser currentUser) {
        if (request.holdingId() != null) {
            BookHolding holding = inventoryService.resolveBorrowableHolding(book, request.holdingId());
            if (currentUser.scope() == com.example.library.identity.AccessScope.BRANCH
                    && currentUser.branchId() != null
                    && (holding.getBranch() == null || !currentUser.branchId().equals(holding.getBranch().getId()))) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Branch-scoped staff can only check out holdings from their branch");
            }
            return holding;
        }

        List<BookHolding> holdings = inventoryService.listBorrowableHoldings(book).stream()
                .filter(holding -> currentUser.scope() != com.example.library.identity.AccessScope.BRANCH
                        || currentUser.branchId() == null
                        || (holding.getBranch() != null && currentUser.branchId().equals(holding.getBranch().getId())))
                .toList();
        if (holdings.isEmpty()) {
            throw new IllegalArgumentException("No borrowable holding exists for the selected book in this scope");
        }
        if (holdings.size() > 1) {
            throw new IllegalArgumentException("Multiple holdings are available. Select a specific holding for checkout.");
        }
        return holdings.get(0);
    }
}
