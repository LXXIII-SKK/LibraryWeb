import { formatDateTime } from "../lib/format";
import { ActivityPanel } from "./ActivityPanel";
import { ProfilePanel } from "./ProfilePanel";
import type { UserHubPageProps } from "../view-models";

export function UserHubPage({
  signedIn,
  canViewPersonalHistory,
  canReturnOwnBorrowings,
  canRenewOwnBorrowings,
  canViewReservations,
  canCancelOwnReservations,
  canViewFines,
  profile,
  borrowings,
  reservations,
  fines,
  notifications,
  logs,
  stats,
  onLogin,
  onRegister,
  onManageAccount,
  onOpenBook,
  onReturn,
  onRenew,
  onOpenDigitalAccess,
  onCollectReservation,
  onCancelReservation,
  onMarkNotificationRead,
}: UserHubPageProps) {
  if (!signedIn || !profile) {
    return (
      <section className="surface empty-user-page">
        <p className="section-label">My Page</p>
        <h2>Sign in to access your personal library workspace.</h2>
        <p className="hero-text">
          Your page includes account details, current borrowings, due dates, reservations, fines,
          and your recent activity history.
        </p>
        <div className="hero-actions">
          <button onClick={onLogin}>Login</button>
          <button className="button-secondary" onClick={onRegister}>
            Register
          </button>
        </div>
      </section>
    );
  }

  const overdueBorrowings = borrowings.filter(
    (borrowing) => borrowing.status === "BORROWED" && new Date(borrowing.dueAt).getTime() < Date.now(),
  );

  return (
    <section className="page-stack">
      <section className="surface user-hero">
        <div className="section-heading">
          <div>
            <p className="section-label">My Page</p>
            <h2>Your profile, current loans, reservations, and fines in one place.</h2>
          </div>
          <div className="status-chip">
            {stats.active} active / {stats.returned} returned
          </div>
        </div>
      </section>

      <section className="workspace-grid user-grid">
        <ProfilePanel profile={profile} onManageAccount={onManageAccount} />
        <div className="surface user-borrowings">
          <div className="section-heading">
            <div>
              <p className="section-label">Borrowings</p>
              <h2>Current and previous loans</h2>
            </div>
          </div>
          {canViewPersonalHistory ? (
            <>
              <div className="stack-list">
                {borrowings.map((borrowing) => (
                  <article key={borrowing.id} className="list-card">
                    <div>
                      <button className="link-button title-link" onClick={() => onOpenBook(borrowing.bookId)}>
                        {borrowing.bookTitle}
                      </button>
                      <p>
                        Borrowed {formatDateTime(borrowing.borrowedAt)}. Due {formatDateTime(borrowing.dueAt)}.
                      </p>
                      <span className={`pill pill-${borrowing.status.toLowerCase()}`}>{borrowing.status}</span>
                    </div>
                    <div className="inline-actions">
                      {borrowing.digitalAccessAvailable ? (
                        <button className="button-secondary" onClick={() => onOpenDigitalAccess(borrowing.id)}>
                          Open online access
                        </button>
                      ) : null}
                      <button
                        className="button-secondary"
                        onClick={() => onRenew(borrowing.id)}
                        disabled={!canRenewOwnBorrowings || borrowing.status === "RETURNED"}
                      >
                        Renew
                      </button>
                      <button
                        onClick={() => onReturn(borrowing.id)}
                        disabled={!canReturnOwnBorrowings || borrowing.status === "RETURNED"}
                      >
                        {borrowing.status === "RETURNED" ? "Returned" : "Return"}
                      </button>
                    </div>
                  </article>
                ))}
              </div>
              {borrowings.length === 0 ? <p className="empty-state">No borrowing history yet for this account.</p> : null}
            </>
          ) : (
            <p className="empty-state">This role does not have self-service borrowing history access.</p>
          )}
        </div>

        <div className="surface">
          <div className="section-heading">
            <div>
              <p className="section-label">Reservations</p>
              <h2>Your active and historical holds</h2>
            </div>
          </div>
          {canViewReservations ? (
            <>
              <div className="stack-list">
                {reservations.map((reservation) => (
                  <article key={reservation.id} className="list-card">
                    <div>
                      <button className="link-button title-link" onClick={() => onOpenBook(reservation.bookId)}>
                        {reservation.bookTitle}
                      </button>
                      <p>
                        Reserved {formatDateTime(reservation.reservedAt)}
                        {reservation.pickupBranch ? ` | pickup ${reservation.pickupBranch.name}` : ""}
                      </p>
                      {reservation.readyAt ? <p>Ready {formatDateTime(reservation.readyAt)}</p> : null}
                      {reservation.expiresAt ? <p>Expires {formatDateTime(reservation.expiresAt)}</p> : null}
                      <span className={`pill pill-${reservation.status.toLowerCase()}`}>{reservation.status}</span>
                    </div>
                    <div className="inline-actions">
                      {reservation.status === "READY_FOR_PICKUP" ? (
                        <button onClick={() => onCollectReservation(reservation.id)}>Collect</button>
                      ) : null}
                      <button
                        className="button-secondary"
                        onClick={() => onCancelReservation(reservation.id)}
                        disabled={
                          !canCancelOwnReservations ||
                          !["ACTIVE", "IN_TRANSIT", "READY_FOR_PICKUP"].includes(reservation.status)
                        }
                      >
                        {["ACTIVE", "IN_TRANSIT", "READY_FOR_PICKUP"].includes(reservation.status) ? "Cancel" : "Closed"}
                      </button>
                    </div>
                  </article>
                ))}
              </div>
              {reservations.length === 0 ? <p className="empty-state">No reservations are attached to this account.</p> : null}
            </>
          ) : (
            <p className="empty-state">This role does not have reservation access.</p>
          )}
        </div>

        <div className="surface">
          <div className="section-heading">
            <div>
              <p className="section-label">Overdue and fines</p>
              <h2>Open restrictions and payable issues</h2>
            </div>
          </div>
          {canViewFines ? (
            <>
              <div className="stack-list">
                {overdueBorrowings.map((borrowing) => (
                  <article key={`overdue-${borrowing.id}`} className="list-card">
                    <div>
                      <strong>{borrowing.bookTitle}</strong>
                      <p>Overdue since {formatDateTime(borrowing.dueAt)}</p>
                    </div>
                    <span className="pill pill-borrowed">OVERDUE</span>
                  </article>
                ))}
                {fines.map((fine) => (
                  <article key={fine.id} className="list-card">
                    <div>
                      <strong>{fine.bookTitle ?? "Manual fine"}</strong>
                      <p>{fine.reason}</p>
                    </div>
                    <div className="fine-chip">
                      <strong>${fine.amount.toFixed(2)}</strong>
                      <span className={`pill pill-${fine.status.toLowerCase()}`}>{fine.status}</span>
                    </div>
                  </article>
                ))}
              </div>
              {overdueBorrowings.length === 0 && fines.length === 0 ? (
                <p className="empty-state">No overdue items or fine records are attached to this account.</p>
              ) : null}
            </>
          ) : (
            <p className="empty-state">This role does not have fine access.</p>
          )}
        </div>

        <div className="surface">
          <div className="section-heading">
            <div>
              <p className="section-label">Notifications</p>
              <h2>Library notices and reservation updates</h2>
            </div>
          </div>
          <div className="stack-list">
            {notifications.map((notification) => (
              <article key={notification.id} className="list-card notification-card">
                <div>
                  <strong>{notification.title}</strong>
                  <p>{notification.message}</p>
                  <small>{formatDateTime(notification.createdAt)}</small>
                </div>
                {!notification.readAt ? (
                  <button className="button-secondary" onClick={() => onMarkNotificationRead(notification.id)}>
                    Mark read
                  </button>
                ) : (
                  <span className="pill">Read</span>
                )}
              </article>
            ))}
          </div>
          {notifications.length === 0 ? <p className="empty-state">No notifications are attached to this account.</p> : null}
        </div>

        <ActivityPanel
          label={canViewPersonalHistory ? "Activity Feed" : "Role Context"}
          title={canViewPersonalHistory ? "Your recent activity" : "Read-only operational profile"}
          emptyMessage={
            canViewPersonalHistory ? "No activity has been recorded yet." : "No personal activity is recorded for this role."
          }
          logs={logs}
        />
      </section>
    </section>
  );
}
