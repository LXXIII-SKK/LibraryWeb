package com.example.library.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTests {

    @Mock
    private CurrentUserService currentUserService;

    @Test
    void memberWithGoodStandingCanBorrowForSelf() {
        AuthorizationService authorizationService = new AuthorizationService(currentUserService);
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser(
                1L,
                "member-1",
                "reader",
                "reader@library.local",
                AppRole.MEMBER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                1L,
                1L));

        assertThat(authorizationService.canBorrowForSelf()).isTrue();
    }

    @Test
    void overdueMemberCannotBorrowForSelf() {
        AuthorizationService authorizationService = new AuthorizationService(currentUserService);
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser(
                1L,
                "member-1",
                "reader",
                "reader@library.local",
                AppRole.MEMBER,
                AccountStatus.ACTIVE,
                MembershipStatus.OVERDUE_RESTRICTED,
                1L,
                1L));

        assertThat(authorizationService.canBorrowForSelf()).isFalse();
    }

    @Test
    void branchLibrarianCanOnlyRequestManagerReviewAndManageBooks() {
        AuthorizationService authorizationService = new AuthorizationService(currentUserService);
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser(
                9L,
                "librarian-1",
                "librarian",
                "librarian@library.local",
                AppRole.LIBRARIAN,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                7L,
                7L));

        AppUser sameBranchMember = new AppUser(
                "member-7",
                "member.same",
                "same@library.local",
                AppRole.MEMBER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                7L,
                7L);
        AppUser otherBranchMember = new AppUser(
                "member-8",
                "member.other",
                "other@library.local",
                AppRole.MEMBER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                8L,
                8L);

        assertThat(authorizationService.canAccessReturnEndpoint()).isFalse();
        assertThat(authorizationService.canReadUsers()).isFalse();
        assertThat(authorizationService.canManageUsers()).isFalse();
        assertThat(authorizationService.canManageUserDiscipline()).isFalse();
        assertThat(authorizationService.canSendStaffNotifications()).isFalse();
        assertThat(authorizationService.canRequestUserDiscipline()).isTrue();
        assertThatThrownBy(() -> authorizationService.assertCanReturnBorrowingForUser(sameBranchMember))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> authorizationService.assertCanReturnBorrowingForUser(otherBranchMember))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> authorizationService.assertCanReadUser(sameBranchMember))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void auditorCannotUseReturnMutationPath() {
        AuthorizationService authorizationService = new AuthorizationService(currentUserService);
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser(
                40L,
                "auditor-1",
                "auditor",
                "auditor@library.local",
                AppRole.AUDITOR,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                null,
                null));

        assertThat(authorizationService.canAccessReturnEndpoint()).isFalse();
        assertThat(authorizationService.canRecordBookView()).isFalse();
    }

    @Test
    void onlyAdminCanDeleteCatalog() {
        AuthorizationService authorizationService = new AuthorizationService(currentUserService);
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser(
                50L,
                "librarian-2",
                "librarian",
                "librarian@library.local",
                AppRole.LIBRARIAN,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                2L,
                2L));

        assertThat(authorizationService.canDeleteCatalog()).isFalse();
    }

    @Test
    void branchManagerCanManageBranchUserAccessWithoutRoleChanges() {
        AuthorizationService authorizationService = new AuthorizationService(currentUserService);
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser(
                60L,
                "branch-manager-1",
                "branch.manager",
                "branch.manager@library.local",
                AppRole.BRANCH_MANAGER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L));

        AppUser target = new AppUser(
                "member-20",
                "member.twenty",
                "member.twenty@library.local",
                AppRole.MEMBER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        UserAccessUpdateRequest request = new UserAccessUpdateRequest(
                AppRole.MEMBER,
                AccountStatus.SUSPENDED,
                MembershipStatus.BORROW_BLOCKED,
                3L,
                3L);

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() ->
                authorizationService.assertCanManageUser(target, request))).isNull();
    }

    @Test
    void adminCanRegisterStaffAccounts() {
        AuthorizationService authorizationService = new AuthorizationService(currentUserService);
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser(
                70L,
                "admin-1",
                "admin",
                "admin@library.local",
                AppRole.ADMIN,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                null,
                null));

        assertThat(authorizationService.canRegisterStaff()).isTrue();
    }

    @Test
    void branchManagerCannotRegisterStaffAccounts() {
        AuthorizationService authorizationService = new AuthorizationService(currentUserService);
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser(
                71L,
                "manager-1",
                "branch.manager",
                "branch.manager@library.local",
                AppRole.BRANCH_MANAGER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L));

        assertThat(authorizationService.canRegisterStaff()).isFalse();
    }

    @Test
    void librarianCannotManageMemberAccessDirectly() {
        AuthorizationService authorizationService = new AuthorizationService(currentUserService);
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser(
                61L,
                "librarian-3",
                "branch.librarian",
                "branch.librarian@library.local",
                AppRole.LIBRARIAN,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L));

        AppUser target = new AppUser(
                "member-30",
                "member.thirty",
                "member.thirty@library.local",
                AppRole.MEMBER,
                AccountStatus.PENDING_VERIFICATION,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        UserAccessUpdateRequest request = new UserAccessUpdateRequest(
                AppRole.LIBRARIAN,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);

        assertThatThrownBy(() -> authorizationService.assertCanManageUser(target, request))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessage("You do not have permission to manage this user");
    }

    @Test
    void branchManagerCannotLockMemberAccount() {
        AuthorizationService authorizationService = new AuthorizationService(currentUserService);
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser(
                62L,
                "branch-manager-2",
                "branch.manager",
                "branch.manager@library.local",
                AppRole.BRANCH_MANAGER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L));

        AppUser target = new AppUser(
                "member-31",
                "member.thirtyone",
                "member.thirtyone@library.local",
                AppRole.MEMBER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        UserAccessUpdateRequest request = new UserAccessUpdateRequest(
                AppRole.MEMBER,
                AccountStatus.LOCKED,
                MembershipStatus.BORROW_BLOCKED,
                3L,
                3L);

        assertThatThrownBy(() -> authorizationService.assertCanManageUser(target, request))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessage("This role cannot apply the requested access change");
    }

    @Test
    void branchManagerCanReadLibrarianInSameBranch() {
        AuthorizationService authorizationService = new AuthorizationService(currentUserService);
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser(
                63L,
                "branch-manager-3",
                "branch.manager",
                "branch.manager@library.local",
                AppRole.BRANCH_MANAGER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L));

        AppUser target = new AppUser(
                "librarian-9",
                "east.librarian",
                "east.librarian@library.local",
                AppRole.LIBRARIAN,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() ->
                authorizationService.assertCanReadUser(target))).isNull();
    }

    @Test
    void librarianCannotReadBranchManagerAccount() {
        AuthorizationService authorizationService = new AuthorizationService(currentUserService);
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser(
                64L,
                "librarian-4",
                "branch.librarian",
                "branch.librarian@library.local",
                AppRole.LIBRARIAN,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L));

        AppUser target = new AppUser(
                "manager-9",
                "branch.manager",
                "branch.manager@library.local",
                AppRole.BRANCH_MANAGER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);

        assertThatThrownBy(() -> authorizationService.assertCanReadUser(target))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessage("You do not have permission to view this user");
    }
}
