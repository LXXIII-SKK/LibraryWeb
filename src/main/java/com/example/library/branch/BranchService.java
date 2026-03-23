package com.example.library.branch;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.example.library.common.OperationalActivityEvent;
import com.example.library.identity.CurrentUser;
import com.example.library.identity.CurrentUserService;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BranchService {

    private final LibraryBranchRepository libraryBranchRepository;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public BranchService(
            LibraryBranchRepository libraryBranchRepository,
            CurrentUserService currentUserService,
            ApplicationEventPublisher applicationEventPublisher) {
        this.libraryBranchRepository = libraryBranchRepository;
        this.currentUserService = currentUserService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public List<LibraryBranchResponse> listAll() {
        return libraryBranchRepository.findAllByOrderByNameAsc().stream()
                .map(LibraryBranchResponse::from)
                .toList();
    }

    public List<LibraryBranchResponse> listPublicActive() {
        return libraryBranchRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(LibraryBranchResponse::from)
                .toList();
    }

    public List<BranchSummaryResponse> listSummaries() {
        return libraryBranchRepository.findAllByOrderByNameAsc().stream()
                .map(BranchSummaryResponse::from)
                .toList();
    }

    public List<BranchSummaryResponse> listSummariesByIds(Collection<Long> branchIds) {
        List<Long> ids = branchIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return libraryBranchRepository.findAllById(ids).stream()
                .sorted(Comparator.comparing(LibraryBranch::getName))
                .map(BranchSummaryResponse::from)
                .toList();
    }

    public LibraryBranch resolveBranch(Long branchId) {
        if (branchId == null) {
            return null;
        }
        return libraryBranchRepository.findById(branchId)
                .orElseThrow(() -> new EntityNotFoundException("Branch %d was not found".formatted(branchId)));
    }

    public LibraryBranch findBranchOrNull(Long branchId) {
        if (branchId == null) {
            return null;
        }
        return libraryBranchRepository.findById(branchId).orElse(null);
    }

    @Transactional
    public LibraryBranchResponse create(BranchUpsertRequest request) {
        assertUniqueCode(request.code(), null);
        LibraryBranch branch = libraryBranchRepository.save(new LibraryBranch(
                request.code(),
                request.name(),
                request.address(),
                request.phone(),
                request.active()));
        publishBranchActivity("created branch", null, branch);
        return LibraryBranchResponse.from(branch);
    }

    @Transactional
    public LibraryBranchResponse update(Long branchId, BranchUpsertRequest request) {
        LibraryBranch branch = resolveBranch(branchId);
        String beforeState = describe(branch);
        assertUniqueCode(request.code(), branchId);
        branch.updateDetails(
                request.code(),
                request.name(),
                request.address(),
                request.phone(),
                request.active());
        publishBranchActivity("updated branch", beforeState, branch);
        return LibraryBranchResponse.from(branch);
    }

    private void assertUniqueCode(String code, Long branchId) {
        boolean exists = branchId == null
                ? libraryBranchRepository.existsByCodeIgnoreCase(code)
                : libraryBranchRepository.existsByCodeIgnoreCaseAndIdNot(code, branchId);
        if (exists) {
            throw new IllegalArgumentException("Branch code is already in use");
        }
    }

    private void publishBranchActivity(String action, String beforeState, LibraryBranch branch) {
        CurrentUser actor = currentUserService.getCurrentUser();
        String message = beforeState == null
                ? "%s %s [%s]".formatted(actor.username(), action, describe(branch))
                : "%s %s from [%s] to [%s]".formatted(actor.username(), action, beforeState, describe(branch));
        applicationEventPublisher.publishEvent(new OperationalActivityEvent(
                actor.id(),
                "BRANCH_UPDATED",
                message,
                java.time.Instant.now()));
    }

    private String describe(LibraryBranch branch) {
        return "code=%s, name=%s, address=%s, phone=%s, active=%s".formatted(
                branch.getCode(),
                branch.getName(),
                branch.getAddress(),
                branch.getPhone(),
                branch.isActive());
    }
}
