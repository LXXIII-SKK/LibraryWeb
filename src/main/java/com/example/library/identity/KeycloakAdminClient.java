package com.example.library.identity;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Service
class KeycloakAdminClient {

    private final KeycloakAdminProperties properties;
    private final RestClient restClient;

    KeycloakAdminClient(KeycloakAdminProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(normalizeBaseUrl(properties.baseUrl()))
                .build();
    }

    ProvisionedUser createStaffUser(
            String username,
            String email,
            String password,
            boolean requirePasswordChange,
            AppRole role) {
        String token = getAdminToken();
        String keycloakUserId = createUser(token, username, email, password, requirePasswordChange);
        try {
            assignRealmRole(token, keycloakUserId, role);
            return new ProvisionedUser(keycloakUserId, username, email);
        } catch (RuntimeException exception) {
            deleteUser(token, keycloakUserId);
            throw exception;
        }
    }

    void deleteUser(String keycloakUserId) {
        deleteUser(getAdminToken(), keycloakUserId);
    }

    private String getAdminToken() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", "admin-cli");
        body.add("username", properties.username());
        body.add("password", properties.password());
        body.add("grant_type", "password");

        KeycloakTokenResponse response = restClient.post()
                .uri("/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(KeycloakTokenResponse.class);
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new IllegalStateException("Keycloak admin token request returned no access token");
        }
        return response.accessToken();
    }

    private String createUser(
            String token,
            String username,
            String email,
            String password,
            boolean requirePasswordChange) {
        try {
            ResponseEntity<Void> response = restClient.post()
                    .uri("/admin/realms/{realm}/users", properties.realm())
                    .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new KeycloakCreateUserRequest(
                            username,
                            email,
                            true,
                            false,
                            List.of(new KeycloakCredentialRepresentation("password", password, requirePasswordChange))))
                    .retrieve()
                    .toBodilessEntity();

            URI location = response.getHeaders().getLocation();
            if (location != null) {
                return extractResourceId(location);
            }
            return findUserId(token, username);
        } catch (HttpClientErrorException.Conflict exception) {
            throw new IllegalArgumentException("A Keycloak user with that username or email already exists");
        }
    }

    private void assignRealmRole(String token, String keycloakUserId, AppRole role) {
        try {
            KeycloakRoleRepresentation roleRepresentation = restClient.get()
                    .uri("/admin/realms/{realm}/roles/{roleName}", properties.realm(), role.keycloakRoleName())
                    .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                    .retrieve()
                    .body(KeycloakRoleRepresentation.class);
            if (roleRepresentation == null || roleRepresentation.id() == null || roleRepresentation.name() == null) {
                throw new IllegalStateException("Keycloak returned an incomplete role representation");
            }

            restClient.post()
                    .uri("/admin/realms/{realm}/users/{userId}/role-mappings/realm", properties.realm(), keycloakUserId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(roleRepresentation))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound exception) {
            throw new IllegalStateException("Keycloak role %s is not available in realm %s"
                    .formatted(role.keycloakRoleName(), properties.realm()));
        }
    }

    private String findUserId(String token, String username) {
        KeycloakUserSummary[] users = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/users")
                        .queryParam("username", username)
                        .queryParam("exact", true)
                        .build(properties.realm()))
                .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                .retrieve()
                .body(KeycloakUserSummary[].class);
        if (users == null) {
            throw new IllegalStateException("Keycloak user creation succeeded but no user was returned for lookup");
        }
        return Arrays.stream(users)
                .filter(user -> username.equalsIgnoreCase(user.username()))
                .findFirst()
                .map(KeycloakUserSummary::id)
                .orElseThrow(() -> new IllegalStateException(
                        "Keycloak user creation succeeded but the new user id could not be resolved"));
    }

    private void deleteUser(String token, String keycloakUserId) {
        try {
            restClient.delete()
                    .uri("/admin/realms/{realm}/users/{userId}", properties.realm(), keycloakUserId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken(token))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound exception) {
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("Keycloak admin base URL is not configured");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String extractResourceId(URI location) {
        String path = location.getPath();
        int separator = path.lastIndexOf('/');
        if (separator < 0 || separator == path.length() - 1) {
            throw new IllegalStateException("Keycloak returned an unreadable user location header");
        }
        return path.substring(separator + 1);
    }

    private String bearerToken(String token) {
        return "Bearer " + token;
    }

    record ProvisionedUser(String keycloakUserId, String username, String email) {
    }

    private record KeycloakTokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    private record KeycloakCreateUserRequest(
            String username,
            String email,
            boolean enabled,
            boolean emailVerified,
            List<KeycloakCredentialRepresentation> credentials) {
    }

    private record KeycloakCredentialRepresentation(
            String type,
            String value,
            boolean temporary) {
    }

    private record KeycloakRoleRepresentation(
            String id,
            String name,
            boolean composite,
            boolean clientRole,
            String containerId) {
    }

    private record KeycloakUserSummary(
            String id,
            String username) {
    }
}
