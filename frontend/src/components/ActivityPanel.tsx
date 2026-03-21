import { formatDateTime } from "../lib/format";
import type { ActivityPanelProps } from "../view-models";

export function ActivityPanel({ label, title, emptyMessage, logs }: ActivityPanelProps) {
  return (
    <div className="surface">
      <div className="section-heading">
        <div>
          <p className="section-label">{label}</p>
          <h2>{title}</h2>
        </div>
      </div>

      <div className="timeline">
        {logs.slice(0, 10).map((log) => (
          <article key={log.id} className="timeline-card">
            <div className="timeline-marker" />
            <div>
              <strong>{log.activityType}</strong>
              <p>{log.message}</p>
              <small>{formatDateTime(log.occurredAt)}</small>
            </div>
          </article>
        ))}
      </div>

      {logs.length === 0 ? <p className="empty-state">{emptyMessage}</p> : null}
    </div>
  );
}
