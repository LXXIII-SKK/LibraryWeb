package com.example.library.identity;

import java.util.List;

public record CurrentUser(
        Long id,
        String keycloakUserId,
        String username,
        String email,
        AppRole role,
        AccountStatus accountStatus,
        MembershipStatus membershipStatus,
        Long branchId,
        Long homeBranchId) {

    public CurrentUser(Long id, String keycloakUserId, String username, String email, AppRole role) {
        this(id, keycloakUserId, username, email, role, AccountStatus.ACTIVE, MembershipStatus.GOOD_STANDING, null, null);
    }

    public boolean hasPermission(AppPermission permission) {
        return role.hasPermission(permission);
    }

    public AccessScope scope() {
        return role.scope();
    }

    public boolean hasActiveAccount() {
        return accountStatus == AccountStatus.ACTIVE;
    }

    public boolean canBorrowForSelf() {
        return role == AppRole.MEMBER && hasPermission(AppPermission.LOAN_SELF_CREATE)
                && hasActiveAccount() && membershipStatus.allowsBorrowing();
    }

    public boolean isReadOnlyRole() {
        return role.isReadOnly();
    }

    public boolean belongsToBranch(Long candidateBranchId) {
        return branchId != null && branchId.equals(candidateBranchId);
    }

    public List<String> permissionNames() {
        return role.permissions().stream()
                .map(Enum::name)
                .sorted()
                .toList();
    }
}
