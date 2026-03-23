package com.example.library.identity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    private final AppUserRepository appUserRepository;

    public CurrentUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public CurrentUser getCurrentUser() {
        AppUser saved = resolveCurrentUserEntity();
        return new CurrentUser(
                saved.getId(),
                saved.getKeycloakUserId(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getRole(),
                saved.getAccountStatus(),
                saved.getMembershipStatus(),
                saved.getBranchId(),
                saved.getHomeBranchId());
    }

    @Transactional
    public AppUser getCurrentUserEntity() {
        return resolveCurrentUserEntity();
    }

    @Transactional(readOnly = true)
    public ProfileResponse getCurrentProfile() {
        return ProfileResponse.from(getCurrentUserEntity());
    }

    private JwtAuthenticationToken currentAuthentication() {
        return (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
    }

    private AppUser resolveCurrentUserEntity() {
        JwtAuthenticationToken authentication = currentAuthentication();
        Jwt jwt = authentication.getToken();

        String subject = jwt.getSubject();
        String username = firstNonBlank(
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("name"),
                subject);
        String email = jwt.getClaimAsString("email");
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        AppUser user = appUserRepository.findByKeycloakUserId(subject)
                .or(() -> appUserRepository.findByUsername(username).map(existing -> relinkSeededUser(existing, subject)))
                .map(existing -> {
                    existing.synchronizeIdentity(username, email);
                    return existing;
                })
                .orElseGet(() -> bootstrapUser(
                        subject,
                        username,
                        email,
                        authorities,
                        jwt.getClaimAsMap("realm_access")));

        return appUserRepository.save(user);
    }

    private AppUser bootstrapUser(
            String subject,
            String username,
            String email,
            List<String> authorities,
            Map<String, Object> realmAccess) {
        AppRole bootstrapRole = resolveBootstrapRole(authorities, realmAccess);
        if (bootstrapRole != AppRole.MEMBER) {
            throw new AccessDeniedException(
                    "This identity requires local staff/admin provisioning before it can access the application");
        }
        return new AppUser(
                subject,
                username,
                email,
                bootstrapRole);
    }

    private AppUser relinkSeededUser(AppUser existing, String subject) {
        if (!isSeedIdentity(existing.getKeycloakUserId())) {
            throw new AccessDeniedException(
                    "This local account is already linked to another identity and cannot be rebound automatically");
        }
        existing.relinkToKeycloakUser(subject);
        return existing;
    }

    private AppRole resolveBootstrapRole(Collection<String> authorities, Map<String, Object> realmAccess) {
        List<String> candidates = List.copyOf(authorities);
        List<String> realmCandidates = readRealmRoles(realmAccess).stream().toList();
        return AppRole.resolve(
                List.of(candidates, realmCandidates).stream()
                        .flatMap(Collection::stream)
                        .toList())
                .orElseThrow(() -> new AccessDeniedException(
                        "Authenticated principal is missing a recognized library role"));
    }

    private Collection<String> readRealmRoles(Map<String, Object> realmAccess) {
        if (realmAccess == null) {
            return List.of();
        }
        Object roles = realmAccess.get("roles");
        if (roles instanceof Collection<?> values) {
            return values.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Authenticated principal is missing a usable username");
    }

    private boolean isSeedIdentity(String keycloakUserId) {
        return keycloakUserId != null && keycloakUserId.startsWith("seed-");
    }
}
