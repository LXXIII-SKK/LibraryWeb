package com.example.library.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.keycloak.admin")
public record KeycloakAdminProperties(
        String baseUrl,
        String realm,
        String username,
        String password) {
}
