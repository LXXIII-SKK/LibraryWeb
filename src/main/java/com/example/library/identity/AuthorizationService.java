package com.example.library.identity;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service("authorizationService")
public class AuthorizationService {

    private static final Set<AccountStatus> BRANCH_MANAGEABLE_ACCOUNT_STATUSES = EnumSet.of(
            AccountStatus.PENDING_VERIFICATION,
            AccountStatus.ACTIVE,
            AccountStatus.SUSPENDED);

    private final CurrentUserService currentUserService;

    public AuthorizationService(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    public boolean canManageCatalog() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && !currentUser.isReadOnlyRole()
                && (currentUser.hasPermission(AppPermission.BOOK_CREATE)
                        || currentUser.hasPermission(AppPermission.BOOK_UPDATE));
    }

    public boolean canDeleteCatalog() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser) && currentUser.role() == AppRole.ADMIN;
    }

    public boolean canBorrowForSelf() {
        return currentUserService.getCurrentUser().canBorrowForSelf();
    }

    public boolean canRenewBorrowings() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && (currentUser.hasPermission(AppPermission.LOAN_SELF_RENEW)
                        || currentUser.hasPermission(AppPermission.LOAN_OVERRIDE_BRANCH)
                        || canMutateGlobally(currentUser));
    }

    public boolean canReadOwnReservations() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && (currentUser.hasPermission(AppPermission.RESERVATION_SELF_CREATE)
                        || currentUser.hasPermission(AppPermission.RESERVATION_SELF_CANCEL));
    }

    public boolean canCreateReservationForSelf() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && currentUser.role() == AppRole.MEMBER
                && currentUser.hasPermission(AppPermission.RESERVATION_SELF_CREATE)
                && currentUser.membershipStatus().allowsBorrowing();
    }

    public boolean canReadOperationalReservations() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && (currentUser.hasPermission(AppPermission.RESERVATION_MANAGE_BRANCH)
                        || currentUser.hasPermission(AppPermission.LOAN_READ_GLOBAL)
                        || currentUser.hasPermission(AppPermission.REPORT_GLOBAL_READ));
    }

    public boolean canManageReservations() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && (currentUser.hasPermission(AppPermission.RESERVATION_MANAGE_BRANCH) || canMutateGlobally(currentUser));
    }

    public boolean canReadTransfers() {
        return canReadOperationalReservations();
    }

    public boolean canRecordBookView() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser) && !currentUser.isReadOnlyRole();
    }

    public boolean canAccessReturnEndpoint() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && (currentUser.hasPermission(AppPermission.LOAN_SELF_RETURN)
                        || currentUser.hasPermission(AppPermission.LOAN_CLOSE_BRANCH)
                        || canMutateGlobally(currentUser));
    }

    public boolean canManageBorrowingExceptions() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && (currentUser.hasPermission(AppPermission.LOAN_CLOSE_BRANCH) || canMutateGlobally(currentUser));
    }

    public boolean canReadOwnBorrowings() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser) && currentUser.hasPermission(AppPermission.LOAN_SELF_READ);
    }

    public boolean canReadOperationalBorrowings() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && (currentUser.hasPermission(AppPermission.LOAN_READ_GLOBAL)
                        || currentUser.hasPermission(AppPermission.REPORT_BRANCH_READ));
    }

    public boolean canReadOwnHistory() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser) && currentUser.hasPermission(AppPermission.LOAN_SELF_READ);
    }

    public boolean canReadOwnFines() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser) && currentUser.hasPermission(AppPermission.FINE_SELF_READ);
    }

    public boolean canReadOperationalFines() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && (currentUser.hasPermission(AppPermission.FINE_READ_BRANCH)
                        || currentUser.hasPermission(AppPermission.FINE_READ_GLOBAL));
    }

    public boolean canWaiveFine() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && (currentUser.hasPermission(AppPermission.FINE_WAIVE_BRANCH) || canMutateGlobally(currentUser));
    }

    public boolean canReadUsers() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && (currentUser.hasPermission(AppPermission.USER_READ_GLOBAL)
                        || currentUser.hasPermission(AppPermission.USER_MANAGE_GLOBAL)
                        || currentUser.hasPermission(AppPermission.MEMBER_READ_BRANCH));
    }

    public boolean canManageUsers() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && (currentUser.hasPermission(AppPermission.USER_MANAGE_GLOBAL)
                        || currentUser.hasPermission(AppPermission.MEMBER_VERIFY_BRANCH)
                        || currentUser.hasPermission(AppPermission.APPROVAL_BRANCH));
    }

    public boolean canManageUserDiscipline() {
        return canManageUsers();
    }

    public boolean canRegisterStaff() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && currentUser.hasPermission(AppPermission.USER_MANAGE_GLOBAL)
                && canMutateGlobally(currentUser);
    }

    public boolean canRequestUserDiscipline() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && currentUser.role() == AppRole.LIBRARIAN
                && currentUser.branchId() != null
                && currentUser.hasPermission(AppPermission.DISCIPLINE_REQUEST_BRANCH);
    }

    public boolean canReadPolicies() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && (currentUser.hasPermission(AppPermission.POLICY_READ)
                        || currentUser.hasPermission(AppPermission.POLICY_MANAGE_GLOBAL));
    }

    public boolean canManagePolicies() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && currentUser.hasPermission(AppPermission.POLICY_MANAGE_GLOBAL)
                && canMutateGlobally(currentUser);
    }

    public boolean canReadAuditLogs() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && ((currentUser.role() == AppRole.BRANCH_MANAGER
                        && currentUser.hasPermission(AppPermission.REPORT_BRANCH_READ)
                        && currentUser.branchId() != null)
                        || currentUser.hasPermission(AppPermission.AUDIT_GLOBAL_READ)
                        || currentUser.hasPermission(AppPermission.REPORT_GLOBAL_READ));
    }

    public boolean canManageBranches() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && currentUser.hasPermission(AppPermission.BRANCH_MANAGE_GLOBAL)
                && canMutateGlobally(currentUser);
    }

    public boolean canReadBranches() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser) && currentUser.role() != AppRole.MEMBER;
    }

    public boolean canManageInventory() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && !currentUser.isReadOnlyRole()
                && (currentUser.hasPermission(AppPermission.COPY_CREATE)
                        || currentUser.hasPermission(AppPermission.COPY_UPDATE)
                        || canMutateGlobally(currentUser));
    }

    public boolean canReadStaffNotifications() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser);
    }

    public boolean canSendStaffNotifications() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && !currentUser.isReadOnlyRole()
                && (currentUser.role() == AppRole.BRANCH_MANAGER || currentUser.role() == AppRole.ADMIN);
    }

    public void assertCanReturnBorrowingForUser(AppUser targetUser) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (targetUser.getId() != null
                && targetUser.getId().equals(currentUser.id())
                && currentUser.hasPermission(AppPermission.LOAN_SELF_RETURN)) {
            return;
        }
        if (canMutateGlobally(currentUser)) {
            return;
        }
        if (currentUser.scope() == AccessScope.BRANCH
                && currentUser.hasPermission(AppPermission.LOAN_CLOSE_BRANCH)
                && currentUser.branchId() != null
                && currentUser.belongsToBranch(targetUser.getBranchId())) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to return this borrowing");
    }

    public void assertCanManageBorrowingExceptionsForUser(AppUser targetUser) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (canMutateGlobally(currentUser)) {
            return;
        }
        if (currentUser.scope() == AccessScope.BRANCH
                && currentUser.hasPermission(AppPermission.LOAN_CLOSE_BRANCH)
                && currentUser.branchId() != null
                && currentUser.belongsToBranch(targetUser.getBranchId())) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to record an exception on this borrowing");
    }

    public void assertCanRenewBorrowingForUser(AppUser targetUser) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (targetUser.getId() != null
                && targetUser.getId().equals(currentUser.id())
                && currentUser.hasPermission(AppPermission.LOAN_SELF_RENEW)
                && currentUser.membershipStatus().allowsBorrowing()) {
            return;
        }
        if (canMutateGlobally(currentUser)) {
            return;
        }
        if (currentUser.scope() == AccessScope.BRANCH
                && currentUser.hasPermission(AppPermission.LOAN_OVERRIDE_BRANCH)
                && currentUser.branchId() != null
                && currentUser.belongsToBranch(targetUser.getBranchId())) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to renew this borrowing");
    }

    public void assertCanReadBorrowingForUser(AppUser targetUser) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (targetUser.getId() != null
                && targetUser.getId().equals(currentUser.id())
                && currentUser.hasPermission(AppPermission.LOAN_SELF_READ)) {
            return;
        }
        if (currentUser.hasPermission(AppPermission.LOAN_READ_GLOBAL)
                || currentUser.hasPermission(AppPermission.AUDIT_GLOBAL_READ)
                || currentUser.hasPermission(AppPermission.REPORT_GLOBAL_READ)) {
            return;
        }
        if (currentUser.scope() == AccessScope.BRANCH
                && currentUser.branchId() != null
                && currentUser.belongsToBranch(targetUser.getBranchId())
                && (currentUser.hasPermission(AppPermission.REPORT_BRANCH_READ)
                        || currentUser.hasPermission(AppPermission.LOAN_CLOSE_BRANCH))) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to view this borrowing");
    }

    public void assertCanManageReservationForUser(AppUser targetUser) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (targetUser.getId() != null
                && targetUser.getId().equals(currentUser.id())
                && currentUser.hasPermission(AppPermission.RESERVATION_SELF_CANCEL)) {
            return;
        }
        if (canMutateGlobally(currentUser)) {
            return;
        }
        if (currentUser.scope() == AccessScope.BRANCH
                && currentUser.hasPermission(AppPermission.RESERVATION_MANAGE_BRANCH)
                && currentUser.branchId() != null
                && currentUser.belongsToBranch(targetUser.getBranchId())) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to manage this reservation");
    }

    public void assertCanReadUser(AppUser targetUser) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (targetUser.getId() != null && targetUser.getId().equals(currentUser.id())) {
            return;
        }
        if (currentUser.hasPermission(AppPermission.USER_READ_GLOBAL)
                || currentUser.hasPermission(AppPermission.USER_MANAGE_GLOBAL)) {
            return;
        }
        if (currentUser.scope() == AccessScope.BRANCH
                && currentUser.hasPermission(AppPermission.MEMBER_READ_BRANCH)
                && currentUser.branchId() != null
                && currentUser.belongsToBranch(targetUser.getBranchId())
                && canBranchControlTarget(currentUser, targetUser)) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to view this user");
    }

    public boolean canCheckoutForMember() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        return isActive(currentUser)
                && (currentUser.hasPermission(AppPermission.LOAN_CREATE_BRANCH) || canMutateGlobally(currentUser));
    }

    public void assertCanManageUser(AppUser targetUser, UserAccessUpdateRequest request) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (currentUser.hasPermission(AppPermission.USER_MANAGE_GLOBAL) && canMutateGlobally(currentUser)) {
            return;
        }
        if (currentUser.scope() != AccessScope.BRANCH
                || currentUser.branchId() == null
                || !currentUser.belongsToBranch(targetUser.getBranchId())
                || !canBranchControlTarget(currentUser, targetUser)) {
            throw new AccessDeniedException("You do not have permission to manage this user");
        }

        if (request.role() != targetUser.getRole()) {
            throw new AccessDeniedException("Only administrators can change roles");
        }
        if (!branchAssignmentValid(request.branchId(), currentUser.branchId())
                || !branchAssignmentValid(request.homeBranchId(), currentUser.branchId())) {
            throw new AccessDeniedException("Branch-scoped staff can only manage users inside their branch");
        }

        if (currentUser.hasPermission(AppPermission.APPROVAL_BRANCH)) {
            if (targetUser.getRole() == AppRole.LIBRARIAN) {
                if (!EnumSet.of(AccountStatus.ACTIVE, AccountStatus.SUSPENDED).contains(request.accountStatus())
                        || request.membershipStatus() != targetUser.getMembershipStatus()) {
                    throw new AccessDeniedException("This role cannot apply the requested access change");
                }
                return;
            }
            if (!BRANCH_MANAGEABLE_ACCOUNT_STATUSES.contains(request.accountStatus())) {
                throw new AccessDeniedException("This role cannot apply the requested access change");
            }
            return;
        }

        if (!currentUser.hasPermission(AppPermission.MEMBER_VERIFY_BRANCH)
                || request.membershipStatus() != targetUser.getMembershipStatus()
                || !BRANCH_MANAGEABLE_ACCOUNT_STATUSES.contains(request.accountStatus())) {
            throw new AccessDeniedException("This role cannot apply the requested access change");
        }
    }

    public void assertCanRegisterStaff() {
        if (canRegisterStaff()) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to register staff accounts");
    }

    public void assertCanApplyUserDiscipline(AppUser targetUser, UserDisciplineActionType action) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (targetUser.getId() != null && targetUser.getId().equals(currentUser.id())) {
            throw new AccessDeniedException("You cannot discipline your own account");
        }

        if (canMutateGlobally(currentUser)) {
            return;
        }

        if (currentUser.scope() != AccessScope.BRANCH
                || currentUser.branchId() == null
                || !currentUser.belongsToBranch(targetUser.getBranchId())
                || !canBranchControlTarget(currentUser, targetUser)) {
            throw new AccessDeniedException("You do not have permission to discipline this user");
        }

        if (action == UserDisciplineActionType.BAN) {
            if (!currentUser.hasPermission(AppPermission.APPROVAL_BRANCH)) {
                throw new AccessDeniedException("Only branch managers or administrators can ban users");
            }
            return;
        }

        if (!currentUser.hasPermission(AppPermission.MEMBER_VERIFY_BRANCH)
                && !currentUser.hasPermission(AppPermission.APPROVAL_BRANCH)) {
            throw new AccessDeniedException("This role cannot discipline users");
        }
    }

    private boolean canBranchControlTarget(CurrentUser currentUser, AppUser targetUser) {
        return switch (currentUser.role()) {
            case LIBRARIAN -> false;
            case BRANCH_MANAGER -> targetUser.getRole() == AppRole.MEMBER || targetUser.getRole() == AppRole.LIBRARIAN;
            default -> false;
        };
    }

    public void assertCanWaiveFineForUser(AppUser targetUser, BigDecimal amount, BigDecimal branchLimit) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (canMutateGlobally(currentUser)) {
            return;
        }
        if (currentUser.scope() == AccessScope.BRANCH
                && currentUser.hasPermission(AppPermission.FINE_WAIVE_BRANCH)
                && currentUser.branchId() != null
                && currentUser.belongsToBranch(targetUser.getBranchId())
                && (branchLimit == null || amount.compareTo(branchLimit) <= 0)) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to waive this fine");
    }

    public void assertBorrowAllowed() {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (!currentUser.canBorrowForSelf()) {
            throw new AccessDeniedException("Borrowing is not allowed for the current account status or role");
        }
    }

    public void assertCanManageBranchInventory(Long branchId) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (branchId == null) {
            if (canMutateGlobally(currentUser)) {
                return;
            }
            throw new AccessDeniedException("Branch assignment is required");
        }
        if (canMutateGlobally(currentUser)) {
            return;
        }
        if (currentUser.scope() == AccessScope.BRANCH
                && currentUser.branchId() != null
                && currentUser.belongsToBranch(branchId)
                && canManageInventory()) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to manage inventory for this branch");
    }

    private boolean isActive(CurrentUser currentUser) {
        return currentUser.hasActiveAccount();
    }

    private boolean canMutateGlobally(CurrentUser currentUser) {
        return currentUser.scope() == AccessScope.GLOBAL
                && !currentUser.isReadOnlyRole()
                && currentUser.role() == AppRole.ADMIN;
    }

    private boolean branchAssignmentValid(Long requestedBranchId, Long allowedBranchId) {
        return requestedBranchId != null && requestedBranchId.equals(allowedBranchId);
    }
}
