import Keycloak from "keycloak-js";

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL ?? "http://localhost:8081",
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? "library",
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? "library-web",
});

export async function initAuth(): Promise<boolean> {
  return keycloak.init({
    onLoad: "check-sso",
    pkceMethod: "S256",
    silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
  });
}

export function login(): Promise<void> {
  return keycloak.login();
}

export function register(): Promise<void> {
  return keycloak.register();
}

export function logout(): Promise<void> {
  return keycloak.logout({ redirectUri: window.location.origin });
}

export function manageAccount(): Promise<void> {
  return keycloak.accountManagement();
}

export function isAuthenticated(): boolean {
  return Boolean(keycloak.authenticated);
}

export function token(): string | undefined {
  return keycloak.token;
}

export async function getAccessToken(forceRefresh = false): Promise<string | undefined> {
  if (!keycloak.authenticated) {
    return undefined;
  }

  try {
    await keycloak.updateToken(forceRefresh ? -1 : 30);
  } catch {
    keycloak.clearToken();
    throw new Error("Session expired. Please log in again.");
  }

  return keycloak.token;
}

export function username(): string {
  return keycloak.tokenParsed?.preferred_username ?? "guest";
}

export function hasRole(role: string): boolean {
  return keycloak.hasRealmRole(role);
}
