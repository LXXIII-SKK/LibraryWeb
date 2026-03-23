import { formatDateTime, pluralize } from "../lib/format";
import type { BookDetailPageProps } from "../view-models";
import { BookCoverArt } from "./BookCoverArt";
import { BookTagChips } from "./BookTagChips";

export function BookDetailPage({
  book,
  canBorrow,
  canReserve,
  canManageCatalog,
  pickupBranches,
  pickupBranchId,
  relatedBorrowings,
  relatedLogs,
  onBack,
  onBorrow,
  onReserve,
  onPickupBranchChange,
  onStartEdit,
}: BookDetailPageProps) {
  const borrowedCopies = book.totalQuantity - book.availableQuantity;
  const stockTone =
    book.availableQuantity === 0
      ? "critical"
      : book.availableQuantity <= Math.max(1, Math.floor(book.totalQuantity / 3))
        ? "warning"
        : "healthy";

  return (
    <section className="detail-layout">
      <div className="surface detail-hero">
        <button className="button-secondary back-button" onClick={onBack}>
          Back to catalog
        </button>
        <div className="detail-header">
          <BookCoverArt title={book.title} coverImageUrl={book.coverImageUrl} className="detail-cover" />
          <div className="detail-copy">
            <p className="section-label">Book Detail</p>
            <h1>{book.title}</h1>
            <p className="detail-author">{book.author}</p>
            <p className="detail-summary">
              This detail page gives a dedicated operational view of a single title, including
              stock posture, catalog metadata, current circulation, and recent activity.
            </p>
            <BookTagChips tags={book.tags} className="tag-strip detail-tags" />
          </div>
          <div className="detail-actions">
            <span className={`stock-badge stock-${stockTone}`}>
              {book.availableQuantity > 0 ? "Available now" : "Currently unavailable"}
            </span>
            {canBorrow ? (
              <p className="hero-text">
                Member borrowing due dates are assigned automatically from the active circulation policy.
              </p>
            ) : null}
            {canReserve ? (
              <label className="field">
                <span>Pickup branch</span>
                <select
                  value={pickupBranchId ?? ""}
                  onChange={(event) => onPickupBranchChange(Number(event.target.value))}
                >
                  {pickupBranches.map((branch) => (
                    <option key={branch.id} value={branch.id}>
                      {branch.name}
                    </option>
                  ))}
                </select>
              </label>
            ) : null}
            <div className="catalog-actions">
              <button
                className="button-secondary"
                onClick={() => onReserve(book.id, pickupBranchId)}
                disabled={!canReserve || pickupBranchId === null}
              >
                Reserve for pickup
              </button>
              {canManageCatalog ? (
                <button className="button-secondary" onClick={() => onStartEdit(book)}>
                  Edit in operations workspace
                </button>
              ) : null}
            </div>
          </div>
        </div>

        <div className="detail-metrics">
          <div className="metric-card">
            <span>Category</span>
            <strong>{book.category ?? "General"}</strong>
          </div>
          <div className="metric-card">
            <span>ISBN</span>
            <strong>{book.isbn ?? "Not assigned"}</strong>
          </div>
          <div className="metric-card">
            <span>Total inventory</span>
            <strong>{pluralize(book.totalQuantity, "copy", "copies")}</strong>
          </div>
          <div className="metric-card">
            <span>Active loans</span>
            <strong>{pluralize(borrowedCopies, "copy", "copies")}</strong>
          </div>
          <div className="metric-card">
            <span>Views logged</span>
            <strong>{pluralize(book.viewCount, "view", "views")}</strong>
          </div>
        </div>
      </div>

      <div className="detail-grid">
        <div className="surface">
          <div className="section-heading">
            <div>
              <p className="section-label">Availability</p>
              <h2>Current fulfillment locations and online access</h2>
            </div>
          </div>
          <div className="stack-list">
            {book.availability.map((holding) => (
              <article key={holding.id} className="list-card">
                <div>
                  <strong>
                    {holding.format === "DIGITAL"
                      ? `${holding.branch?.name ?? "Digital collection"} online access`
                      : `${holding.branch?.name ?? "Branch"} | ${holding.location?.name ?? "Unassigned shelf"}`}
                  </strong>
                  <p>
                    {holding.availableQuantity}/{holding.totalQuantity}{" "}
                    {holding.format === "DIGITAL" ? "licenses" : "copies"} available
                  </p>
                  <small>{holding.format === "DIGITAL" ? "Borrow to unlock the online resource." : "Physical pickup location."}</small>
                </div>
                <button
                  onClick={() => onBorrow(book.id, holding.id)}
                  disabled={!canBorrow || holding.availableQuantity === 0 || !holding.active}
                >
                  Borrow {holding.format === "DIGITAL" ? "online" : "from here"}
                </button>
              </article>
            ))}
          </div>
          {book.availability.length === 0 ? (
            <p className="empty-state">This title has metadata but no configured holding yet.</p>
          ) : null}
        </div>

        <div className="surface">
          <div className="section-heading">
            <div>
              <p className="section-label">Circulation</p>
              <h2>Recent transactions for this title</h2>
            </div>
          </div>
          <div className="stack-list">
            {relatedBorrowings.map((borrowing) => (
              <article key={borrowing.id} className="list-card">
                <div>
                  <h3>{borrowing.username}</h3>
                  <p>
                    Borrowed {formatDateTime(borrowing.borrowedAt)}. Due {formatDateTime(borrowing.dueAt)}.
                  </p>
                </div>
                <span className={`pill pill-${borrowing.status.toLowerCase()}`}>{borrowing.status}</span>
              </article>
            ))}
          </div>
          {relatedBorrowings.length === 0 ? (
            <p className="empty-state">No circulation records yet for this title.</p>
          ) : null}
        </div>

        <div className="surface">
          <div className="section-heading">
            <div>
              <p className="section-label">Activity Trail</p>
              <h2>Recent events for this title</h2>
            </div>
          </div>
          <div className="timeline">
            {relatedLogs.map((log) => (
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
          {relatedLogs.length === 0 ? <p className="empty-state">No activity has been recorded for this title.</p> : null}
        </div>
      </div>
    </section>
  );
}
