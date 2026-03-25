package com.example.library.circulation;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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

@WebMvcTest(TransferController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.security.cors.allowed-origins=http://localhost:3000")
class TransferControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferService transferService;

    @MockitoBean(name = "authorizationService")
    private AuthorizationService authorizationService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void transfersRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/transfers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void transfersReturnForbiddenWhenAuthorizationServiceDeniesRead() throws Exception {
        when(authorizationService.canReadTransfers()).thenReturn(false);

        mockMvc.perform(get("/api/transfers").with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void transfersReturnOkWhenReadIsAllowed() throws Exception {
        when(authorizationService.canReadTransfers()).thenReturn(true);
        when(transferService.listManagedTransfers()).thenReturn(List.of());

        mockMvc.perform(get("/api/transfers").with(jwt()))
                .andExpect(status().isOk());
    }
}
