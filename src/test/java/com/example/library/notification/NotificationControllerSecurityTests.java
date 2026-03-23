package com.example.library.notification;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.example.library.config.SecurityConfig;
import com.example.library.identity.AppRole;
import com.example.library.identity.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.security.cors.allowed-origins=http://localhost:3000")
class NotificationControllerSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean(name = "authorizationService")
    private AuthorizationService authorizationService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void notificationsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void notificationsReturnForbiddenWhenAuthorizationServiceDeniesRead() throws Exception {
        when(authorizationService.canReadStaffNotifications()).thenReturn(false);

        mockMvc.perform(get("/api/notifications").with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void notificationsReturnOkWhenReadIsAllowed() throws Exception {
        when(authorizationService.canReadStaffNotifications()).thenReturn(true);
        when(notificationService.listCurrentNotifications()).thenReturn(List.of());

        mockMvc.perform(get("/api/notifications").with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void createNotificationRequiresSendPermission() throws Exception {
        when(authorizationService.canSendStaffNotifications()).thenReturn(false);

        mockMvc.perform(post("/api/notifications")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Desk notice",
                                  "message": "Updated workflow",
                                  "branchId": 3,
                                  "targetRoles": ["LIBRARIAN"]
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createNotificationReturnsOkWhenAuthorized() throws Exception {
        when(authorizationService.canSendStaffNotifications()).thenReturn(true);
        when(notificationService.create(org.mockito.ArgumentMatchers.any())).thenReturn(new StaffNotificationResponse(
                1L,
                "Desk notice",
                "Updated workflow",
                null,
                null,
                null,
                Set.of(AppRole.LIBRARIAN),
                "branch.manager",
                Instant.parse("2026-03-23T07:00:00Z"),
                null));

        mockMvc.perform(post("/api/notifications")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Desk notice",
                                  "message": "Updated workflow",
                                  "branchId": 3,
                                  "targetRoles": ["LIBRARIAN"]
                                }
                                """))
                .andExpect(status().isOk());
    }
}
