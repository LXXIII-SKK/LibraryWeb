package com.example.library.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import com.example.library.branch.BranchService;
import com.example.library.branch.LibraryBranch;
import com.example.library.identity.AccountStatus;
import com.example.library.identity.AppRole;
import com.example.library.identity.AppUser;
import com.example.library.identity.AppUserRepository;
import com.example.library.identity.AuthorizationService;
import com.example.library.identity.CurrentUser;
import com.example.library.identity.CurrentUserService;
import com.example.library.identity.MembershipStatus;
import com.example.library.identity.UserDisciplineActionType;
import com.example.library.identity.UserDisciplineReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTests {

    @Mock
    private StaffNotificationRepository staffNotificationRepository;

    @Mock
    private StaffNotificationReceiptRepository staffNotificationReceiptRepository;

    @Mock
    private BranchService branchService;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuthorizationService authorizationService;

    @Test
    void librarianCanEscalateDisciplineRequestToManagerAndAdmin() {
        NotificationService service = new NotificationService(
                staffNotificationRepository,
                staffNotificationReceiptRepository,
                branchService,
                appUserRepository,
                currentUserService,
                authorizationService);
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
        AppUser requester = new AppUser(
                "librarian-1",
                "branch.librarian",
                "librarian@library.local",
                AppRole.LIBRARIAN,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        AppUser targetUser = new AppUser(
                "member-1",
                "central.member",
                "central.member@library.local",
                AppRole.MEMBER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        AppUser branchManager = new AppUser(
                "manager-1",
                "branch.manager",
                "manager@library.local",
                AppRole.BRANCH_MANAGER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        AppUser admin = new AppUser(
                "admin-1",
                "admin",
                "admin@library.local",
                AppRole.ADMIN,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                (Long) null,
                (Long) null);
        LibraryBranch branch = new LibraryBranch("CENTRAL", "Central Knowledge Library", null, null, true);
        ReflectionTestUtils.setField(requester, "id", 10L);
        ReflectionTestUtils.setField(targetUser, "id", 21L);
        ReflectionTestUtils.setField(branchManager, "id", 11L);
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(branch, "id", 3L);

        when(currentUserService.getCurrentUser()).thenReturn(librarian);
        when(currentUserService.getCurrentUserEntity()).thenReturn(requester);
        when(authorizationService.canRequestUserDiscipline()).thenReturn(true);
        when(appUserRepository.findByUsername("central.member")).thenReturn(Optional.of(targetUser));
        when(branchService.resolveBranch(3L)).thenReturn(branch);
        when(appUserRepository.findAllByBranch_IdAndRoleOrderByUsernameAsc(3L, AppRole.BRANCH_MANAGER))
                .thenReturn(java.util.List.of(branchManager));
        when(appUserRepository.findAllByRoleOrderByUsernameAsc(AppRole.ADMIN))
                .thenReturn(java.util.List.of(admin));
        when(staffNotificationRepository.save(any(StaffNotification.class))).thenAnswer(invocation -> {
            StaffNotification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", System.nanoTime());
            ReflectionTestUtils.setField(notification, "createdAt", Instant.parse("2026-03-20T08:00:00Z"));
            return notification;
        });

        DisciplineRequestNotificationResponse response = service.createDisciplineRequest(
                new CreateDisciplineRequestNotificationRequest(
                        "central.member",
                        UserDisciplineActionType.SUSPEND,
                        UserDisciplineReason.CONDUCT_VIOLATION,
                        "Escalating repeated disruption"));

        assertThat(response.targetUsername()).isEqualTo("central.member");
        assertThat(response.action()).isEqualTo(UserDisciplineActionType.SUSPEND);
        assertThat(response.notifiedRecipients()).containsExactly("branch.manager", "admin");
    }

    @Test
    void librarianCannotEscalateRequestForOtherBranchUser() {
        NotificationService service = new NotificationService(
                staffNotificationRepository,
                staffNotificationReceiptRepository,
                branchService,
                appUserRepository,
                currentUserService,
                authorizationService);
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
        AppUser targetUser = new AppUser(
                "member-1",
                "east.member",
                "east.member@library.local",
                AppRole.MEMBER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                4L,
                4L);

        when(currentUserService.getCurrentUser()).thenReturn(librarian);
        when(authorizationService.canRequestUserDiscipline()).thenReturn(true);
        when(appUserRepository.findByUsername("east.member")).thenReturn(Optional.of(targetUser));

        assertThatThrownBy(() -> service.createDisciplineRequest(
                new CreateDisciplineRequestNotificationRequest(
                        "east.member",
                        UserDisciplineActionType.BAN,
                        UserDisciplineReason.SECURITY_REVIEW,
                        "Outside assigned branch")))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessage("This user is outside your branch");
    }
}
