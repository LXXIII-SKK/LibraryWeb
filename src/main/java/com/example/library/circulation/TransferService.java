package com.example.library.circulation;

import java.time.Instant;
import java.util.List;

import com.example.library.common.OperationalActivityEvent;
import com.example.library.identity.AccessScope;
import com.example.library.identity.AppPermission;
import com.example.library.identity.AuthorizationService;
import com.example.library.identity.CurrentUser;
import com.example.library.identity.CurrentUserService;
import com.example.library.inventory.BookCopy;
import com.example.library.inventory.BookHolding;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TransferService {

    private final BookTransferRepository bookTransferRepository;
    private final CurrentUserService currentUserService;
    private final AuthorizationService authorizationService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public TransferService(
            BookTransferRepository bookTransferRepository,
            CurrentUserService currentUserService,
            AuthorizationService authorizationService,
            ApplicationEventPublisher applicationEventPublisher) {
        this.bookTransferRepository = bookTransferRepository;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public List<BookTransferResponse> listManagedTransfers() {
        if (!authorizationService.canReadTransfers()) {
            throw new AccessDeniedException("This role cannot view transfer operations");
        }
        CurrentUser currentUser = currentUserService.getCurrentUser();
        List<BookTransfer> transfers = switch (currentUser.scope()) {
            case GLOBAL -> bookTransferRepository.findAllByOrderByRequestedAtDesc();
            case BRANCH -> {
                if (currentUser.branchId() == null
                        || !currentUser.hasPermission(AppPermission.RESERVATION_MANAGE_BRANCH)) {
                    throw new AccessDeniedException("Branch-scoped staff require reservation permissions to read transfers");
                }
                yield bookTransferRepository.findAllBySourceHolding_Branch_IdOrDestinationBranch_IdOrderByRequestedAtDesc(
                        currentUser.branchId(),
                        currentUser.branchId());
            }
            case SELF -> throw new AccessDeniedException("Self-scoped users cannot read transfer operations");
        };
        return transfers.stream()
                .map(BookTransferResponse::from)
                .toList();
    }

    @Transactional
    public BookTransfer createInTransitTransfer(BookHolding sourceHolding, BookCopy copy, com.example.library.branch.LibraryBranch destinationBranch, Instant now) {
        BookTransfer transfer = bookTransferRepository.save(new BookTransfer(copy, sourceHolding, destinationBranch, now));
        publishActivity("created transfer", transfer);
        return transfer;
    }

    @Transactional
    public void markReady(BookTransfer transfer, Instant readyAt) {
        transfer.markReadyForPickup(readyAt);
        publishActivity("marked transfer ready", transfer);
    }

    @Transactional
    public void complete(BookTransfer transfer, Instant completedAt) {
        transfer.complete(completedAt);
        publishActivity("completed transfer", transfer);
    }

    @Transactional
    public void cancel(BookTransfer transfer, Instant closedAt) {
        transfer.cancel(closedAt);
        publishActivity("cancelled transfer", transfer);
    }

    @Transactional
    public void expire(BookTransfer transfer, Instant closedAt) {
        transfer.expire(closedAt);
        publishActivity("expired transfer", transfer);
    }

    private void publishActivity(String action, BookTransfer transfer) {
        CurrentUser actor = currentUserService.getCurrentUser();
        String message = "%s %s [copy=%s, book=%s, from=%s, to=%s, status=%s]".formatted(
                actor.username(),
                action,
                transfer.getCopy().getBarcode(),
                transfer.getSourceHolding().getBook().getTitle(),
                transfer.getSourceHolding().getBranch().getCode(),
                transfer.getDestinationBranch().getCode(),
                transfer.getStatus());
        applicationEventPublisher.publishEvent(new OperationalActivityEvent(
                actor.id(),
                "TRANSFER_UPDATED",
                message,
                Instant.now()));
    }
}
