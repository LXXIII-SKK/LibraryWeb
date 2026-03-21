package com.example.library.identity;

import java.util.List;
import java.util.Set;

import com.example.library.branch.BranchService;
import com.example.library.branch.BranchSummaryResponse;
import com.example.library.branch.LibraryBranch;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccessManagementService {

    private final AppUserRepository appUserRepository;
    private final BranchService branchService;
    private final CurrentUserService currentUserService;
    private final AuthorizationService authorizationService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final UserDisciplineRecordRepository userDisciplineRecordRepository;

    public AccessManagementService(
            AppUserRepository appUserRepository,
            BranchService branchService,
            CurrentUserService currentUserService,
            AuthorizationService authorizationService,
            ApplicationEventPublisher applicationEventPublisher,
            UserDisciplineRecordRepository userDisciplineRecordRepository) {
        this.appUserRepository = appUserRepository;
        this.branchService = branchService;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.userDisciplineRecordRepository = userDisciplineRecordRepository;
    }

    public List<UserAccessResponse> listUsers() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        List<AppUser> users = switch (currentUser.scope()) {
            case GLOBAL -> appUserRepository.findAllByOrderByUsernameAsc();
            case BRANCH -> {
                if (currentUser.branchId() == null) {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "Branch-scoped user requires a branch assignment");
                }
                List<AppRole> visibleRoles = branchVisibleRoles(currentUser);
                if (visibleRoles.isEmpty()) {
                    yield List.of();
                }
                if (visibleRoles.size() == 1) {
                    yield appUserRepository.findAllByBranch_IdAndRoleOrderByUsernameAsc(
                            currentUser.branchId(),
                            visibleRoles.getFirst());
                }
                yield appUserRepository.findAllByBranch_IdAndRoleInOrderByUsernameAsc(
                        currentUser.branchId(),
                        visibleRoles);
            }
            case SELF -> List.of(appUserRepository.findById(currentUser.id())
                    .orElseThrow(() -> new EntityNotFoundException("Current user was not found")));
        };
        return users.stream()
                .peek(authorizationService::assertCanReadUser)
                .map(user -> UserAccessResponse.from(user, canSeePermissions(currentUser, user)))
                .toList();
    }

    public UserAccessResponse getUser(Long userId) {
        AppUser user = findUser(userId);
        authorizationService.assertCanReadUser(user);
        return UserAccessResponse.from(user, canSeePermissions(currentUserService.getCurrentUser(), user));
    }

    public AccessOptionsResponse optionsForUser(Long userId) {
        AppUser user = findUser(userId);
        authorizationService.assertCanReadUser(user);
        CurrentUser currentUser = currentUserService.getCurrentUser();
        UserAccessResponse target = UserAccessResponse.from(user);

        if (currentUser.hasPermission(AppPermission.USER_MANAGE_GLOBAL)) {
            return new AccessOptionsResponse(
                    List.of(AppRole.values()),
                    List.of(AccountStatus.values()),
                    List.of(MembershipStatus.values()),
                    branchService.listSummaries(),
                    filterDisciplineActionsForState(List.of(UserDisciplineActionType.values()), user.getAccountStatus()),
                    List.of(UserDisciplineReason.values()));
        }
        if (canManageBranchTarget(currentUser, user)) {
            List<AppRole> roles = List.of(user.getRole());
            List<AccountStatus> accountStatuses = manageableAccountStatuses(currentUser, user);
            List<MembershipStatus> membershipStatuses = manageableMembershipStatuses(currentUser, user);
            List<BranchSummaryResponse> branches = branchService.listSummariesByIds(List.of(currentUser.branchId()));
            return new AccessOptionsResponse(
                    roles,
                    accountStatuses,
                    membershipStatuses,
                    branches,
                    disciplineActionsFor(currentUser, user),
                    List.of(UserDisciplineReason.values()));
        }
        return AccessOptionsResponse.lockedTo(target);
    }

    @Transactional
    public UserAccessResponse updateUserAccess(Long userId, UserAccessUpdateRequest request) {
        AppUser user = findUser(userId);
        authorizationService.assertCanManageUser(user, request);
        CurrentUser actor = currentUserService.getCurrentUser();
        String beforeState = describeAccess(user);
        LibraryBranch branch = branchService.resolveBranch(request.branchId());
        LibraryBranch homeBranch = branchService.resolveBranch(request.homeBranchId());
        user.updateAccess(
                request.role(),
                request.accountStatus(),
                request.membershipStatus(),
                branch,
                homeBranch);
        applicationEventPublisher.publishEvent(new UserAccessUpdatedEvent(
                actor.id(),
                actor.username(),
                user.getId(),
                user.getUsername(),
                "%s updated access for %s from [%s] to [%s]".formatted(
                        actor.username(),
                        user.getUsername(),
                        beforeState,
                        describeAccess(user))));
        return UserAccessResponse.from(user);
    }

    public List<UserDisciplineRecordResponse> listUserDisciplineHistory(Long userId) {
        AppUser user = findUser(userId);
        authorizationService.assertCanReadUser(user);
        return userDisciplineRecordRepository.findAllByTargetUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(UserDisciplineRecordResponse::from)
                .toList();
    }

    @Transactional
    public UserDisciplineRecordResponse applyUserDiscipline(Long userId, UserDisciplineRequest request) {
        AppUser user = findUser(userId);
        authorizationService.assertCanApplyUserDiscipline(user, request.action());
        validateDisciplineTransition(user, request.action());

        CurrentUser actor = currentUserService.getCurrentUser();
        AppUser actorEntity = currentUserService.getCurrentUserEntity();
        AccountStatus previousStatus = user.getAccountStatus();
        AccountStatus resultingStatus = request.action().resultingAccountStatus();
        user.updateAccess(
                user.getRole(),
                resultingStatus,
                user.getMembershipStatus(),
                user.getBranch(),
                user.getHomeBranch());

        UserDisciplineRecord record = userDisciplineRecordRepository.save(new UserDisciplineRecord(
                user,
                actorEntity,
                request.action(),
                request.reason(),
                request.note(),
                previousStatus,
                resultingStatus));

        applicationEventPublisher.publishEvent(new UserDisciplineRecordedEvent(
                actor.id(),
                actor.username(),
                user.getId(),
                user.getUsername(),
                request.action(),
                request.reason(),
                disciplineMessage(actor.username(), user.getUsername(), request.action(), request.reason(), request.note()),
                record.getCreatedAt()));

        return UserDisciplineRecordResponse.from(record);
    }

    public AccessOptionsResponse options() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (currentUser.hasPermission(AppPermission.USER_MANAGE_GLOBAL)) {
            return AccessOptionsResponse.defaultOptions(branchService.listSummaries());
        }

        List<BranchSummaryResponse> branches = currentUser.branchId() == null
                ? List.of()
                : branchService.listSummariesByIds(List.of(currentUser.branchId()));
        return new AccessOptionsResponse(
                List.of(AppRole.MEMBER),
                List.of(AccountStatus.PENDING_VERIFICATION, AccountStatus.ACTIVE, AccountStatus.SUSPENDED),
                List.of(MembershipStatus.values()),
                branches,
                disciplineActionsForCurrentUser(currentUser),
                List.of(UserDisciplineReason.values()));
    }

    private AppUser findUser(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User %d was not found".formatted(userId)));
    }

    private String describeAccess(AppUser user) {
        return "role=%s, account=%s, membership=%s, branch=%s, homeBranch=%s".formatted(
                user.getRole(),
                user.getAccountStatus(),
                user.getMembershipStatus(),
                describeBranch(user.getBranch()),
                describeBranch(user.getHomeBranch()));
    }

    private String describeBranch(LibraryBranch branch) {
        if (branch == null) {
            return "none";
        }
        return "%s (%s)".formatted(branch.getName(), branch.getCode());
    }

    private boolean canSeePermissions(CurrentUser currentUser, AppUser targetUser) {
        if (targetUser.getId() != null && targetUser.getId().equals(currentUser.id())) {
            return true;
        }
        if (currentUser.hasPermission(AppPermission.USER_MANAGE_GLOBAL)) {
            return true;
        }
        return currentUser.role() == AppRole.BRANCH_MANAGER && canManageBranchTarget(currentUser, targetUser);
    }

    private boolean canManageBranchTarget(CurrentUser currentUser, AppUser user) {
        if (currentUser.scope() != AccessScope.BRANCH
                || currentUser.branchId() == null
                || !currentUser.belongsToBranch(user.getBranchId())) {
            return false;
        }
        return switch (currentUser.role()) {
            case LIBRARIAN -> false;
            case BRANCH_MANAGER -> Set.of(AppRole.MEMBER, AppRole.LIBRARIAN).contains(user.getRole());
            default -> false;
        };
    }

    private List<AppRole> branchVisibleRoles(CurrentUser currentUser) {
        return switch (currentUser.role()) {
            case LIBRARIAN -> List.of();
            case BRANCH_MANAGER -> List.of(AppRole.MEMBER, AppRole.LIBRARIAN);
            default -> List.of();
        };
    }

    private List<AccountStatus> manageableAccountStatuses(CurrentUser currentUser, AppUser user) {
        if (user.getRole() == AppRole.MEMBER) {
            return List.of(AccountStatus.PENDING_VERIFICATION, AccountStatus.ACTIVE, AccountStatus.SUSPENDED);
        }
        if (currentUser.role() == AppRole.BRANCH_MANAGER && user.getRole() == AppRole.LIBRARIAN) {
            return List.of(AccountStatus.ACTIVE, AccountStatus.SUSPENDED);
        }
        return List.of(user.getAccountStatus());
    }

    private List<MembershipStatus> manageableMembershipStatuses(CurrentUser currentUser, AppUser user) {
        if (user.getRole() == AppRole.MEMBER) {
            return currentUser.hasPermission(AppPermission.APPROVAL_BRANCH)
                    ? List.of(MembershipStatus.values())
                    : List.of(user.getMembershipStatus());
        }
        return List.of(user.getMembershipStatus());
    }

    private void validateDisciplineTransition(AppUser user, UserDisciplineActionType action) {
        switch (action) {
            case SUSPEND, BAN -> {
                if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                    throw new IllegalArgumentException("Only active users can be suspended or banned");
                }
            }
            case REINSTATE -> {
                if (!Set.of(AccountStatus.SUSPENDED, AccountStatus.LOCKED).contains(user.getAccountStatus())) {
                    throw new IllegalArgumentException("Only suspended or banned users can be reinstated");
                }
            }
        }
    }

    private List<UserDisciplineActionType> disciplineActionsFor(CurrentUser currentUser, AppUser targetUser) {
        if (targetUser.getId() != null && targetUser.getId().equals(currentUser.id())) {
            return List.of();
        }
        if (currentUser.hasPermission(AppPermission.USER_MANAGE_GLOBAL)) {
            return filterDisciplineActionsForState(List.of(UserDisciplineActionType.values()), targetUser.getAccountStatus());
        }
        if (currentUser.scope() != AccessScope.BRANCH
                || currentUser.branchId() == null
                || !currentUser.belongsToBranch(targetUser.getBranchId())) {
            return List.of();
        }
        if (currentUser.hasPermission(AppPermission.APPROVAL_BRANCH)) {
            if (!Set.of(AppRole.MEMBER, AppRole.LIBRARIAN).contains(targetUser.getRole())) {
                return List.of();
            }
            return filterDisciplineActionsForState(List.of(UserDisciplineActionType.values()), targetUser.getAccountStatus());
        }
        if (currentUser.hasPermission(AppPermission.MEMBER_VERIFY_BRANCH)) {
            if (targetUser.getRole() != AppRole.MEMBER) {
                return List.of();
            }
            return filterDisciplineActionsForState(
                    List.of(UserDisciplineActionType.SUSPEND, UserDisciplineActionType.REINSTATE),
                    targetUser.getAccountStatus());
        }
        return List.of();
    }

    private List<UserDisciplineActionType> disciplineActionsForCurrentUser(CurrentUser currentUser) {
        if (currentUser.hasPermission(AppPermission.USER_MANAGE_GLOBAL)) {
            return List.of(UserDisciplineActionType.values());
        }
        if (currentUser.hasPermission(AppPermission.APPROVAL_BRANCH)) {
            return List.of(UserDisciplineActionType.values());
        }
        if (currentUser.hasPermission(AppPermission.MEMBER_VERIFY_BRANCH)) {
            return List.of(UserDisciplineActionType.SUSPEND, UserDisciplineActionType.REINSTATE);
        }
        return List.of();
    }

    private String disciplineMessage(
            String actorUsername,
            String targetUsername,
            UserDisciplineActionType action,
            UserDisciplineReason reason,
            String note) {
        String base = switch (action) {
            case SUSPEND -> "%s suspended %s for %s".formatted(actorUsername, targetUsername, reason);
            case BAN -> "%s banned %s for %s".formatted(actorUsername, targetUsername, reason);
            case REINSTATE -> "%s reinstated %s after %s".formatted(actorUsername, targetUsername, reason);
        };
        return note == null || note.isBlank() ? base : "%s. Note: %s".formatted(base, note.trim());
    }

    private List<UserDisciplineActionType> filterDisciplineActionsForState(
            List<UserDisciplineActionType> actions,
            AccountStatus accountStatus) {
        return actions.stream()
                .filter(action -> switch (action) {
                    case SUSPEND, BAN -> accountStatus == AccountStatus.ACTIVE;
                    case REINSTATE -> Set.of(AccountStatus.SUSPENDED, AccountStatus.LOCKED).contains(accountStatus);
                })
                .toList();
    }
}
