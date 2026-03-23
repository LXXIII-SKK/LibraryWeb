package com.example.library.upcoming;

import java.util.List;

import com.example.library.branch.BranchService;
import com.example.library.branch.LibraryBranch;
import com.example.library.common.OperationalActivityEvent;
import com.example.library.identity.AccessScope;
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
public class UpcomingBookService {

    private final UpcomingBookRepository upcomingBookRepository;
    private final BranchService branchService;
    private final CurrentUserService currentUserService;
    private final AuthorizationService authorizationService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public UpcomingBookService(
            UpcomingBookRepository upcomingBookRepository,
            BranchService branchService,
            CurrentUserService currentUserService,
            AuthorizationService authorizationService,
            ApplicationEventPublisher applicationEventPublisher) {
        this.upcomingBookRepository = upcomingBookRepository;
        this.branchService = branchService;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public List<UpcomingBookResponse> listPublic() {
        return upcomingBookRepository.findAllByOrderByExpectedAtAscTitleAsc().stream()
                .map(UpcomingBookResponse::from)
                .toList();
    }

    @Transactional
    public UpcomingBookResponse create(UpcomingBookRequest request) {
        Long branchId = resolveManagedBranchId(request.branchId());
        LibraryBranch branch = branchService.findBranchOrNull(branchId);
        UpcomingBook upcomingBook = upcomingBookRepository.save(new UpcomingBook(
                request.title(),
                request.author(),
                request.category(),
                request.isbn(),
                request.summary(),
                request.expectedAt(),
                branch,
                request.tags()));
        publishUpcomingBookActivity("created upcoming book", null, upcomingBook);
        return UpcomingBookResponse.from(upcomingBook);
    }

    @Transactional
    public UpcomingBookResponse update(Long upcomingBookId, UpcomingBookRequest request) {
        UpcomingBook upcomingBook = upcomingBookRepository.findById(upcomingBookId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Upcoming book %d was not found".formatted(upcomingBookId)));
        String beforeState = describe(upcomingBook);
        if (upcomingBook.getBranch() != null && upcomingBook.getBranch().getId() != null) {
            authorizationService.assertCanManageBranchInventory(upcomingBook.getBranch().getId());
        }
        Long branchId = resolveManagedBranchId(request.branchId());
        LibraryBranch branch = branchService.findBranchOrNull(branchId);
        upcomingBook.update(
                request.title(),
                request.author(),
                request.category(),
                request.isbn(),
                request.summary(),
                request.expectedAt(),
                branch,
                request.tags());
        publishUpcomingBookActivity("updated upcoming book", beforeState, upcomingBook);
        return UpcomingBookResponse.from(upcomingBook);
    }

    @Transactional
    public void delete(Long upcomingBookId) {
        UpcomingBook upcomingBook = upcomingBookRepository.findById(upcomingBookId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Upcoming book %d was not found".formatted(upcomingBookId)));
        if (upcomingBook.getBranch() != null && upcomingBook.getBranch().getId() != null) {
            authorizationService.assertCanManageBranchInventory(upcomingBook.getBranch().getId());
        } else if (currentUserService.getCurrentUser().scope() != AccessScope.GLOBAL) {
            throw new AccessDeniedException("Only global staff can remove global upcoming titles");
        }
        publishUpcomingBookActivity("deleted upcoming book", describe(upcomingBook), upcomingBook);
        upcomingBookRepository.delete(upcomingBook);
    }

    private Long resolveManagedBranchId(Long requestedBranchId) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (currentUser.scope() == AccessScope.GLOBAL) {
            return requestedBranchId;
        }
        if (currentUser.scope() != AccessScope.BRANCH || currentUser.branchId() == null) {
            throw new AccessDeniedException("This role cannot manage upcoming titles");
        }
        if (requestedBranchId != null && !currentUser.branchId().equals(requestedBranchId)) {
            throw new AccessDeniedException("Branch-scoped staff can only manage their own branch");
        }
        return currentUser.branchId();
    }

    private void publishUpcomingBookActivity(String action, String beforeState, UpcomingBook upcomingBook) {
        CurrentUser actor = currentUserService.getCurrentUser();
        String currentState = describe(upcomingBook);
        String message = beforeState == null
                ? "%s %s [%s]".formatted(actor.username(), action, currentState)
                : action.startsWith("deleted")
                ? "%s %s [%s]".formatted(actor.username(), action, beforeState)
                : "%s %s from [%s] to [%s]".formatted(actor.username(), action, beforeState, currentState);
        applicationEventPublisher.publishEvent(new OperationalActivityEvent(
                actor.id(),
                "UPCOMING_BOOK_UPDATED",
                message,
                java.time.Instant.now()));
    }

    private String describe(UpcomingBook upcomingBook) {
        return "title=%s, author=%s, category=%s, expectedAt=%s, branch=%s".formatted(
                upcomingBook.getTitle(),
                upcomingBook.getAuthor(),
                upcomingBook.getCategory(),
                upcomingBook.getExpectedAt(),
                upcomingBook.getBranch() != null ? upcomingBook.getBranch().getCode() : "global");
    }
}
