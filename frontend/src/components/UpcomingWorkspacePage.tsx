import { useState } from "react";

import { formatDateTime } from "../lib/format";
import type { UpcomingWorkspacePageProps } from "../view-models";
import { BookTagChips } from "./BookTagChips";
import { PagedGridSection } from "./PagedGridSection";

export function UpcomingWorkspacePage({ upcomingBooks, onNavigateBooks }: UpcomingWorkspacePageProps) {
  const [query, setQuery] = useState("");
  const [branchFilter, setBranchFilter] = useState("all");

  const branchOptions = upcomingBooks.reduce<{ id: string; label: string }[]>((branches, book) => {
    if (!book.branch) {
      return branches;
    }

    if (branches.some((entry) => entry.id === book.branch?.id.toString())) {
      return branches;
    }

    branches.push({ id: book.branch.id.toString(), label: book.branch.name });
    return branches;
  }, []);

  const trimmedQuery = query.trim().toLowerCase();
  const filteredUpcomingBooks = upcomingBooks.filter((book) => {
    const matchesBranch =
      branchFilter === "all" ||
      (branchFilter === "shared" && book.branch === null) ||
      book.branch?.id.toString() === branchFilter;

    if (!matchesBranch) {
      return false;
    }

    if (!trimmedQuery) {
      return true;
    }

    const haystack = [
      book.title,
      book.author,
      book.category ?? "",
      book.summary ?? "",
      book.branch?.name ?? "shared acquisition",
      book.tags.join(" "),
    ]
      .join(" ")
      .toLowerCase();

    return haystack.includes(trimmedQuery);
  });

  return (
    <section className="page-stack">
      <section className="surface books-hero">
        <div className="section-heading">
          <div>
            <p className="section-label">Upcoming Workspace</p>
            <h2>Track planned arrivals and shared acquisition work in a dedicated explorer.</h2>
          </div>
          <div className="hero-actions books-hero-actions">
            <button type="button" className="button-secondary" onClick={onNavigateBooks}>
              Browse current catalog
            </button>
          </div>
        </div>
        <p className="hero-text books-hero-text">
          Upcoming titles now live on their own page instead of competing with the active catalog.
          Filter the pipeline, review expected arrival windows, and scan branch-specific planning in
          4-card pages.
        </p>
      </section>

      <section className="surface surface-command">
        <div className="section-heading">
          <div>
            <p className="section-label">Upcoming Explorer</p>
            <h2>Filter planned arrivals before they enter the live catalog.</h2>
          </div>
          <span className="page-badge">
            {filteredUpcomingBooks.length} upcoming {filteredUpcomingBooks.length === 1 ? "title" : "titles"}
          </span>
        </div>

        <div className="command-grid">
          <label className="field">
            <span>Search upcoming titles</span>
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Title, author, summary, or tag"
              maxLength={120}
            />
          </label>

          <label className="field">
            <span>Branch</span>
            <select value={branchFilter} onChange={(event) => setBranchFilter(event.target.value)}>
              <option value="all">All branches</option>
              <option value="shared">Shared acquisition</option>
              {branchOptions.map((branch) => (
                <option key={branch.id} value={branch.id}>
                  {branch.label}
                </option>
              ))}
            </select>
          </label>
        </div>
      </section>

      <PagedGridSection
        label="Upcoming Titles"
        title="Arrival queue"
        description="Every page shows 4 planned titles so the acquisition view stays readable."
        items={filteredUpcomingBooks}
        emptyMessage="No upcoming titles match the current filters."
        getKey={(book) => book.id}
        resetKey={`${query}|${branchFilter}|${filteredUpcomingBooks.length}`}
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
    </section>
  );
}
