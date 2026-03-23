package com.example.library.inventory;

import java.util.List;

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

    private final BookHoldingRepository bookHoldingRepository;
    private final CatalogService catalogService;
    private final BranchService branchService;
    private final LocationService locationService;
    private final CurrentUserService currentUserService;
    private final AuthorizationService authorizationService;
    private final BorrowTransactionRepository borrowTransactionRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public InventoryService(
            BookHoldingRepository bookHoldingRepository,
            CatalogService catalogService,
            BranchService branchService,
            LocationService locationService,
            CurrentUserService currentUserService,
            AuthorizationService authorizationService,
            BorrowTransactionRepository borrowTransactionRepository,
            ApplicationEventPublisher applicationEventPublisher) {
        this.bookHoldingRepository = bookHoldingRepository;
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
        return holdings.stream()
                .map(BookHoldingResponse::from)
                .toList();
    }

    public BookHolding findHolding(Long holdingId) {
        return bookHoldingRepository.findById(holdingId)
                .orElseThrow(() -> new EntityNotFoundException("Holding %d was not found".formatted(holdingId)));
    }

    public BookHolding resolveBorrowableHolding(Book book, Long requestedHoldingId) {
        if (requestedHoldingId != null) {
            BookHolding holding = findHolding(requestedHoldingId);
            if (!holding.getBook().getId().equals(book.getId())) {
                throw new IllegalArgumentException("Selected holding does not belong to the requested book");
            }
            if (!holding.isBorrowable()) {
                throw new IllegalArgumentException("Selected holding is not currently available");
            }
            return holding;
        }

        List<BookHolding> borrowableHoldings = bookHoldingRepository.findAllByBook_IdOrderByFormatAscBranch_NameAscIdAsc(book.getId()).stream()
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

        holding.update(
                branch,
                location,
                request.format(),
                request.totalQuantity(),
                request.availableQuantity(),
                request.accessUrl(),
                request.active());
        synchronizeBookInventory(holding.getBook());
        publishHoldingActivity("updated holding", beforeState, holding);
        return BookHoldingResponse.from(holding);
    }

    @Transactional
    public void synchronizeBookInventory(Book book) {
        List<BookHolding> holdings = bookHoldingRepository.findAllByBook_IdOrderByFormatAscBranch_NameAscIdAsc(book.getId());
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
