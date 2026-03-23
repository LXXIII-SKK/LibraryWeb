package com.example.library.integration;

import java.util.List;
import java.util.Map;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
abstract class PostgresBackedIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("library")
            .withUsername("library_app")
            .withPassword("library_app_pw");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "http://localhost/fake-jwks.json");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost/fake-issuer");
        registry.add("management.opentelemetry.enabled", () -> "false");
        registry.add("app.keycloak.admin.base-url", () -> "http://localhost:8081");
    }

    protected RequestPostProcessor adminJwt() {
        return jwt()
                .jwt(token -> token
                        .subject("00000000-0000-4000-8000-000000000001")
                        .claim("preferred_username", "admin")
                        .claim("email", "admin@library.local")
                        .claim("realm_access", Map.of("roles", List.of("admin"))))
                .authorities(() -> "ROLE_ADMIN");
    }

    protected RequestPostProcessor readerJwt() {
        return jwt()
                .jwt(token -> token
                        .subject("00000000-0000-4000-8000-000000000002")
                        .claim("preferred_username", "reader")
                        .claim("email", "reader@library.local")
                        .claim("realm_access", Map.of("roles", List.of("member"))))
                .authorities(() -> "ROLE_MEMBER");
    }

    protected RequestPostProcessor branchManagerJwt() {
        return jwt()
                .jwt(token -> token
                        .subject("00000000-0000-4000-8000-000000000007")
                        .claim("preferred_username", "branch.manager")
                        .claim("email", "branch.manager@library.local")
                        .claim("realm_access", Map.of("roles", List.of("branch_manager"))))
                .authorities(() -> "ROLE_BRANCH_MANAGER");
    }
}
