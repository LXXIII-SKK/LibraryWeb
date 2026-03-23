package com.example.library.identity;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.example.library.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccessManagementController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.security.cors.allowed-origins=http://localhost:3000")
class AccessManagementControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccessManagementService accessManagementService;

    @MockitoBean
    private StaffRegistrationService staffRegistrationService;

    @MockitoBean(name = "authorizationService")
    private AuthorizationService authorizationService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void staffRegistrationRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/users/staff-registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "new.librarian",
                                  "email": "new.librarian@library.local",
                                  "password": "SecurePass123",
                                  "role": "LIBRARIAN",
                                  "accountStatus": "ACTIVE",
                                  "branchId": 1,
                                  "homeBranchId": 1,
                                  "requirePasswordChange": true
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffRegistrationReturnsForbiddenWhenAuthorizationDeniesAdminProvisioning() throws Exception {
        when(authorizationService.canRegisterStaff()).thenReturn(false);

        mockMvc.perform(post("/api/users/staff-registration")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "new.librarian",
                                  "email": "new.librarian@library.local",
                                  "password": "SecurePass123",
                                  "role": "LIBRARIAN",
                                  "accountStatus": "ACTIVE",
                                  "branchId": 1,
                                  "homeBranchId": 1,
                                  "requirePasswordChange": true
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffRegistrationOptionsReturnOkWhenAuthorized() throws Exception {
        when(authorizationService.canRegisterStaff()).thenReturn(true);
        when(staffRegistrationService.options()).thenReturn(new AccessOptionsResponse(
                List.of(AppRole.LIBRARIAN, AppRole.BRANCH_MANAGER, AppRole.AUDITOR, AppRole.ADMIN),
                List.of(AccountStatus.ACTIVE, AccountStatus.SUSPENDED, AccountStatus.LOCKED),
                List.of(MembershipStatus.GOOD_STANDING),
                List.of(),
                List.of(),
                List.of()));

        mockMvc.perform(get("/api/users/staff-registration/options").with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void staffRegistrationReturnsOkWhenAuthorized() throws Exception {
        when(authorizationService.canRegisterStaff()).thenReturn(true);
        when(staffRegistrationService.register(org.mockito.ArgumentMatchers.any())).thenReturn(new UserAccessResponse(
                99L,
                "new.librarian",
                "new.librarian@library.local",
                AppRole.LIBRARIAN,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                AccessScope.BRANCH,
                1L,
                1L,
                null,
                null,
                List.of("BOOK_CREATE")));

        mockMvc.perform(post("/api/users/staff-registration")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "new.librarian",
                                  "email": "new.librarian@library.local",
                                  "password": "SecurePass123",
                                  "role": "LIBRARIAN",
                                  "accountStatus": "ACTIVE",
                                  "branchId": 1,
                                  "homeBranchId": 1,
                                  "requirePasswordChange": true
                                }
                                """))
                .andExpect(status().isOk());
    }
}
