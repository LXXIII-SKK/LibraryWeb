package com.example.library.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTests {

    @Mock
    private AppUserRepository appUserRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void existingUserKeepsStoredAccessWhenTokenClaimsDiffer() {
        AppUser existingUser = new AppUser(
                "subject-1",
                "reader",
                "reader@library.local",
                AppRole.MEMBER,
                AccountStatus.SUSPENDED,
                MembershipStatus.BORROW_BLOCKED,
                7L,
                7L);
        when(appUserRepository.findByKeycloakUserId("subject-1")).thenReturn(Optional.of(existingUser));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(authentication(
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .subject("subject-1")
                        .claim("preferred_username", "reader")
                        .claim("email", "reader@library.local")
                        .claim("realm_access", java.util.Map.of("roles", List.of("ADMIN")))
                        .claim("library_account_status", "ACTIVE")
                        .claim("library_membership_status", "GOOD_STANDING")
                        .claim("library_branch_id", 1)
                        .build(),
                List.of("ROLE_ADMIN")));

        CurrentUserService currentUserService = new CurrentUserService(appUserRepository);
        CurrentUser currentUser = currentUserService.getCurrentUser();

        assertThat(currentUser.role()).isEqualTo(AppRole.MEMBER);
        assertThat(currentUser.accountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(currentUser.membershipStatus()).isEqualTo(MembershipStatus.BORROW_BLOCKED);
        assertThat(currentUser.branchId()).isEqualTo(7L);
    }

    @Test
    void firstTimeUserWithoutRecognizedRoleIsDenied() {
        when(appUserRepository.findByKeycloakUserId("subject-2")).thenReturn(Optional.empty());
        when(appUserRepository.findByUsername("new.user")).thenReturn(Optional.empty());
        SecurityContextHolder.getContext().setAuthentication(authentication(
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .subject("subject-2")
                        .claim("preferred_username", "new.user")
                        .claim("email", "new.user@library.local")
                        .build(),
                List.of("SCOPE_openid")));

        CurrentUserService currentUserService = new CurrentUserService(appUserRepository);

        assertThatThrownBy(currentUserService::getCurrentUser)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Authenticated principal is missing a recognized library role");
    }

    @Test
    void getCurrentUserEntityRelinksSeededUsernameWithoutSecondLookupFailure() {
        AppUser existingUser = new AppUser(
                "seed-alina-reader",
                "alina.reader",
                "alina.reader@library.local",
                AppRole.MEMBER);
        when(appUserRepository.findByKeycloakUserId("subject-3")).thenReturn(Optional.empty());
        when(appUserRepository.findByUsername("alina.reader")).thenReturn(Optional.of(existingUser));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(authentication(
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .subject("subject-3")
                        .claim("preferred_username", "alina.reader")
                        .claim("email", "alina.reader@library.local")
                        .claim("realm_access", java.util.Map.of("roles", List.of("MEMBER")))
                        .build(),
                List.of("ROLE_MEMBER")));

        CurrentUserService currentUserService = new CurrentUserService(appUserRepository);
        AppUser currentUser = currentUserService.getCurrentUserEntity();

        assertThat(currentUser.getUsername()).isEqualTo("alina.reader");
        assertThat(currentUser.getKeycloakUserId()).isEqualTo("subject-3");
        assertThat(currentUser.getRole()).isEqualTo(AppRole.MEMBER);
    }

    @Test
    void existingNonSeededUsernameCannotBeRelinkedAutomatically() {
        AppUser existingUser = new AppUser(
                "real-subject-1",
                "managed.user",
                "managed.user@library.local",
                AppRole.MEMBER);
        when(appUserRepository.findByKeycloakUserId("subject-4")).thenReturn(Optional.empty());
        when(appUserRepository.findByUsername("managed.user")).thenReturn(Optional.of(existingUser));
        SecurityContextHolder.getContext().setAuthentication(authentication(
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .subject("subject-4")
                        .claim("preferred_username", "managed.user")
                        .claim("email", "managed.user@library.local")
                        .claim("realm_access", java.util.Map.of("roles", List.of("MEMBER")))
                        .build(),
                List.of("ROLE_MEMBER")));

        CurrentUserService currentUserService = new CurrentUserService(appUserRepository);

        assertThatThrownBy(currentUserService::getCurrentUserEntity)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("This local account is already linked to another identity and cannot be rebound automatically");
    }

    @Test
    void firstTimeMemberBootstrapIgnoresAccessClaimsAndUsesServerDefaults() {
        when(appUserRepository.findByKeycloakUserId("subject-5")).thenReturn(Optional.empty());
        when(appUserRepository.findByUsername("new.member")).thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(authentication(
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .subject("subject-5")
                        .claim("preferred_username", "new.member")
                        .claim("email", "new.member@library.local")
                        .claim("realm_access", java.util.Map.of("roles", List.of("MEMBER")))
                        .claim("library_account_status", "LOCKED")
                        .claim("library_membership_status", "BORROW_BLOCKED")
                        .claim("library_branch_id", 99)
                        .claim("library_home_branch_id", 88)
                        .build(),
                List.of("ROLE_MEMBER")));

        CurrentUserService currentUserService = new CurrentUserService(appUserRepository);
        CurrentUser currentUser = currentUserService.getCurrentUser();

        assertThat(currentUser.role()).isEqualTo(AppRole.MEMBER);
        assertThat(currentUser.accountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(currentUser.membershipStatus()).isEqualTo(MembershipStatus.GOOD_STANDING);
        assertThat(currentUser.branchId()).isNull();
        assertThat(currentUser.homeBranchId()).isNull();
    }

    private JwtAuthenticationToken authentication(Jwt jwt, List<String> authorities) {
        return new JwtAuthenticationToken(
                jwt,
                authorities.stream()
                        .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                        .toList());
    }
}
