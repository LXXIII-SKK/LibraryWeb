package com.example.library.circulation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import com.example.library.branch.BranchService;
import com.example.library.branch.LibraryBranch;
import com.example.library.catalog.Book;
import com.example.library.catalog.CatalogService;
import com.example.library.identity.AccessScope;
import com.example.library.identity.AppPermission;
import com.example.library.identity.AppUser;
import com.example.library.identity.AuthorizationService;
import com.example.library.identity.CurrentUser;
import com.example.library.identity.CurrentUserService;
import com.example.library.inventory.BookHolding;
import com.example.library.inventory.HoldingFormat;
import com.example.library.inventory.InventoryService;
import com.example.library.notification.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BorrowTransactionRepository borrowTransactionRepository;
    private final CatalogService catalogService;
    private final BranchService branchService;
    private final CurrentUserService currentUserService;
    private final AuthorizationService authorizationService;
    private final PolicyService policyService;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ReservationService(
            ReservationRepository reservationRepository,
            BorrowTransactionRepository borrowTransactionRepository,
            CatalogService catalogService,
            BranchService branchService,
            CurrentUserService currentUserService,
            AuthorizationService authorizationService,
            PolicyService policyService,
            InventoryService inventoryService,
            NotificationService notificationService,
            ApplicationEventPublisher applicationEventPublisher) {
        this.reservationRepository = reservationRepository;
        this.borrowTransactionRepository = borrowTransactionRepository;
        this.catalogService = catalogService;
        this.branchService = branchService;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
        this.policyService = policyService;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public ReservationResponse createForCurrentUser(CreateReservationRequest request) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (!authorizationService.canCreateReservationForSelf()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "This account cannot create reservations");
        }

        if (reservationRepository.existsByUserIdAndBookIdAndStatus(currentUser.id(), request.bookId(), ReservationStatus.ACTIVE)) {
            throw new IllegalArgumentException("An active reservation already exists for this book");
        }
        if (borrowTransactionRepository.existsByUserIdAndBookIdAndStatus(currentUser.id(), request.bookId(), BorrowStatus.BORROWED)) {
            throw new IllegalArgumentException("This user already has an active borrowing for the selected book");
        }

        Book book = catalogService.findEntity(request.bookId());
        AppUser user = currentUserService.getCurrentUserEntity();
        LibraryBranch pickupBranch = resolvePickupBranch(currentUser, request.pickupBranchId());
        Reservation reservation = reservationRepository.save(new Reservation(user, book, pickupBranch, Instant.now()));
        applicationEventPublisher.publishEvent(new ReservationCreatedEvent(
                currentUser.id(),
                currentUser.username(),
                book.getId(),
                book.getTitle(),
                reservation.getReservedAt()));
        return ReservationResponse.from(reservation);
    }

    public List<ReservationResponse> listForCurrentUser() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return reservationRepository.findAllByUserIdOrderByReservedAtDesc(currentUser.id()).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    public List<ReservationResponse> listAll() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        List<Reservation> reservations = switch (currentUser.scope()) {
            case GLOBAL -> reservationRepository.findAllByOrderByReservedAtDesc();
            case BRANCH -> {
                if (currentUser.branchId() == null || !currentUser.hasPermission(AppPermission.RESERVATION_MANAGE_BRANCH)) {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "Branch-scoped user requires reservation-management permission");
                }
                yield reservationRepository.findAllByUser_Branch_IdOrderByReservedAtDesc(currentUser.branchId());
            }
            case SELF -> throw new org.springframework.security.access.AccessDeniedException(
                    "Self-scoped users cannot list operational reservations");
        };
        return reservations.stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @Transactional
    public ReservationResponse cancel(Long reservationId) {
        Reservation reservation = findReservation(reservationId);
        CurrentUser currentUser = currentUserService.getCurrentUser();
        authorizationService.assertCanManageReservationForUser(reservation.getUser());
        if (!Set.of(ReservationStatus.ACTIVE, ReservationStatus.IN_TRANSIT, ReservationStatus.READY_FOR_PICKUP)
                .contains(reservation.getStatus())) {
            throw new IllegalArgumentException("Only open reservations can be cancelled");
        }
        releaseReservedHoldingIfPresent(reservation);
        reservation.cancel(Instant.now());
        applicationEventPublisher.publishEvent(new ReservationCancelledEvent(
                currentUser.id(),
                currentUser.username(),
                reservation.getBook().getId(),
                reservation.getBook().getTitle(),
                reservation.getUser().getUsername(),
                reservation.getUpdatedAt()));
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse markNoShow(Long reservationId) {
        Reservation reservation = findReservation(reservationId);
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (!authorizationService.canManageReservations()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "This role cannot manage reservation operations");
        }
        if (currentUser.scope() == AccessScope.BRANCH
                && currentUser.branchId() != null
                && !currentUser.belongsToBranch(reservation.getUser().getBranchId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Branch-scoped staff cannot manage reservations outside their branch");
        }
        if (!Set.of(ReservationStatus.ACTIVE, ReservationStatus.READY_FOR_PICKUP).contains(reservation.getStatus())) {
            throw new IllegalArgumentException("Only active or ready reservations can be marked as no-show");
        }
        releaseReservedHoldingIfPresent(reservation);
        reservation.markNoShow(Instant.now());
        applicationEventPublisher.publishEvent(new ReservationNoShowEvent(
                currentUser.id(),
                currentUser.username(),
                reservation.getBook().getId(),
                reservation.getBook().getTitle(),
                reservation.getUser().getUsername(),
                reservation.getUpdatedAt()));
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse prepareForPickup(Long reservationId, PrepareReservationRequest request) {
        Reservation reservation = findReservation(reservationId);
        CurrentUser currentUser = currentUserService.getCurrentUser();
        assertCanManageReservation(reservation, currentUser);
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active reservations can be prepared for pickup");
        }

        BookHolding holding = request != null && request.holdingId() != null
                ? inventoryService.resolveBorrowableHolding(reservation.getBook(), request.holdingId())
                : selectFulfillmentHolding(reservation.getBook(), reservation.getPickupBranch());
        holding.borrowOne();
        inventoryService.synchronizeBookInventory(reservation.getBook());

        Instant now = Instant.now();
        if (holding.getFormat() == HoldingFormat.DIGITAL
                || reservation.getPickupBranch() == null
                || reservation.getPickupBranch().getId().equals(holding.getBranch().getId())) {
            reservation.markReadyForPickup(holding, now, now.plus(3, ChronoUnit.DAYS));
            notificationService.notifyUser(
                    reservation.getUser(),
                    "Reservation ready for pickup",
                    "\"%s\" is ready for pickup at %s.".formatted(
                            reservation.getBook().getTitle(),
                            reservation.getPickupBranch() != null
                                    ? reservation.getPickupBranch().getName()
                                    : holding.getBranch().getName()),
                    reservation.getPickupBranch(),
                    currentUserService.getCurrentUserEntity());
        } else {
            reservation.beginTransfer(holding, now);
            notificationService.notifyUser(
                    reservation.getUser(),
                    "Reservation in transit",
                    "\"%s\" is being transferred to %s for pickup.".formatted(
                            reservation.getBook().getTitle(),
                            reservation.getPickupBranch().getName()),
                    reservation.getPickupBranch(),
                    currentUserService.getCurrentUserEntity());
        }
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse markReadyForPickup(Long reservationId) {
        Reservation reservation = findReservation(reservationId);
        CurrentUser currentUser = currentUserService.getCurrentUser();
        assertCanManageReservation(reservation, currentUser);
        if (reservation.getStatus() != ReservationStatus.IN_TRANSIT || reservation.getReservedHolding() == null) {
            throw new IllegalArgumentException("Only in-transit reservations with a reserved holding can be marked ready");
        }
        Instant now = Instant.now();
        reservation.markReadyForPickup(reservation.getReservedHolding(), now, now.plus(3, ChronoUnit.DAYS));
        notificationService.notifyUser(
                reservation.getUser(),
                "Reservation ready for pickup",
                "\"%s\" is ready for pickup at %s.".formatted(
                        reservation.getBook().getTitle(),
                        reservation.getPickupBranch() != null
                                ? reservation.getPickupBranch().getName()
                                : reservation.getReservedHolding().getBranch().getName()),
                reservation.getPickupBranch(),
                currentUserService.getCurrentUserEntity());
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse expire(Long reservationId) {
        Reservation reservation = findReservation(reservationId);
        CurrentUser currentUser = currentUserService.getCurrentUser();
        assertCanManageReservation(reservation, currentUser);
        if (!Set.of(ReservationStatus.ACTIVE, ReservationStatus.IN_TRANSIT, ReservationStatus.READY_FOR_PICKUP)
                .contains(reservation.getStatus())) {
            throw new IllegalArgumentException("Only open reservations can be expired");
        }
        releaseReservedHoldingIfPresent(reservation);
        reservation.expire(Instant.now());
        notificationService.notifyUser(
                reservation.getUser(),
                "Reservation expired",
                "\"%s\" expired before pickup.".formatted(reservation.getBook().getTitle()),
                reservation.getPickupBranch(),
                currentUserService.getCurrentUserEntity());
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public BorrowTransactionResponse collectForCurrentUser(Long reservationId) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        authorizationService.assertBorrowAllowed();
        Reservation reservation = findReservation(reservationId);
        if (reservation.getUser().getId() == null || !reservation.getUser().getId().equals(currentUser.id())) {
            throw new org.springframework.security.access.AccessDeniedException("This reservation belongs to another user");
        }
        if (reservation.getStatus() != ReservationStatus.READY_FOR_PICKUP || reservation.getReservedHolding() == null) {
            throw new IllegalArgumentException("Only ready reservations can be collected");
        }
        if (reservation.getExpiresAt() != null && reservation.getExpiresAt().isBefore(Instant.now())) {
            releaseReservedHoldingIfPresent(reservation);
            reservation.expire(Instant.now());
            notificationService.notifyUser(
                    reservation.getUser(),
                    "Reservation expired",
                    "\"%s\" expired before pickup.".formatted(reservation.getBook().getTitle()),
                    reservation.getPickupBranch(),
                    currentUserService.getCurrentUserEntity());
            throw new IllegalArgumentException("This reservation has already expired");
        }

        Instant borrowedAt = Instant.now();
        LibraryPolicy policy = policyService.getCurrentPolicyEntity();
        BorrowTransaction saved = borrowTransactionRepository.save(new BorrowTransaction(
                currentUserService.getCurrentUserEntity(),
                reservation.getBook(),
                reservation.getReservedHolding(),
                borrowedAt,
                borrowedAt.plus(policy.getStandardLoanDays(), ChronoUnit.DAYS)));
        reservation.fulfill(borrowedAt);
        applicationEventPublisher.publishEvent(new BookBorrowedEvent(
                currentUser.id(),
                currentUser.username(),
                saved.getUser().getId(),
                saved.getUser().getUsername(),
                reservation.getBook().getId(),
                reservation.getBook().getTitle(),
                true,
                saved.getBorrowedAt()));
        return BorrowTransactionResponse.from(saved);
    }

    @Transactional
    public void markFulfilledForBookAndUser(Book book, AppUser user) {
        reservationRepository.findAllByUserIdAndBookIdAndStatusInOrderByReservedAtAsc(
                        user.getId(),
                        book.getId(),
                        List.of(
                                ReservationStatus.ACTIVE,
                                ReservationStatus.IN_TRANSIT,
                                ReservationStatus.READY_FOR_PICKUP))
                .stream()
                .findFirst()
                .ifPresent(reservation -> reservation.fulfill(Instant.now()));
    }

    public void assertNoReadyReservationBlocksDirectBorrow(Long userId, Long bookId) {
        reservationRepository.findAllByUserIdAndBookIdAndStatusInOrderByReservedAtAsc(
                        userId,
                        bookId,
                        List.of(ReservationStatus.IN_TRANSIT, ReservationStatus.READY_FOR_PICKUP))
                .stream()
                .findFirst()
                .ifPresent(reservation -> {
                    throw new IllegalArgumentException(
                            reservation.getStatus() == ReservationStatus.IN_TRANSIT
                                    ? "This title is already reserved for transfer. Wait until it is ready for pickup."
                                    : "This title already has a ready reservation. Collect it from your account page.");
                });
    }

    private Reservation findReservation(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Reservation %d was not found".formatted(reservationId)));
    }

    private LibraryBranch resolvePickupBranch(CurrentUser currentUser, Long requestedBranchId) {
        Long fallbackBranchId = requestedBranchId != null ? requestedBranchId
                : currentUser.homeBranchId() != null ? currentUser.homeBranchId() : currentUser.branchId();
        return branchService.resolveBranch(fallbackBranchId);
    }

    private void assertCanManageReservation(Reservation reservation, CurrentUser currentUser) {
        if (!authorizationService.canManageReservations()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "This role cannot manage reservation operations");
        }
        if (currentUser.scope() == AccessScope.BRANCH
                && currentUser.branchId() != null
                && !currentUser.belongsToBranch(reservation.getUser().getBranchId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Branch-scoped staff cannot manage reservations outside their branch");
        }
    }

    private BookHolding selectFulfillmentHolding(Book book, LibraryBranch pickupBranch) {
        List<BookHolding> borrowableHoldings = inventoryService.listBorrowableHoldings(book);
        if (borrowableHoldings.isEmpty()) {
            throw new IllegalArgumentException("No available holding exists for this reservation");
        }
        if (pickupBranch != null) {
            return borrowableHoldings.stream()
                    .filter(holding -> holding.getBranch() != null)
                    .filter(holding -> pickupBranch.getId().equals(holding.getBranch().getId()))
                    .findFirst()
                    .orElse(borrowableHoldings.get(0));
        }
        return borrowableHoldings.get(0);
    }

    private void releaseReservedHoldingIfPresent(Reservation reservation) {
        if (reservation.getReservedHolding() != null
                && Set.of(ReservationStatus.IN_TRANSIT, ReservationStatus.READY_FOR_PICKUP).contains(reservation.getStatus())) {
            reservation.getReservedHolding().returnOne();
            inventoryService.synchronizeBookInventory(reservation.getBook());
        }
    }
}
