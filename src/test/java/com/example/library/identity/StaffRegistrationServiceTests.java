package com.example.library.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.example.library.branch.BranchService;
import com.example.library.branch.BranchSummaryResponse;
import com.example.library.branch.LibraryBranch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StaffRegistrationServiceTests {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private BranchService branchService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Test
    void optionsExposeStaffOnlyRolesAndRegistrationStatuses() {
        StaffRegistrationService service = service();
        when(branchService.listSummaries()).thenReturn(List.of(
                new BranchSummaryResponse(1L, "CENTRAL", "Central", true),
                new BranchSummaryResponse(999L, "HQ", "Headquarters", true)));

        AccessOptionsResponse options = service.options();

        assertThat(options.roles()).containsExactly(
                AppRole.LIBRARIAN,
                AppRole.BRANCH_MANAGER,
                AppRole.ADMIN,
                AppRole.AUDITOR);
        assertThat(options.accountStatuses()).containsExactly(
                AccountStatus.ACTIVE,
                AccountStatus.SUSPENDED,
                AccountStatus.LOCKED);
        assertThat(options.membershipStatuses()).containsExactly(MembershipStatus.GOOD_STANDING);
    }

    @Test
    void registerCreatesBranchStaffAndDefaultsHomeBranchToAssignedBranch() {
        StaffRegistrationService service = service();
        LibraryBranch central = branch(3L, "CENTRAL", "Central");
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
        when(appUserRepository.findByUsername("new.librarian")).thenReturn(Optional.empty());
        when(appUserRepository.findByEmailIgnoreCase("new.librarian@library.local")).thenReturn(Optional.empty());
        when(branchService.resolveBranch(3L)).thenReturn(central);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(keycloakAdminClient.createStaffUser(
                "new.librarian",
                "new.librarian@library.local",
                "SecurePass123",
                true,
                AppRole.LIBRARIAN))
                .thenReturn(new KeycloakAdminClient.ProvisionedUser(
                        "kc-1",
                        "new.librarian",
                        "new.librarian@library.local"));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 77L);
            return user;
        });

        UserAccessResponse response = service.register(new StaffRegistrationRequest(
                "New.Librarian",
                "NEW.LIBRARIAN@library.local",
                "SecurePass123",
                AppRole.LIBRARIAN,
                AccountStatus.ACTIVE,
                3L,
                null,
                true));

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        AppUser savedUser = userCaptor.getValue();

        assertThat(response.id()).isEqualTo(77L);
        assertThat(response.role()).isEqualTo(AppRole.LIBRARIAN);
        assertThat(savedUser.getUsername()).isEqualTo("new.librarian");
        assertThat(savedUser.getEmail()).isEqualTo("new.librarian@library.local");
        assertThat(savedUser.getBranchId()).isEqualTo(3L);
        assertThat(savedUser.getHomeBranchId()).isEqualTo(3L);
        assertThat(savedUser.getMembershipStatus()).isEqualTo(MembershipStatus.GOOD_STANDING);
    }

    @Test
    void registerRejectsMemberRoleBeforeProvisioning() {
        StaffRegistrationService service = service();

        assertThatThrownBy(() -> service.register(new StaffRegistrationRequest(
                "member.one",
                "member.one@library.local",
                "SecurePass123",
                AppRole.MEMBER,
                AccountStatus.ACTIVE,
                1L,
                1L,
                false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Staff registration cannot create member accounts");

        verifyNoInteractions(keycloakAdminClient);
    }

    @Test
    void registerDeletesProvisionedKeycloakUserWhenLocalSaveFails() {
        StaffRegistrationService service = service();
        LibraryBranch headquarters = branch(999L, "HQ", "Headquarters");
        when(appUserRepository.findByUsername("auditor.one")).thenReturn(Optional.empty());
        when(appUserRepository.findByEmailIgnoreCase("auditor.one@library.local")).thenReturn(Optional.empty());
        when(branchService.resolveBranch(999L)).thenReturn(headquarters);
        when(keycloakAdminClient.createStaffUser(
                "auditor.one",
                "auditor.one@library.local",
                "SecurePass123",
                false,
                AppRole.AUDITOR))
                .thenReturn(new KeycloakAdminClient.ProvisionedUser(
                        "kc-auditor-1",
                        "auditor.one",
                        "auditor.one@library.local"));
        when(appUserRepository.save(any(AppUser.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.register(new StaffRegistrationRequest(
                "auditor.one",
                "auditor.one@library.local",
                "SecurePass123",
                AppRole.AUDITOR,
                AccountStatus.ACTIVE,
                999L,
                null,
                false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A local user with that username or email already exists");

        verify(keycloakAdminClient).deleteUser("kc-auditor-1");
    }

    private StaffRegistrationService service() {
        return new StaffRegistrationService(
                appUserRepository,
                branchService,
                currentUserService,
                authorizationService,
                keycloakAdminClient,
                applicationEventPublisher);
    }

    private LibraryBranch branch(Long id, String code, String name) {
        LibraryBranch branch = new LibraryBranch(code, name, null, null, true);
        ReflectionTestUtils.setField(branch, "id", id);
        return branch;
    }
}
