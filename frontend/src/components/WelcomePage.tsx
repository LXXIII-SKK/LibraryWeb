import { pluralize } from "../lib/format";
import type { DiscoveryBook } from "../types";
import type { WelcomePageProps } from "../view-models";
import { BookCoverArt } from "./BookCoverArt";

function SpotlightColumn({
  label,
  title,
  books,
  onOpenBook,
}: {
  label: string;
  title: string;
  books: DiscoveryBook[];
  onOpenBook: (bookId: number) => void;
}) {
  return (
    <section className="surface">
      <div className="section-heading">
        <div>
          <p className="section-label">{label}</p>
          <h2>{title}</h2>
        </div>
      </div>
      <div className="spotlight-stack">
        {books.map((book) => (
          <article key={book.id} className="catalog-card spotlight-card">
            <div className="spotlight-head">
              <BookCoverArt title={book.title} coverImageUrl={book.coverImageUrl} className="spotlight-cover" />
              <div className="catalog-card-copy">
                <button className="link-button title-link" onClick={() => onOpenBook(book.id)}>
                  {book.title}
                </button>
                <p>{book.author}</p>
              </div>
            </div>
            <p className="spotlight-note">{book.spotlight}</p>
            <div className="catalog-actions">
              <span className="status-chip">{pluralize(book.availableQuantity, "copy", "copies")} available</span>
              <button className="button-secondary" onClick={() => onOpenBook(book.id)}>
                View book
              </button>
            </div>
          </article>
        ))}
      </div>
      {books.length === 0 ? <p className="empty-state">No movement yet for this week.</p> : null}
    </section>
  );
}

export function WelcomePage({
  signedIn,
  canAccessOperations,
  username,
  roleLabel,
  inventoryStats,
  myBorrowingStats,
  recommendations,
  mostBorrowed,
  mostViewed,
  upcomingBooks,
  onOpenBook,
  onNavigateBooks,
  onNavigateAccount,
  onLogin,
  onRegister,
  onLogout,
}: WelcomePageProps) {
  return (
    <section className="page-stack">
      <section className="welcome-hero surface">
        <div className="hero-copy">
          <p className="eyebrow">Welcome Page</p>
          <h1>Discover what is moving through the library this week.</h1>
          <p className="hero-text">
            Start with live recommendations, see what readers are borrowing most, and jump directly
            into the catalog or your own borrowing workspace.
          </p>
          <div className="metric-strip">
            <div className="metric-card">
              <span>Total titles</span>
              <strong>{inventoryStats.totalTitles}</strong>
            </div>
            <div className="metric-card">
              <span>Available copies</span>
              <strong>{inventoryStats.availableCopies}</strong>
            </div>
            <div className="metric-card">
              <span>My active loans</span>
              <strong>{myBorrowingStats.active}</strong>
            </div>
            <div className="metric-card">
              <span>Out of stock</span>
              <strong>{inventoryStats.outOfStock}</strong>
            </div>
          </div>
        </div>

        <aside className="hero-aside">
          <div className="identity-card">
            <span className="identity-label">Current session</span>
            <strong>{signedIn ? username : "Guest session"}</strong>
            <p>
              {signedIn
                ? canAccessOperations
                  ? `${roleLabel} tools are available from the operations workspace.`
                  : "Borrow books, review due dates, and manage returns from your personal page."
                : "Sign in to borrow books, record views, and manage your lending activity."}
            </p>
            <div className="hero-actions">
              <button onClick={onNavigateBooks}>Browse books</button>
              <button className="button-secondary" onClick={onNavigateAccount}>
                My page
              </button>
            </div>
          </div>
          <div className="hero-actions">
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
        </aside>
      </section>

      <SpotlightColumn
        label="Recommendations"
        title="Recommended right now"
        books={recommendations}
        onOpenBook={onOpenBook}
      />

      <section className="surface">
        <div className="section-heading">
          <div>
            <p className="section-label">Upcoming</p>
            <h2>Books arriving soon</h2>
          </div>
        </div>
        <div className="stack-list">
          {upcomingBooks.map((book) => (
            <article key={book.id} className="list-card">
              <div>
                <strong>{book.title}</strong>
                <p>{book.author}</p>
                <p>{book.summary ?? "Arrival planning is in progress."}</p>
              </div>
              <span className="status-chip">{new Date(book.expectedAt).toLocaleDateString()}</span>
            </article>
          ))}
        </div>
        {upcomingBooks.length === 0 ? <p className="empty-state">No upcoming arrivals are being tracked yet.</p> : null}
      </section>

      <section className="spotlight-grid">
        <SpotlightColumn
          label="Most Borrowed"
          title="Most borrowed this week"
          books={mostBorrowed}
          onOpenBook={onOpenBook}
        />
        <SpotlightColumn
          label="Most Viewed"
          title="Most viewed this week"
          books={mostViewed}
          onOpenBook={onOpenBook}
        />
      </section>
    </section>
  );
}
