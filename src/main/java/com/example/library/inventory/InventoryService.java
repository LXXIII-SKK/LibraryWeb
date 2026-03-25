package com.example.library.inventory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import com.example.library.branch.BranchService;
import com.example.library.branch.LibraryBranch;
import com.example.library.catalog.Book;
import com.example.library.catalog.CatalogService;
import com.example.library.common.OperationalActivityEvent;
import com.example.library.circulation.BorrowStatus;
import com.example.library.circulation.BorrowTransaction;
import com.example.library.circulation.BorrowTransactionRepository;
import com.example.library.identity.AuthorizationService;
import com.example.library.identity.CurrentUser;
import com.example.library.identity.CurrentUserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InventoryService {

    private static final EnumSet<BookCopyStatus> REMOVABLE_COPY_STATUSES =
            EnumSet.of(BookCopyStatus.AVAILABLE, BookCopyStatus.UNAVAILABLE);

    private final BookHoldingRepository bookHoldingRepository;
    private final BookCopyRepository bookCopyRepository;
    private final CatalogService catalogService;
    private final BranchService branchService;
    private final LocationService locationService;
    private final CurrentUserService currentUserService;
    private final AuthorizationService authorizationService;
    private final BorrowTransactionRepository borrowTransactionRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public InventoryService(
            BookHoldingRepository bookHoldingRepository,
            BookCopyRepository bookCopyRepository,
            CatalogService catalogService,
            BranchService branchService,
            LocationService locationService,
            CurrentUserService currentUserService,
            AuthorizationService authorizationService,
            BorrowTransactionRepository borrowTransactionRepository,
            ApplicationEventPublisher applicationEventPublisher) {
        this.bookHoldingRepository = bookHoldingRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.catalogService = catalogService;
        this.branchService = branchService;
        this.locationService = locationService;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
        this.borrowTransactionRepository = borrowTransactionRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public List<BookHoldingResponse> listManagedHoldings() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        List<BookHolding> holdings = switch (currentUser.scope()) {
            case GLOBAL -> bookHoldingRepository.findAllByOrderByBook_TitleAscBranch_NameAscIdAsc();
            case BRANCH -> {
                if (!authorizationService.canManageInventory() || currentUser.branchId() == null) {
                    throw new AccessDeniedException("Branch-scoped staff require inventory permissions");
                }
                yield bookHoldingRepository.findAllByBranch_IdOrderByBook_TitleAscIdAsc(currentUser.branchId());
            }
            case SELF -> throw new AccessDeniedException("This role cannot manage inventory holdings");
        };
        holdings.stream()
                .filter(holding -> holding.getFormat() == HoldingFormat.PHYSICAL)
                .forEach(this::synchronizeHoldingInventoryFromCopies);
        return holdings.stream()
                .map(BookHoldingResponse::from)
                .toList();
    }

    public List<BookCopyResponse> listManagedCopies() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        List<BookCopy> copies = switch (currentUser.scope()) {
            case GLOBAL -> bookCopyRepository.findAllByOrderByHolding_Book_TitleAscBarcodeAsc();
            case BRANCH -> {
                if (!authorizationService.canManageInventory() || currentUser.branchId() == null) {
                    throw new AccessDeniedException("Branch-scoped staff require inventory permissions");
                }
                yield bookCopyRepository.findAllByCurrentBranch_IdOrderByHolding_Book_TitleAscBarcodeAsc(currentUser.branchId());
            }
            case SELF -> throw new AccessDeniedException("This role cannot manage inventory copies");
        };
        return copies.stream()
                .map(BookCopyResponse::from)
                .toList();
    }

    public BookHolding findHolding(Long holdingId) {
        return bookHoldingRepository.findById(holdingId)
                .orElseThrow(() -> new EntityNotFoundException("Holding %d was not found".formatted(holdingId)));
    }

    public BookCopy findCopy(Long copyId) {
        return bookCopyRepository.findById(copyId)
                .orElseThrow(() -> new EntityNotFoundException("Copy %d was not found".formatted(copyId)));
    }

    public BookHolding resolveBorrowableHolding(Book book, Long requestedHoldingId) {
        if (requestedHoldingId != null) {
            BookHolding holding = findHolding(requestedHoldingId);
            if (!holding.getBook().getId().equals(book.getId())) {
                throw new IllegalArgumentException("Selected holding does not belong to the requested book");
            }
            if (holding.getFormat() == HoldingFormat.PHYSICAL) {
                synchronizeHoldingInventoryFromCopies(holding);
            }
            if (!holding.isBorrowable()) {
                throw new IllegalArgumentException("Selected holding is not currently available");
            }
            return holding;
        }

        List<BookHolding> borrowableHoldings = bookHoldingRepository.findAllByBook_IdOrderByFormatAscBranch_NameAscIdAsc(book.getId()).stream()
                .peek(holding -> {
                    if (holding.getFormat() == HoldingFormat.PHYSICAL) {
                        synchronizeHoldingInventoryFromCopies(holding);
                    }
                })
                .filter(BookHolding::isBorrowable)
                .toList();
        if (borrowableHoldings.isEmpty()) {
            throw new IllegalArgumentException("No available holding exists for this title");
        }
        if (borrowableHoldings.size() > 1) {
            throw new IllegalArgumentException("Multiple availability options exist. Open the detail page and choose a location or digital option.");
        }
        return borrowableHoldings.get(0);
    }

    public List<BookHolding> listBorrowableHoldings(Book book) {
        return bookHoldingRepository.findAllByBook_IdOrderByFormatAscBranch_NameAscIdAsc(book.getId()).stream()
                .peek(holding -> {
                    if (holding.getFormat() == HoldingFormat.PHYSICAL) {
                        synchronizeHoldingInventoryFromCopies(holding);
                    }
                })
                .filter(BookHolding::isBorrowable)
                .toList();
    }

    @Transactional
    public BookHoldingResponse create(BookHoldingUpsertRequest request) {
        Book book = catalogService.findEntity(request.bookId());
        LibraryBranch branch = branchService.resolveBranch(request.branchId());
        authorizationService.assertCanManageBranchInventory(branch.getId());
        LibraryLocation location = resolveLocation(branch, request.locationId(), request.format());

        BookHolding holding = bookHoldingRepository.save(new BookHolding(
                book,
                branch,
                location,
                request.format(),
                request.totalQuantity(),
                request.availableQuantity(),
                request.accessUrl(),
                request.active()));
        if (request.format() == HoldingFormat.PHYSICAL) {
            reconcilePhysicalCopies(holding, request.totalQuantity(), request.availableQuantity());
        }
        synchronizeBookInventory(book);
        publishHoldingActivity("created holding", null, holding);
        return BookHoldingResponse.from(holding);
    }

    @Transactional
    public BookHoldingResponse update(Long holdingId, BookHoldingUpsertRequest request) {
        BookHolding holding = findHolding(holdingId);
        String beforeState = describe(holding);
        authorizationService.assertCanManageBranchInventory(holding.getBranch().getId());
        LibraryBranch branch = branchService.resolveBranch(request.branchId());
        authorizationService.assertCanManageBranchInventory(branch.getId());
        LibraryLocation location = resolveLocation(branch, request.locationId(), request.format());

        if (holding.getFormat() != request.format() && bookCopyRepository.countByHolding_Id(holding.getId()) > 0) {
            throw new IllegalArgumentException("Cannot change format for a holding that already has tracked copies");
        }
        assertTrackedCopiesCanBeRelocated(holding, branch, location, request.format());

        holding.update(
                branch,
                location,
                request.format(),
                request.totalQuantity(),
                request.availableQuantity(),
                request.accessUrl(),
                request.active());
        if (request.format() == HoldingFormat.PHYSICAL) {
            reconcilePhysicalCopies(holding, request.totalQuantity(), request.availableQuantity());
        }
        synchronizeBookInventory(holding.getBook());
        publishHoldingActivity("updated holding", beforeState, holding);
        return BookHoldingResponse.from(holding);
    }

    @Transactional
    public BookCopy checkoutCopy(BookHolding holding) {
        BookCopy copy = selectAvailableCopy(holding);
        copy.markBorrowed();
        synchronizeBookInventory(holding.getBook());
        return copy;
    }

    @Transactional
    public BookCopy reserveCopyForPickup(BookHolding holding, LibraryBranch pickupBranch) {
        BookCopy copy = selectAvailableCopy(holding);
        copy.markReadyForPickup(pickupBranch != null ? pickupBranch : holding.getBranch());
        synchronizeBookInventory(holding.getBook());
        return copy;
    }

    @Transactional
    public BookCopy startTransfer(BookHolding holding, LibraryBranch destinationBranch) {
        BookCopy copy = selectAvailableCopy(holding);
        copy.markInTransit(destinationBranch);
        synchronizeBookInventory(holding.getBook());
        return copy;
    }

    @Transactional
    public void markCopyReadyForPickup(BookCopy copy, LibraryBranch pickupBranch) {
        copy.markReadyForPickup(pickupBranch);
        synchronizeBookInventory(copy.getHolding().getBook());
    }

    @Transactional
    public void releaseReservedCopy(BookCopy copy) {
        copy.markAvailableAtHome();
        synchronizeBookInventory(copy.getHolding().getBook());
    }

    @Transactional
    public void returnCopy(BookCopy copy) {
        copy.markAvailableAtHome();
        synchronizeBookInventory(copy.getHolding().getBook());
    }

    @Transactional
    public void returnLegacyPhysicalCopy(BookHolding holding) {
        initializePhysicalCopiesFromAggregateIfMissing(holding);
        BookCopy copy = bookCopyRepository.findAllByHolding_IdAndStatusInOrderByBarcodeAsc(
                        holding.getId(),
                        List.of(BookCopyStatus.UNAVAILABLE))
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No tracked unavailable copy could be restored for holding %d".formatted(holding.getId())));
        copy.markAvailableAtHome();
        synchronizeBookInventory(holding.getBook());
    }

    @Transactional
    public void applyBorrowingException(BookCopy copy, BorrowStatus status) {
        switch (status) {
            case CLAIMED_RETURNED -> copy.markClaimedReturned();
            case LOST -> copy.markLost();
            case DAMAGED -> copy.markDamaged();
            default -> throw new IllegalArgumentException("Unsupported copy exception status " + status);
        }
        synchronizeBookInventory(copy.getHolding().getBook());
    }

    @Transactional
    public void synchronizeBookInventory(Book book) {
        List<BookHolding> holdings = bookHoldingRepository.findAllByBook_IdOrderByFormatAscBranch_NameAscIdAsc(book.getId());
        for (BookHolding holding : holdings) {
            if (holding.getFormat() == HoldingFormat.PHYSICAL) {
                synchronizeHoldingInventoryFromCopies(holding);
            }
        }
        int total = holdings.stream()
                .filter(BookHolding::isActive)
                .mapToInt(BookHolding::getTotalQuantity)
                .sum();
        int available = holdings.stream()
                .filter(BookHolding::isActive)
                .mapToInt(BookHolding::getAvailableQuantity)
                .sum();
        book.synchronizeInventory(total, available);
    }

    public DigitalAccessResponse digitalAccessForTransaction(Long transactionId) {
        BorrowTransaction transaction = borrowTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Borrow transaction %d was not found".formatted(transactionId)));
        authorizationService.assertCanReadBorrowingForUser(transaction.getUser());
        if (transaction.getStatus() != BorrowStatus.BORROWED) {
            throw new IllegalArgumentException("Digital access is only available for active borrowings");
        }
        BookHolding holding = transaction.getHolding();
        if (holding == null || holding.getFormat() != HoldingFormat.DIGITAL || holding.getAccessUrl() == null) {
            throw new IllegalArgumentException("This borrowing does not provide online access");
        }
        return new DigitalAccessResponse(
                transaction.getId(),
                transaction.getBook().getId(),
                transaction.getBook().getTitle(),
                holding.getAccessUrl());
    }

    private LibraryLocation resolveLocation(LibraryBranch branch, Long locationId, HoldingFormat format) {
        if (locationId == null) {
            if (format == HoldingFormat.PHYSICAL) {
                throw new IllegalArgumentException("Physical holdings require a location");
            }
            return null;
        }

        LibraryLocation location = locationService.findEntity(locationId);
        if (!location.getBranch().getId().equals(branch.getId())) {
            throw new IllegalArgumentException("Location must belong to the selected branch");
        }
        return location;
    }

    private void reconcilePhysicalCopies(BookHolding holding, int targetTotal, int targetAvailable) {
        initializePhysicalCopiesFromAggregateIfMissing(holding);
        List<BookCopy> copies = new ArrayList<>(bookCopyRepository.findAllByHolding_IdOrderByBarcodeAsc(holding.getId()));

        if (targetTotal < 0 || targetAvailable < 0 || targetAvailable > targetTotal) {
            throw new IllegalArgumentException("Physical holding quantities are invalid");
        }

        if (targetTotal < copies.size()) {
            int removalsNeeded = copies.size() - targetTotal;
            List<BookCopy> removableCopies = copies.stream()
                    .filter(copy -> copy.getStatus().canBeRemovedFromHolding())
                    .sorted(Comparator.comparing(BookCopy::getBarcode).reversed())
                    .limit(removalsNeeded)
                    .toList();
            if (removableCopies.size() < removalsNeeded) {
                throw new IllegalArgumentException("Reduce active reservations or borrowings before shrinking this holding");
            }
            bookCopyRepository.deleteAll(removableCopies);
            copies.removeAll(removableCopies);
        }

        if (targetTotal > copies.size()) {
            for (int index = copies.size(); index < targetTotal; index++) {
                BookCopy copy = new BookCopy(holding, nextBarcode(holding), BookCopyStatus.AVAILABLE);
                copies.add(bookCopyRepository.save(copy));
            }
        }

        long currentAvailable = copies.stream()
                .filter(BookCopy::isAvailable)
                .count();
        if (targetAvailable > currentAvailable) {
            List<BookCopy> unavailableCopies = copies.stream()
                    .filter(copy -> copy.getStatus() == BookCopyStatus.UNAVAILABLE)
                    .limit(targetAvailable - currentAvailable)
                    .toList();
            if (unavailableCopies.size() < targetAvailable - currentAvailable) {
                throw new IllegalArgumentException("Not enough unavailable copies exist to increase shelf availability");
            }
            unavailableCopies.forEach(BookCopy::markAvailableAtHome);
        } else if (targetAvailable < currentAvailable) {
            List<BookCopy> availableCopies = copies.stream()
                    .filter(copy -> copy.getStatus() == BookCopyStatus.AVAILABLE)
                    .sorted(Comparator.comparing(BookCopy::getBarcode).reversed())
                    .limit(currentAvailable - targetAvailable)
                    .toList();
            if (availableCopies.size() < currentAvailable - targetAvailable) {
                throw new IllegalArgumentException("Not enough available copies exist to lower shelf availability");
            }
            availableCopies.forEach(BookCopy::markUnavailable);
        }

        copies.forEach(BookCopy::rebaseHomeLocation);
        synchronizeHoldingInventoryFromCopies(holding);
    }

    private void assertTrackedCopiesCanBeRelocated(
            BookHolding holding,
            LibraryBranch targetBranch,
            LibraryLocation targetLocation,
            HoldingFormat targetFormat) {
        if (holding.getFormat() != HoldingFormat.PHYSICAL || targetFormat != HoldingFormat.PHYSICAL) {
            return;
        }
        if (!holdingBranchChanged(holding, targetBranch) && !holdingLocationChanged(holding, targetLocation)) {
            return;
        }

        boolean hasActiveTrackedCopies = bookCopyRepository.findAllByHolding_IdOrderByBarcodeAsc(holding.getId()).stream()
                .anyMatch(copy -> !copy.isMutableForManualInventory());
        if (hasActiveTrackedCopies) {
            throw new IllegalArgumentException(
                    "Cannot move a physical holding while tracked copies are borrowed, reserved, or in transit");
        }
    }

    private BookCopy selectAvailableCopy(BookHolding holding) {
        if (holding.getFormat() != HoldingFormat.PHYSICAL) {
            throw new IllegalArgumentException("Copy-level allocation only applies to physical holdings");
        }
        initializePhysicalCopiesFromAggregateIfMissing(holding);
        return bookCopyRepository.findFirstByHolding_IdAndStatusOrderByBarcodeAsc(holding.getId(), BookCopyStatus.AVAILABLE)
                .orElseThrow(() -> new IllegalArgumentException("No available physical copy exists for this holding"));
    }

    private void initializePhysicalCopiesFromAggregateIfMissing(BookHolding holding) {
        if (holding.getFormat() != HoldingFormat.PHYSICAL || holding.getId() == null || bookCopyRepository.countByHolding_Id(holding.getId()) > 0) {
            return;
        }

        List<BookCopy> copies = new ArrayList<>();
        for (int index = 0; index < holding.getTotalQuantity(); index++) {
            BookCopyStatus status = index < holding.getAvailableQuantity()
                    ? BookCopyStatus.AVAILABLE
                    : BookCopyStatus.UNAVAILABLE;
            copies.add(new BookCopy(holding, nextBarcode(holding), status));
        }
        bookCopyRepository.saveAll(copies);
    }

    private void synchronizeHoldingInventoryFromCopies(BookHolding holding) {
        initializePhysicalCopiesFromAggregateIfMissing(holding);
        List<BookCopy> copies = bookCopyRepository.findAllByHolding_IdOrderByBarcodeAsc(holding.getId());
        int available = (int) copies.stream()
                .filter(BookCopy::isAvailable)
                .count();
        holding.synchronizeInventory(copies.size(), available);
    }

    private String nextBarcode(BookHolding holding) {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "COPY-%d-%s".formatted(holding.getId(), suffix);
    }

    private boolean holdingBranchChanged(BookHolding holding, LibraryBranch targetBranch) {
        Long currentBranchId = holding.getBranch() != null ? holding.getBranch().getId() : null;
        Long targetBranchId = targetBranch != null ? targetBranch.getId() : null;
        return !java.util.Objects.equals(currentBranchId, targetBranchId);
    }

    private boolean holdingLocationChanged(BookHolding holding, LibraryLocation targetLocation) {
        Long currentLocationId = holding.getLocation() != null ? holding.getLocation().getId() : null;
        Long targetLocationId = targetLocation != null ? targetLocation.getId() : null;
        return !java.util.Objects.equals(currentLocationId, targetLocationId);
    }

    private void publishHoldingActivity(String action, String beforeState, BookHolding holding) {
        CurrentUser actor = currentUserService.getCurrentUser();
        String message = beforeState == null
                ? "%s %s [%s]".formatted(actor.username(), action, describe(holding))
                : "%s %s from [%s] to [%s]".formatted(actor.username(), action, beforeState, describe(holding));
        applicationEventPublisher.publishEvent(new OperationalActivityEvent(
                actor.id(),
                "HOLDING_UPDATED",
                message,
                java.time.Instant.now()));
    }

    private String describe(BookHolding holding) {
        return "book=%s, branch=%s, location=%s, format=%s, total=%d, available=%d, active=%s".formatted(
                holding.getBook().getTitle(),
                holding.getBranch() != null ? holding.getBranch().getCode() : "none",
                holding.getLocation() != null ? holding.getLocation().getCode() : "none",
                holding.getFormat(),
                holding.getTotalQuantity(),
                holding.getAvailableQuantity(),
                holding.isActive());
    }
}
