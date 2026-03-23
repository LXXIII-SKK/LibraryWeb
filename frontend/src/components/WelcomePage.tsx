import { useEffect, useState } from "react";

import { formatDateTime, pluralize } from "../lib/format";
import type { DiscoveryBook, UpcomingBook } from "../types";
import type { WelcomePageProps } from "../view-models";
import { BookCoverArt } from "./BookCoverArt";
import { BookTagChips } from "./BookTagChips";
import { PagedGridSection } from "./PagedGridSection";

function RecommendationShowcase({
  books,
  onOpenBook,
}: {
  books: DiscoveryBook[];
  onOpenBook: (bookId: number) => void;
}) {
  const [activeIndex, setActiveIndex] = useState(0);

  useEffect(() => {
    if (books.length === 0) {
      setActiveIndex(0);
      return;
    }
    if (activeIndex >= books.length) {
      setActiveIndex(0);
    }
  }, [activeIndex, books.length]);

  useEffect(() => {
    if (books.length <= 1) {
      return;
    }

    const timer = window.setInterval(() => {
      setActiveIndex((current) => (current + 1) % books.length);
    }, 5000);

    return () => window.clearInterval(timer);
  }, [books.length]);

  if (books.length === 0) {
    return (
      <aside className="hero-showcase">
        <div className="showcase-card showcase-card-placeholder">
          <span className="showcase-badge">* 00</span>
          <div className="showcase-copy">
            <p className="showcase-title-static">No live recommendations yet.</p>
          </div>
        </div>
      </aside>
    );
  }

  const activeBook = books[activeIndex];
  const showcaseStyle = activeBook.coverImageUrl
    ? {
        backgroundImage: `linear-gradient(180deg, rgba(9, 16, 24, 0.04) 0%, rgba(9, 16, 24, 0.54) 56%, rgba(9, 16, 24, 0.88) 100%), url(${activeBook.coverImageUrl})`,
      }
    : undefined;

  function goToSlide(index: number) {
    setActiveIndex(index);
  }

  function goToPrevious() {
    setActiveIndex((current) => (current - 1 + books.length) % books.length);
  }

  function goToNext() {
    setActiveIndex((current) => (current + 1) % books.length);
  }

  return (
    <aside className="hero-showcase">
      <article
        className={`showcase-card${activeBook.coverImageUrl ? "" : " showcase-card-placeholder"}`}
        key={activeBook.id}
        style={showcaseStyle}
      >
        <span className="showcase-badge">* {String(activeIndex + 1).padStart(2, "0")}</span>

        <button type="button" className="showcase-arrow showcase-arrow-left" onClick={goToPrevious} aria-label="Previous recommendation">
          {"<"}
        </button>
        <button type="button" className="showcase-arrow showcase-arrow-right" onClick={goToNext} aria-label="Next recommendation">
          {">"}
        </button>

        <div className="showcase-copy">
          <div className="showcase-symbols">
            <span className="showcase-symbol">
              <strong>@</strong>
              <span title={activeBook.author}>{activeBook.author}</span>
            </span>
            <span className="showcase-symbol">
              <strong>#</strong>
              <span>{activeBook.availableQuantity}</span>
            </span>
          </div>

          <button type="button" className="link-button title-link showcase-title" onClick={() => onOpenBook(activeBook.id)}>
            {activeBook.title}
          </button>
        </div>

        {books.length > 1 ? (
          <div className="showcase-indicators" aria-label="Recommendation slides">
            {books.map((book, index) => (
              <button
                key={book.id}
                type="button"
                className={`showcase-indicator${index === activeIndex ? " showcase-indicator-active" : ""}`}
                onClick={() => goToSlide(index)}
                aria-label={`Open recommendation ${index + 1}`}
                aria-pressed={index === activeIndex}
              />
            ))}
          </div>
        ) : null}
      </article>
    </aside>
  );
}

function DiscoveryPageSection({
  label,
  title,
  description,
  books,
  onOpenBook,
}: {
  label: string;
  title: string;
  description: string;
  books: DiscoveryBook[];
  onOpenBook: (bookId: number) => void;
}) {
  return (
    <PagedGridSection
      label={label}
      title={title}
      description={description}
      items={books}
      emptyMessage="No movement yet for this week."
      getKey={(book) => book.id}
      renderItem={(book) => (
        <article className="catalog-card spotlight-card">
          <div className="spotlight-head">
            <BookCoverArt title={book.title} coverImageUrl={book.coverImageUrl} className="spotlight-cover" />
            <div className="catalog-card-copy">
              <button type="button" className="link-button title-link" onClick={() => onOpenBook(book.id)}>
                {book.title}
              </button>
              <p>{book.author}</p>
            </div>
          </div>
          <p className="spotlight-note">{book.spotlight}</p>
          <div className="catalog-actions">
            <span className="status-chip">{pluralize(book.availableQuantity, "copy", "copies")} available</span>
            <button type="button" className="button-secondary" onClick={() => onOpenBook(book.id)}>
              View book
            </button>
          </div>
        </article>
      )}
    />
  );
}

function UpcomingPageSection({
  books,
  onNavigateUpcoming,
}: {
  books: UpcomingBook[];
  onNavigateUpcoming: () => void;
}) {
  return (
    <PagedGridSection
      label="Upcoming"
      title="Books arriving soon"
      description="Upcoming arrivals now sit at the end of the home page and use the same 4-card paging rhythm as the rest of discovery."
      items={books}
      emptyMessage="No upcoming arrivals are being tracked yet."
      getKey={(book) => book.id}
      headerAction={
        <button type="button" className="button-secondary" onClick={onNavigateUpcoming}>
          Open upcoming page
        </button>
      }
      renderItem={(book) => (
        <article className="catalog-card upcoming-card">
          <div className="catalog-card-head">
            <div className="catalog-card-copy">
              <h3>{book.title}</h3>
              <p>{book.author}</p>
            </div>
            <span className="status-chip">{formatDateTime(book.expectedAt)}</span>
          </div>
          <p>{book.summary ?? "Arrival planning is in progress."}</p>
          <BookTagChips tags={book.tags} className="tag-strip compact-tags" />
          <dl className="catalog-meta">
            <div>
              <dt>Branch</dt>
              <dd>{book.branch?.name ?? "Shared acquisition"}</dd>
            </div>
            <div>
              <dt>Category</dt>
              <dd>{book.category ?? "General"}</dd>
            </div>
            <div>
              <dt>ISBN</dt>
              <dd>{book.isbn ?? "Not assigned"}</dd>
            </div>
          </dl>
        </article>
      )}
    />
  );
}

export function WelcomePage({
  inventoryStats,
  myBorrowingStats,
  recommendations,
  mostBorrowed,
  mostViewed,
  upcomingBooks,
  onOpenBook,
  onNavigateUpcoming,
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

        <RecommendationShowcase books={recommendations} onOpenBook={onOpenBook} />
      </section>

      <DiscoveryPageSection
        label="Most Borrowed"
        title="Most borrowed this week"
        description="Borrowing movement is now shown in paged 4-card rows instead of a single vertical stack."
        books={mostBorrowed}
        onOpenBook={onOpenBook}
      />

      <DiscoveryPageSection
        label="Most Viewed"
        title="Most viewed this week"
        description="View activity uses the same paged layout and only counts one signed-in view per reader and book until the next reset."
        books={mostViewed}
        onOpenBook={onOpenBook}
      />

      <UpcomingPageSection books={upcomingBooks} onNavigateUpcoming={onNavigateUpcoming} />
    </section>
  );
}
