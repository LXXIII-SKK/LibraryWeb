package com.example.library.branch;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.example.library.config.SecurityConfig;
import com.example.library.identity.AccountStatus;
import com.example.library.identity.AppRole;
import com.example.library.identity.AuthorizationService;
import com.example.library.identity.CurrentUser;
import com.example.library.identity.CurrentUserService;
import com.example.library.identity.MembershipStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BranchController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.security.cors.allowed-origins=http://localhost:3000")
class BranchControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BranchService branchService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean(name = "authorizationService")
    private AuthorizationService authorizationService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void publicBranchesAllowsAnonymousAccess() throws Exception {
        when(branchService.listPublicActive()).thenReturn(List.of());

        mockMvc.perform(get("/api/branches/public"))
                .andExpect(status().isOk());
    }

    @Test
    void listBranchesRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/branches"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listBranchesReturnsForbiddenWhenAuthorizationServiceDeniesAccess() throws Exception {
        when(authorizationService.canReadBranches()).thenReturn(false);

        mockMvc.perform(get("/api/branches").with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createBranchAllowsAuthorizedAdmin() throws Exception {
        when(authorizationService.canManageBranches()).thenReturn(true);
        when(branchService.create(org.mockito.ArgumentMatchers.any())).thenReturn(
                new LibraryBranchResponse(1L, "CENTRAL", "Central", null, null, true));

        mockMvc.perform(post("/api/branches")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "CENTRAL",
                                  "name": "Central",
                                  "address": "",
                                  "phone": "",
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void listBranchesAllowsAuthorizedGlobalUser() throws Exception {
        when(authorizationService.canReadBranches()).thenReturn(true);
        when(currentUserService.getCurrentUser()).thenReturn(new CurrentUser(
                1L,
                "admin-1",
                "admin",
                "admin@library.local",
                AppRole.ADMIN,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                null,
                null));
        when(branchService.listAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/branches").with(jwt()))
                .andExpect(status().isOk());
    }
}
