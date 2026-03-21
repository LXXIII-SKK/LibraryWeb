import type { NavigationBarProps } from "../view-models";

export function NavigationBar({
  signedIn,
  canAccessOperations,
  canViewNotifications,
  unreadNotificationCount,
  username,
  roleLabel,
  currentPath,
  notificationsOpen,
  onNavigateHome,
  onNavigateBooks,
  onNavigateAccount,
  onNavigateAdmin,
  onToggleNotifications,
  onLogin,
  onRegister,
  onLogout,
}: NavigationBarProps) {
  return (
    <header className="topbar surface">
      <div className="topbar-brand">
        <p className="section-label">Library Web</p>
        <strong>Circulation & discovery workspace</strong>
      </div>

      <nav className="topbar-nav" aria-label="Primary">
        <button className={currentPath === "home" ? "button-secondary is-active" : "button-secondary"} onClick={onNavigateHome}>
          Home
        </button>
        <button className={currentPath === "books" ? "button-secondary is-active" : "button-secondary"} onClick={onNavigateBooks}>
          Books
        </button>
        <button className={currentPath === "account" ? "button-secondary is-active" : "button-secondary"} onClick={onNavigateAccount}>
          My page
        </button>
        {canAccessOperations ? (
          <button className={currentPath === "admin" ? "button-secondary is-active" : "button-secondary"} onClick={onNavigateAdmin}>
            Operations
          </button>
        ) : null}
      </nav>

      <div className="topbar-account">
        <div className="identity-inline">
          <span className="identity-label">{signedIn ? username : "Guest session"}</span>
          <span className="pill">{signedIn ? roleLabel : "Anonymous"}</span>
        </div>
        {canViewNotifications ? (
          <button className={notificationsOpen ? "button-secondary is-active" : "button-secondary"} onClick={onToggleNotifications}>
            Alerts {unreadNotificationCount > 0 ? `(${unreadNotificationCount})` : ""}
          </button>
        ) : null}
        {signedIn ? (
          <button onClick={onLogout}>Logout</button>
        ) : (
          <>
            <button onClick={onLogin}>Login</button>
            <button className="button-secondary" onClick={onRegister}>
              Register
            </button>
          </>
        )}
      </div>
    </header>
  );
}
