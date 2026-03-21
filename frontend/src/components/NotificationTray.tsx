import { formatDateTime, humanizeToken } from "../lib/format";
import type { NotificationTrayProps } from "../view-models";

export function NotificationTray({ open, notifications, onMarkRead }: NotificationTrayProps) {
  if (!open) {
    return null;
  }

  return (
    <section className="surface notification-tray">
      <div className="section-heading">
        <div>
          <p className="section-label">Notifications</p>
          <h2>Your alerts and library notices</h2>
        </div>
      </div>

      <div className="stack-list">
        {notifications.map((notification) => (
          <article key={notification.id} className="list-card notification-card">
            <div>
              <div className="notification-head">
                <strong>{notification.title}</strong>
                <span className={`pill ${notification.readAt ? "" : "pill-borrowed"}`}>
                  {notification.readAt ? "Read" : "Unread"}
                </span>
              </div>
              <p>{notification.message}</p>
              <small>
                {notification.branch?.name ?? "Global"} for{" "}
                {notification.targetRoles.map((role) => humanizeToken(role)).join(", ")}. Sent by{" "}
                {notification.createdByUsername} on {formatDateTime(notification.createdAt)}.
              </small>
            </div>
            {!notification.readAt ? (
              <button className="button-secondary" onClick={() => onMarkRead(notification.id)}>
                Mark read
              </button>
            ) : null}
          </article>
        ))}
      </div>

      {notifications.length === 0 ? <p className="empty-state">No notifications are waiting for this account.</p> : null}
    </section>
  );
}
