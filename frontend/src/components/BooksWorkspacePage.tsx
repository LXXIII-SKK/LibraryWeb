import { CatalogPanel } from "./CatalogPanel";
import type { BooksWorkspacePageProps } from "../view-models";
import { BookTagChips } from "./BookTagChips";
import { formatDateTime } from "../lib/format";

export function BooksWorkspacePage(props: BooksWorkspacePageProps) {
  return (
    <section className="page-stack">
      <section className="surface books-hero">
        <div className="section-heading">
          <div>
            <p className="section-label">Books Workspace</p>
            <h2>Search, filter, inspect, and borrow from the library catalog.</h2>
          </div>
        </div>
        <p className="hero-text books-hero-text">
          This page is focused on discovery and circulation. Browse titles, inspect covers, open
          detail pages, and execute borrowing workflows without leaving the catalog.
        </p>
      </section>

      <section className="surface">
        <div className="section-heading">
          <div>
            <p className="section-label">Upcoming Books</p>
            <h2>Arrivals already queued into acquisition planning</h2>
          </div>
        </div>
        <div className="stack-list">
          {props.upcomingBooks.map((book) => (
            <article key={book.id} className="list-card">
              <div>
                <strong>{book.title}</strong>
                <p>{book.author}</p>
                <p>{book.summary ?? "Arrival planning is in progress."}</p>
                <BookTagChips tags={book.tags} className="tag-strip compact-tags" />
              </div>
              <div className="availability-copy">
                <strong>{book.branch?.name ?? "Shared acquisition"}</strong>
                <small>Expected {formatDateTime(book.expectedAt)}</small>
              </div>
            </article>
          ))}
        </div>
        {props.upcomingBooks.length === 0 ? <p className="empty-state">No upcoming titles are listed right now.</p> : null}
      </section>

      <CatalogPanel {...props} />
    </section>
  );
}
