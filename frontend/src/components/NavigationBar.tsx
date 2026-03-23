import type { NavigationBarProps } from "../view-models";

export function NavigationBar({
  visible,
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
  onNavigateUpcoming,
  onNavigateAccount,
  onNavigateAdmin,
  onToggleNotifications,
  onLogin,
  onRegister,
  onLogout,
}: NavigationBarProps) {
  return (
    <div className={`topbar-shell${visible ? " topbar-shell-visible" : ""}`}>
      <header className="topbar" aria-label="Primary">
        <div className="topbar-brand">
          <span className="topbar-brand-mark">L</span>
          <div className="topbar-brand-copy">
            <p className="section-label">{signedIn ? "Library" : "Current session"}</p>
            <strong>{signedIn ? username : "Guest session"}</strong>
          </div>
        </div>

        <nav className="topbar-nav" aria-label="Primary navigation">
          <button className={currentPath === "home" ? "button-secondary is-active" : "button-secondary"} onClick={onNavigateHome}>
            Home
          </button>
          <button className={currentPath === "books" ? "button-secondary is-active" : "button-secondary"} onClick={onNavigateBooks}>
            Books
          </button>
          <button
            className={currentPath === "upcoming" ? "button-secondary is-active" : "button-secondary"}
            onClick={onNavigateUpcoming}
          >
            Upcoming
          </button>
          {signedIn ? (
            <button className={currentPath === "account" ? "button-secondary is-active" : "button-secondary"} onClick={onNavigateAccount}>
              My page
            </button>
          ) : null}
          {canAccessOperations ? (
            <button className={currentPath === "admin" ? "button-secondary is-active" : "button-secondary"} onClick={onNavigateAdmin}>
              Ops
            </button>
          ) : null}
        </nav>

        <div className="topbar-account">
          <span className="pill topbar-role-pill">{signedIn ? roleLabel : "Anonymous"}</span>
          {canViewNotifications ? (
            <button className={notificationsOpen ? "button-secondary is-active" : "button-secondary"} onClick={onToggleNotifications}>
              !{unreadNotificationCount > 0 ? ` ${unreadNotificationCount}` : ""}
            </button>
          ) : null}
          {signedIn ? (
            <button onClick={onLogout}>Logout</button>
          ) : (
            <>
              <button onClick={onLogin}>Login</button>
              <button className="button-secondary" onClick={onRegister}>
                Join
              </button>
            </>
          )}
        </div>
      </header>
    </div>
  );
}
