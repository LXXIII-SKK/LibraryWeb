import { humanizeToken } from "../lib/format";
import type { LibraryBranch, StaffNotification, UserDisciplineActionType, UserDisciplineReason } from "../types";
import type { DisciplineRequestFormState, NotificationFormState } from "../view-models";

type NotificationsPanelProps = {
  notifications: StaffNotification[];
  notificationForm: NotificationFormState;
  disciplineRequestForm: DisciplineRequestFormState;
  branches: LibraryBranch[];
  canSendNotifications: boolean;
  canRequestDisciplineReview: boolean;
  onUpdateField: <K extends keyof NotificationFormState>(field: K, value: NotificationFormState[K]) => void;
  onUpdateDisciplineRequestField: <K extends keyof DisciplineRequestFormState>(
    field: K,
    value: DisciplineRequestFormState[K],
  ) => void;
  onSend: () => void;
  onSubmitDisciplineRequest: () => void;
  onMarkRead: (notificationId: number) => void;
};

const staffRoleOptions = ["MEMBER", "LIBRARIAN", "BRANCH_MANAGER", "ADMIN", "AUDITOR"] as const;

export function NotificationsPanel({
  notifications,
  notificationForm,
  disciplineRequestForm,
  branches,
  canSendNotifications,
  canRequestDisciplineReview,
  onUpdateField,
  onUpdateDisciplineRequestField,
  onSend,
  onSubmitDisciplineRequest,
  onMarkRead,
}: NotificationsPanelProps) {
  function toggleRole(role: string) {
    const nextRoles = notificationForm.targetRoles.includes(role)
      ? notificationForm.targetRoles.filter((value) => value !== role)
      : [...notificationForm.targetRoles, role];
    onUpdateField("targetRoles", nextRoles);
  }

  const disciplineRequestActionOptions: UserDisciplineActionType[] = ["SUSPEND", "BAN", "REINSTATE"];
  const disciplineRequestReasonOptions: UserDisciplineReason[] = [
    "OVERDUE_ABUSE",
    "UNPAID_FEES",
    "LOST_ITEMS",
    "DAMAGED_ITEMS",
    "IDENTITY_MISUSE",
    "CONDUCT_VIOLATION",
    "SECURITY_REVIEW",
    "SPAM_OR_SYSTEM_ABUSE",
    "POLICY_VIOLATION",
    "APPEAL_APPROVED",
    "ISSUE_RESOLVED",
    "MANUAL_ADMIN_ACTION",
  ];

  return (
    <div className="admin-panel admin-panel-wide">
      <div className="section-heading">
        <div>
          <p className="section-label">Notifications</p>
          <h3>{canRequestDisciplineReview ? "Manager escalation and notices" : "Broadcast library notices to selected roles"}</h3>
        </div>
      </div>

      {canSendNotifications ? (
        <>
          <div className="command-grid">
            <label className="field">
              <span>Title</span>
              <input value={notificationForm.title} onChange={(event) => onUpdateField("title", event.target.value)} maxLength={120} />
            </label>
            <label className="field">
              <span>Branch scope</span>
              <select value={notificationForm.branchId} onChange={(event) => onUpdateField("branchId", event.target.value)}>
                <option value="">Global notification</option>
                {branches.map((branch) => (
                  <option key={branch.id} value={branch.id}>
                    {branch.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="field field-wide">
              <span>Message</span>
              <textarea value={notificationForm.message} onChange={(event) => onUpdateField("message", event.target.value)} rows={3} maxLength={600} />
            </label>
          </div>

          <div className="role-picker">
            {staffRoleOptions.map((role) => (
              <button
                key={role}
                type="button"
                className={notificationForm.targetRoles.includes(role) ? "button-secondary is-active" : "button-secondary"}
                onClick={() => toggleRole(role)}
              >
                {humanizeToken(role)}
              </button>
            ))}
          </div>

          <div className="form-actions">
            <button
              onClick={onSend}
              disabled={!notificationForm.title.trim() || !notificationForm.message.trim() || notificationForm.targetRoles.length === 0}
            >
              Send notification
            </button>
          </div>
        </>
      ) : null}

      {canRequestDisciplineReview ? (
        <div className="admin-panel">
          <h4>Request manager action on a member account</h4>
          <p className="hero-text">
            Librarians cannot suspend, ban, or edit users directly. Submit a request and the branch manager or an administrator will review it.
          </p>
          <div className="command-grid">
            <label className="field">
              <span>Member username</span>
              <input
                value={disciplineRequestForm.targetUsername}
                onChange={(event) => onUpdateDisciplineRequestField("targetUsername", event.target.value)}
                maxLength={100}
                placeholder="central.member"
              />
            </label>
            <label className="field">
              <span>Requested action</span>
              <select
                value={disciplineRequestForm.action}
                onChange={(event) =>
                  onUpdateDisciplineRequestField("action", event.target.value as UserDisciplineActionType)
                }
              >
                {disciplineRequestActionOptions.map((action) => (
                  <option key={action} value={action}>
                    {humanizeToken(action)}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Reason</span>
              <select
                value={disciplineRequestForm.reason}
                onChange={(event) =>
                  onUpdateDisciplineRequestField("reason", event.target.value as UserDisciplineReason)
                }
              >
                {disciplineRequestReasonOptions.map((reason) => (
                  <option key={reason} value={reason}>
                    {humanizeToken(reason)}
                  </option>
                ))}
              </select>
            </label>
            <label className="field field-wide">
              <span>Note</span>
              <textarea
                value={disciplineRequestForm.note}
                onChange={(event) => onUpdateDisciplineRequestField("note", event.target.value)}
                rows={3}
                maxLength={500}
                placeholder="Why this account needs manager review"
              />
            </label>
          </div>
          <div className="form-actions">
            <button
              onClick={onSubmitDisciplineRequest}
              disabled={!disciplineRequestForm.targetUsername.trim() || !disciplineRequestForm.note.trim()}
            >
              Send manager request
            </button>
          </div>
        </div>
      ) : null}

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
                {notification.branch?.name ?? "Global"} | {notification.targetRoles.map((role) => humanizeToken(role)).join(", ")}
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
    </div>
  );
}
