package com.example.library.circulation;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.config.SecurityConfig;
import com.example.library.identity.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BorrowController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.security.cors.allowed-origins=http://localhost:3000")
class BorrowControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CirculationService circulationService;

    @MockitoBean(name = "authorizationService")
    private AuthorizationService authorizationService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void borrowingExceptionRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/borrowings/42/exception")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "CLAIM_RETURNED",
                                  "note": "Desk search started"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void borrowingExceptionReturnsForbiddenWhenAuthorizationDeniesDeskWorkflow() throws Exception {
        when(authorizationService.canManageBorrowingExceptions()).thenReturn(false);

        mockMvc.perform(post("/api/borrowings/42/exception")
                        .with(jwt())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "CLAIM_RETURNED",
                                  "note": "Desk search started"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void borrowingExceptionReturnsOkWhenAuthorized() throws Exception {
        when(authorizationService.canManageBorrowingExceptions()).thenReturn(true);
        when(circulationService.recordBorrowingException(
                org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.any(BorrowingExceptionRequest.class)))
                        .thenReturn(new BorrowTransactionResponse(
                                42L,
                                10L,
                                "Domain-Driven Design",
                                100L,
                                com.example.library.inventory.HoldingFormat.PHYSICAL,
                                "East Branch",
                                "Stacks A",
                                false,
                                7L,
                                "reader",
                                java.time.Instant.parse("2026-03-20T10:15:30Z"),
                                java.time.Instant.parse("2026-04-03T10:15:30Z"),
                                null,
                                0,
                                false,
                                null,
                                java.time.Instant.parse("2026-03-23T09:00:00Z"),
                                "Desk search started",
                                null,
                                BorrowStatus.CLAIMED_RETURNED));

        mockMvc.perform(post("/api/borrowings/42/exception")
                        .with(jwt())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "CLAIM_RETURNED",
                                  "note": "Desk search started"
                                }
                                """))
                .andExpect(status().isOk());
    }
}
