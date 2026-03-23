import Keycloak from "keycloak-js";

type TestAuthDriver = {
  initAuth?: () => boolean | Promise<boolean>;
  login?: () => void | Promise<void>;
  register?: () => void | Promise<void>;
  logout?: () => void | Promise<void>;
  manageAccount?: () => void | Promise<void>;
  isAuthenticated?: () => boolean;
  getAccessToken?: (forceRefresh?: boolean) => string | undefined | Promise<string | undefined>;
  username?: () => string;
  hasRole?: (role: string) => boolean;
};

declare global {
  interface Window {
    __LIBRARY_E2E_AUTH__?: TestAuthDriver;
  }
}

function resolveKeycloakUrl() {
  const configuredUrl = import.meta.env.VITE_KEYCLOAK_URL?.trim();
  if (!configuredUrl) {
    return "http://localhost:8081";
  }

  return new URL(configuredUrl, window.location.origin).toString().replace(/\/$/, "");
}

const keycloak = new Keycloak({
  url: resolveKeycloakUrl(),
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? "library",
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? "library-web",
});

function testDriver() {
  if (typeof window === "undefined") {
    return undefined;
  }
  return window.__LIBRARY_E2E_AUTH__;
}

export async function initAuth(): Promise<boolean> {
  const driver = testDriver();
  if (driver) {
    if (driver.initAuth) {
      return Boolean(await driver.initAuth());
    }
    return Boolean(driver.isAuthenticated?.() ?? false);
  }
  return keycloak.init({
    onLoad: "check-sso",
    pkceMethod: "S256",
    silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
  });
}

export function login(): Promise<void> {
  const driver = testDriver();
  if (driver?.login) {
    return Promise.resolve(driver.login());
  }
  return keycloak.login();
}

export function register(): Promise<void> {
  const driver = testDriver();
  if (driver?.register) {
    return Promise.resolve(driver.register());
  }
  return keycloak.register();
}

export function logout(): Promise<void> {
  const driver = testDriver();
  if (driver?.logout) {
    return Promise.resolve(driver.logout());
  }
  return keycloak.logout({ redirectUri: window.location.origin });
}

export function manageAccount(): Promise<void> {
  const driver = testDriver();
  if (driver?.manageAccount) {
    return Promise.resolve(driver.manageAccount());
  }
  return keycloak.accountManagement();
}

export function isAuthenticated(): boolean {
  const driver = testDriver();
  if (driver?.isAuthenticated) {
    return driver.isAuthenticated();
  }
  return Boolean(keycloak.authenticated);
}

export function token(): string | undefined {
  return keycloak.token;
}

export async function getAccessToken(forceRefresh = false): Promise<string | undefined> {
  const driver = testDriver();
  if (driver?.getAccessToken) {
    return driver.getAccessToken(forceRefresh);
  }

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
  const driver = testDriver();
  if (driver?.username) {
    return driver.username();
  }
  return keycloak.tokenParsed?.preferred_username ?? "guest";
}

export function hasRole(role: string): boolean {
  const driver = testDriver();
  if (driver?.hasRole) {
    return driver.hasRole(role);
  }
  return keycloak.hasRealmRole(role);
}
