package com.example.library.identity;

import java.util.List;
import java.util.Locale;

import com.example.library.branch.BranchService;
import com.example.library.branch.BranchSummaryResponse;
import com.example.library.branch.LibraryBranch;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class StaffRegistrationService {

    private final AppUserRepository appUserRepository;
    private final BranchService branchService;
    private final CurrentUserService currentUserService;
    private final AuthorizationService authorizationService;
    private final KeycloakAdminClient keycloakAdminClient;
    private final ApplicationEventPublisher applicationEventPublisher;

    StaffRegistrationService(
            AppUserRepository appUserRepository,
            BranchService branchService,
            CurrentUserService currentUserService,
            AuthorizationService authorizationService,
            KeycloakAdminClient keycloakAdminClient,
            ApplicationEventPublisher applicationEventPublisher) {
        this.appUserRepository = appUserRepository;
        this.branchService = branchService;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
        this.keycloakAdminClient = keycloakAdminClient;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    AccessOptionsResponse options() {
        authorizationService.assertCanRegisterStaff();
        List<BranchSummaryResponse> branches = branchService.listSummaries();
        return AccessOptionsResponse.staffRegistrationOptions(branches);
    }

    @Transactional
    UserAccessResponse register(StaffRegistrationRequest request) {
        authorizationService.assertCanRegisterStaff();

        if (!request.role().isStaff()) {
            throw new IllegalArgumentException("Staff registration cannot create member accounts");
        }

        String username = normalizeUsername(request.username());
        String email = normalizeEmail(request.email());
        assertLocalIdentityAvailable(username, email);

        RegistrationAssignment assignment = resolveAssignment(request, request.role());
        KeycloakAdminClient.ProvisionedUser provisioned = keycloakAdminClient.createStaffUser(
                username,
                email,
                request.password(),
                request.requirePasswordChange(),
                request.role());

        AppUser user = new AppUser(
                provisioned.keycloakUserId(),
                provisioned.username(),
                provisioned.email(),
                request.role(),
                request.accountStatus(),
                MembershipStatus.GOOD_STANDING,
                assignment.branch(),
                assignment.homeBranch());

        try {
            AppUser saved = appUserRepository.save(user);
            publishRegistrationActivity(saved);
            return UserAccessResponse.from(saved);
        } catch (RuntimeException exception) {
            keycloakAdminClient.deleteUser(provisioned.keycloakUserId());
            if (exception instanceof DataIntegrityViolationException) {
                throw new IllegalArgumentException("A local user with that username or email already exists");
            }
            throw exception;
        }
    }

    private void assertLocalIdentityAvailable(String username, String email) {
        if (appUserRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("A local user with that username already exists");
        }
        if (appUserRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new IllegalArgumentException("A local user with that email already exists");
        }
    }

    private RegistrationAssignment resolveAssignment(StaffRegistrationRequest request, AppRole role) {
        LibraryBranch branch = request.branchId() == null ? null : branchService.resolveBranch(request.branchId());
        LibraryBranch homeBranch = request.homeBranchId() == null ? null : branchService.resolveBranch(request.homeBranchId());

        if (role.scope() == AccessScope.BRANCH) {
            if (branch == null) {
                throw new IllegalArgumentException("Branch assignment is required for branch staff");
            }
            if (homeBranch == null) {
                homeBranch = branch;
            }
        }

        return new RegistrationAssignment(branch, homeBranch);
    }

    private void publishRegistrationActivity(AppUser user) {
        CurrentUser actor = currentUserService.getCurrentUser();
        applicationEventPublisher.publishEvent(new UserAccessUpdatedEvent(
                actor.id(),
                actor.username(),
                user.getId(),
                user.getUsername(),
                "%s registered staff account %s [%s, account=%s, branch=%s, homeBranch=%s]".formatted(
                        actor.username(),
                        user.getUsername(),
                        user.getRole(),
                        user.getAccountStatus(),
                        describeBranch(user.getBranch()),
                        describeBranch(user.getHomeBranch()))));
    }

    private String describeBranch(LibraryBranch branch) {
        if (branch == null) {
            return "none";
        }
        return "%s (%s)".formatted(branch.getName(), branch.getCode());
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private record RegistrationAssignment(
            LibraryBranch branch,
            LibraryBranch homeBranch) {
    }
}
