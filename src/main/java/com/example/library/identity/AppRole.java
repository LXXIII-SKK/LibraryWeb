package com.example.library.identity;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public enum AppRole {
    MEMBER(
            AccessScope.SELF,
            EnumSet.of(
                    AppPermission.CATALOG_READ,
                    AppPermission.FINE_SELF_READ,
                    AppPermission.LOAN_SELF_READ,
                    AppPermission.LOAN_SELF_CREATE,
                    AppPermission.LOAN_SELF_RETURN,
                    AppPermission.LOAN_SELF_RENEW,
                    AppPermission.RESERVATION_SELF_CREATE,
                    AppPermission.RESERVATION_SELF_CANCEL,
                    AppPermission.PROFILE_SELF_UPDATE)),
    LIBRARIAN(
            AccessScope.BRANCH,
            EnumSet.of(
                    AppPermission.CATALOG_READ,
                    AppPermission.BOOK_CREATE,
                    AppPermission.BOOK_UPDATE,
                    AppPermission.COPY_CREATE,
                    AppPermission.COPY_UPDATE,
                    AppPermission.INVENTORY_CHECK_BRANCH,
                    AppPermission.DISCIPLINE_REQUEST_BRANCH)),
    BRANCH_MANAGER(
            AccessScope.BRANCH,
            EnumSet.of(
                    AppPermission.CATALOG_READ,
                    AppPermission.POLICY_READ,
                    AppPermission.BOOK_CREATE,
                    AppPermission.BOOK_UPDATE,
                    AppPermission.COPY_CREATE,
                    AppPermission.COPY_UPDATE,
                    AppPermission.MEMBER_READ_BRANCH,
                    AppPermission.MEMBER_VERIFY_BRANCH,
                    AppPermission.LOAN_CREATE_BRANCH,
                    AppPermission.LOAN_CLOSE_BRANCH,
                    AppPermission.RESERVATION_MANAGE_BRANCH,
                    AppPermission.INVENTORY_CHECK_BRANCH,
                    AppPermission.FINE_READ_BRANCH,
                    AppPermission.REPORT_BRANCH_READ,
                    AppPermission.FINE_WAIVE_BRANCH,
                    AppPermission.LOAN_OVERRIDE_BRANCH,
                    AppPermission.STAFF_VIEW_BRANCH,
                    AppPermission.APPROVAL_BRANCH)),
    ADMIN(
            AccessScope.GLOBAL,
            EnumSet.of(
                    AppPermission.CATALOG_READ,
                    AppPermission.POLICY_READ,
                    AppPermission.BOOK_CREATE,
                    AppPermission.BOOK_UPDATE,
                    AppPermission.COPY_CREATE,
                    AppPermission.COPY_UPDATE,
                    AppPermission.MEMBER_READ_BRANCH,
                    AppPermission.MEMBER_VERIFY_BRANCH,
                    AppPermission.LOAN_CREATE_BRANCH,
                    AppPermission.LOAN_CLOSE_BRANCH,
                    AppPermission.RESERVATION_MANAGE_BRANCH,
                    AppPermission.INVENTORY_CHECK_BRANCH,
                    AppPermission.FINE_WAIVE_BRANCH,
                    AppPermission.LOAN_OVERRIDE_BRANCH,
                    AppPermission.STAFF_VIEW_BRANCH,
                    AppPermission.APPROVAL_BRANCH,
                    AppPermission.FINE_READ_BRANCH,
                    AppPermission.USER_MANAGE_GLOBAL,
                    AppPermission.ROLE_ASSIGN_GLOBAL,
                    AppPermission.POLICY_MANAGE_GLOBAL,
                    AppPermission.BRANCH_MANAGE_GLOBAL,
                    AppPermission.REPORT_GLOBAL_READ,
                    AppPermission.AUDIT_GLOBAL_READ,
                    AppPermission.LOAN_READ_GLOBAL,
                    AppPermission.USER_READ_GLOBAL,
                    AppPermission.BOOK_READ_GLOBAL,
                    AppPermission.FINE_READ_GLOBAL,
                    AppPermission.SYSTEM_CONFIG_MANAGE)),
    AUDITOR(
            AccessScope.GLOBAL,
            EnumSet.of(
                    AppPermission.CATALOG_READ,
                    AppPermission.POLICY_READ,
                    AppPermission.USER_READ_GLOBAL,
                    AppPermission.BOOK_READ_GLOBAL,
                    AppPermission.LOAN_READ_GLOBAL,
                    AppPermission.FINE_READ_GLOBAL,
                    AppPermission.REPORT_GLOBAL_READ,
                    AppPermission.AUDIT_GLOBAL_READ));

    private final AccessScope scope;
    private final Set<AppPermission> permissions;

    AppRole(AccessScope scope, Set<AppPermission> permissions) {
        this.scope = scope;
        this.permissions = Set.copyOf(permissions);
    }

    public AccessScope scope() {
        return scope;
    }

    public Set<AppPermission> permissions() {
        return permissions;
    }

    public boolean hasPermission(AppPermission permission) {
        return permissions.contains(permission);
    }

    public boolean isReadOnly() {
        return this == AUDITOR;
    }

    public static Optional<AppRole> resolve(Collection<String> authorities) {
        return Arrays.stream(new AppRole[] { ADMIN, AUDITOR, BRANCH_MANAGER, LIBRARIAN, MEMBER })
                .filter(role -> authorities.stream().anyMatch(authority ->
                        authority.equalsIgnoreCase(role.name()) || authority.equalsIgnoreCase("ROLE_" + role.name())))
                .findFirst();
    }
}
