package com.example.library.branch;

import java.util.List;

import com.example.library.identity.AccessScope;
import com.example.library.identity.CurrentUser;
import com.example.library.identity.CurrentUserService;
import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/branches")
class BranchController {

    private final BranchService branchService;
    private final CurrentUserService currentUserService;

    BranchController(BranchService branchService, CurrentUserService currentUserService) {
        this.branchService = branchService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/public")
    List<LibraryBranchResponse> publicBranches() {
        return branchService.listPublicActive();
    }

    @GetMapping
    @PreAuthorize("@authorizationService.canReadBranches()")
    List<LibraryBranchResponse> listBranches() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (currentUser.scope() == AccessScope.BRANCH && currentUser.branchId() != null) {
            return branchService.listSummariesByIds(List.of(currentUser.branchId())).stream()
                    .map(summary -> new LibraryBranchResponse(
                            summary.id(),
                            summary.code(),
                            summary.name(),
                            null,
                            null,
                            summary.active()))
                    .toList();
        }
        return branchService.listAll();
    }

    @PostMapping
    @PreAuthorize("@authorizationService.canManageBranches()")
    LibraryBranchResponse createBranch(@Valid @RequestBody BranchUpsertRequest request) {
        return branchService.create(request);
    }

    @PutMapping("/{branchId}")
    @PreAuthorize("@authorizationService.canManageBranches()")
    LibraryBranchResponse updateBranch(@PathVariable Long branchId, @Valid @RequestBody BranchUpsertRequest request) {
        return branchService.update(branchId, request);
    }
}
