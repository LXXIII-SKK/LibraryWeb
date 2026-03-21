package com.example.library.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.example.library.branch.BranchService;
import com.example.library.branch.BranchSummaryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AccessManagementServiceTests {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private BranchService branchService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private UserDisciplineRecordRepository userDisciplineRecordRepository;

    @Test
    void branchLibrarianHasNoDirectUserListVisibility() {
        AccessManagementService service = new AccessManagementService(
                appUserRepository,
                branchService,
                currentUserService,
                authorizationService,
                applicationEventPublisher,
                userDisciplineRecordRepository);
        CurrentUser librarian = new CurrentUser(
                10L,
                "librarian-1",
                "branch.librarian",
                "librarian@library.local",
                AppRole.LIBRARIAN,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        when(currentUserService.getCurrentUser()).thenReturn(librarian);

        List<UserAccessResponse> users = service.listUsers();

        assertThat(users).isEmpty();
    }

    @Test
    void branchManagerListsMembersAndLibrariansOnly() {
        AccessManagementService service = new AccessManagementService(
                appUserRepository,
                branchService,
                currentUserService,
                authorizationService,
                applicationEventPublisher,
                userDisciplineRecordRepository);
        CurrentUser manager = new CurrentUser(
                11L,
                "manager-1",
                "branch.manager",
                "manager@library.local",
                AppRole.BRANCH_MANAGER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        AppUser member = new AppUser(
                "member-1",
                "central.member",
                "central.member@library.local",
                AppRole.MEMBER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        AppUser librarian = new AppUser(
                "librarian-1",
                "branch.librarian",
                "librarian@library.local",
                AppRole.LIBRARIAN,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(appUserRepository.findAllByBranch_IdAndRoleInOrderByUsernameAsc(
                3L,
                List.of(AppRole.MEMBER, AppRole.LIBRARIAN)))
                .thenReturn(List.of(member, librarian));

        List<UserAccessResponse> users = service.listUsers();

        assertThat(users).extracting(UserAccessResponse::role)
                .containsExactly(AppRole.MEMBER, AppRole.LIBRARIAN);
        assertThat(users).allSatisfy(user -> assertThat(user.permissions()).isNotEmpty());
    }

    @Test
    void branchLibrarianCannotReceiveEditableAccessOptionsForMember() {
        AccessManagementService service = new AccessManagementService(
                appUserRepository,
                branchService,
                currentUserService,
                authorizationService,
                applicationEventPublisher,
                userDisciplineRecordRepository);
        AppUser targetUser = new AppUser(
                "member-1",
                "reader",
                "reader@library.local",
                AppRole.MEMBER,
                AccountStatus.PENDING_VERIFICATION,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        CurrentUser librarian = new CurrentUser(
                10L,
                "librarian-1",
                "branch.librarian",
                "librarian@library.local",
                AppRole.LIBRARIAN,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(targetUser));
        when(currentUserService.getCurrentUser()).thenReturn(librarian);
        doNothing().when(authorizationService).assertCanReadUser(targetUser);

        AccessOptionsResponse options = service.optionsForUser(1L);

        assertThat(options.roles()).containsExactly(AppRole.MEMBER);
        assertThat(options.accountStatuses()).containsExactly(AccountStatus.PENDING_VERIFICATION);
        assertThat(options.membershipStatuses()).containsExactly(MembershipStatus.GOOD_STANDING);
        assertThat(options.branches()).extracting(BranchSummaryResponse::id).containsExactly(3L);
        assertThat(options.disciplineActions()).isEmpty();
        assertThat(options.disciplineReasons()).isEmpty();
    }

    @Test
    void adminSeesFullAccessOptionsForUser() {
        AccessManagementService service = new AccessManagementService(
                appUserRepository,
                branchService,
                currentUserService,
                authorizationService,
                applicationEventPublisher,
                userDisciplineRecordRepository);
        AppUser targetUser = new AppUser(
                "member-1",
                "reader",
                "reader@library.local",
                AppRole.MEMBER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        CurrentUser admin = new CurrentUser(
                1L,
                "admin-1",
                "admin",
                "admin@library.local",
                AppRole.ADMIN,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                null,
                null);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(targetUser));
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(branchService.listSummaries()).thenReturn(List.of(
                new BranchSummaryResponse(3L, "CENTRAL", "Central Knowledge Library", true),
                new BranchSummaryResponse(999L, "HQ", "Library Headquarters", true)));
        doNothing().when(authorizationService).assertCanReadUser(targetUser);

        AccessOptionsResponse options = service.optionsForUser(1L);

        assertThat(options.roles()).containsExactly(AppRole.values());
        assertThat(options.accountStatuses()).containsExactly(AccountStatus.values());
        assertThat(options.membershipStatuses()).containsExactly(MembershipStatus.values());
        assertThat(options.branches()).hasSize(2);
        assertThat(options.disciplineActions()).containsExactly(UserDisciplineActionType.SUSPEND, UserDisciplineActionType.BAN);
    }

    @Test
    void branchManagerSeesRestrictedAccessOptionsForLibrarian() {
        AccessManagementService service = new AccessManagementService(
                appUserRepository,
                branchService,
                currentUserService,
                authorizationService,
                applicationEventPublisher,
                userDisciplineRecordRepository);
        AppUser targetUser = new AppUser(
                "librarian-1",
                "branch.librarian",
                "librarian@library.local",
                AppRole.LIBRARIAN,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        CurrentUser manager = new CurrentUser(
                11L,
                "manager-1",
                "branch.manager",
                "manager@library.local",
                AppRole.BRANCH_MANAGER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        when(appUserRepository.findById(99L)).thenReturn(Optional.of(targetUser));
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(branchService.listSummariesByIds(List.of(3L))).thenReturn(List.of(
                new BranchSummaryResponse(3L, "CENTRAL", "Central Knowledge Library", true)));
        doNothing().when(authorizationService).assertCanReadUser(targetUser);

        AccessOptionsResponse options = service.optionsForUser(99L);

        assertThat(options.roles()).containsExactly(AppRole.LIBRARIAN);
        assertThat(options.accountStatuses()).containsExactly(AccountStatus.ACTIVE, AccountStatus.SUSPENDED);
        assertThat(options.membershipStatuses()).containsExactly(MembershipStatus.GOOD_STANDING);
        assertThat(options.disciplineActions()).containsExactly(
                UserDisciplineActionType.SUSPEND,
                UserDisciplineActionType.BAN);
    }

    @Test
    void branchManagerCanBanActiveMemberInSameBranch() {
        AccessManagementService service = new AccessManagementService(
                appUserRepository,
                branchService,
                currentUserService,
                authorizationService,
                applicationEventPublisher,
                userDisciplineRecordRepository);
        AppUser targetUser = new AppUser(
                "member-1",
                "reader",
                "reader@library.local",
                AppRole.MEMBER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        AppUser actorEntity = new AppUser(
                "manager-1",
                "branch.manager",
                "manager@library.local",
                AppRole.BRANCH_MANAGER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        CurrentUser manager = new CurrentUser(
                11L,
                "manager-1",
                "branch.manager",
                "manager@library.local",
                AppRole.BRANCH_MANAGER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        org.springframework.test.util.ReflectionTestUtils.setField(targetUser, "id", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(actorEntity, "id", 11L);
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(targetUser));
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(currentUserService.getCurrentUserEntity()).thenReturn(actorEntity);
        doNothing().when(authorizationService).assertCanApplyUserDiscipline(targetUser, UserDisciplineActionType.BAN);
        when(userDisciplineRecordRepository.save(any(UserDisciplineRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDisciplineRecordResponse response = service.applyUserDiscipline(
                1L,
                new UserDisciplineRequest(UserDisciplineActionType.BAN, UserDisciplineReason.CONDUCT_VIOLATION, "Repeated abuse"));

        assertThat(response.action()).isEqualTo(UserDisciplineActionType.BAN);
        assertThat(response.resultingAccountStatus()).isEqualTo(AccountStatus.LOCKED);
        assertThat(targetUser.getAccountStatus()).isEqualTo(AccountStatus.LOCKED);
    }
}
