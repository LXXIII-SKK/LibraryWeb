import { pluralize } from "../lib/format";
import type { CatalogPanelProps } from "../view-models";
import { BookCoverArt } from "./BookCoverArt";
import { BookTagChips } from "./BookTagChips";

export function CatalogPanel({
  loading,
  canBorrow,
  canReserve,
  canManageCatalog,
  query,
  categoryFilter,
  tagFilter,
  categories,
  tags,
  books,
  onQueryChange,
  onCategoryChange,
  onTagChange,
  onBorrow,
  onReserve,
  onStartEdit,
  onOpenBook,
}: CatalogPanelProps) {
  function borrowableHoldingsForBook(book: CatalogPanelProps["books"][number]) {
    return book.availability.filter((holding) => holding.active && holding.availableQuantity > 0);
  }

  function holdingLabel(holding: CatalogPanelProps["books"][number]["availability"][number]) {
    if (holding.format === "DIGITAL") {
      return `${holding.branch?.name ?? "Digital collection"} online access`;
    }
    return [holding.branch?.name, holding.location?.name].filter(Boolean).join(" | ");
  }

  return (
    <section className="surface surface-command">
      <div className="section-heading">
        <div>
          <p className="section-label">Catalog Explorer</p>
          <h2>Search, filter, and execute circulation flows.</h2>
        </div>
        <div className="status-chip">{loading ? "Refreshing data" : "Live data ready"}</div>
      </div>

      <div className="command-grid">
        <label className="field">
          <span>Search catalog</span>
          <input
            value={query}
            onChange={(event) => onQueryChange(event.target.value)}
            placeholder="Title, author, or category"
            maxLength={120}
          />
        </label>

        <label className="field">
          <span>Category</span>
          <select value={categoryFilter} onChange={(event) => onCategoryChange(event.target.value)}>
            <option value="all">All categories</option>
            {categories.map((category) => (
              <option key={category} value={category}>
                {category}
              </option>
            ))}
          </select>
        </label>

        <label className="field">
          <span>Tag</span>
          <select value={tagFilter} onChange={(event) => onTagChange(event.target.value)}>
            <option value="all">All tags</option>
            {tags.map((tag) => (
              <option key={tag} value={tag}>
                {tag}
              </option>
            ))}
          </select>
        </label>
      </div>

      {canBorrow ? (
        <p className="hero-text books-hero-text">
          Member borrowing due dates are assigned automatically from the active circulation policy.
        </p>
      ) : null}

      <div className="catalog-grid">
        {books.map((book) => {
          const borrowedCopies = book.totalQuantity - book.availableQuantity;
          const borrowableHoldings = borrowableHoldingsForBook(book);
          const stockTone =
            book.availableQuantity === 0
              ? "critical"
              : book.availableQuantity <= Math.max(1, Math.floor(book.totalQuantity / 3))
                ? "warning"
                : "healthy";

          return (
            <article key={book.id} className="catalog-card">
              <BookCoverArt title={book.title} coverImageUrl={book.coverImageUrl} className="catalog-card-cover" />
              <div className="catalog-card-head">
                <div className="catalog-card-copy">
                  <button className="link-button title-link" onClick={() => onOpenBook(book.id)}>
                    {book.title}
                  </button>
                  <p>{book.author}</p>
                </div>
                <span className={`stock-badge stock-${stockTone}`}>
                  {book.availableQuantity > 0 ? "Available" : "Unavailable"}
                </span>
              </div>

              <BookTagChips tags={book.tags} />

              <div className="availability-preview">
                {book.availability.slice(0, 3).map((holding) => (
                  <div key={holding.id} className="availability-line">
                    <strong>{holdingLabel(holding)}</strong>
                    <span>
                      {holding.availableQuantity}/{holding.totalQuantity} {holding.format === "DIGITAL" ? "licenses" : "copies"}
                    </span>
                  </div>
                ))}
                {book.availability.length === 0 ? (
                  <p className="empty-inline">No holdings are configured for this title yet.</p>
                ) : null}
              </div>

              <dl className="catalog-meta">
                <div>
                  <dt>Category</dt>
                  <dd>{book.category ?? "General"}</dd>
                </div>
                <div>
                  <dt>ISBN</dt>
                  <dd>{book.isbn ?? "Not assigned"}</dd>
                </div>
                <div>
                  <dt>Inventory</dt>
                  <dd>{pluralize(book.totalQuantity, "copy", "copies")}</dd>
                </div>
                <div>
                  <dt>On loan</dt>
                  <dd>{pluralize(borrowedCopies, "copy", "copies")}</dd>
                </div>
              </dl>

              <div className="catalog-actions">
                <button className="button-secondary" onClick={() => onOpenBook(book.id)}>
                  View details
                </button>
                <button
                  onClick={() =>
                    borrowableHoldings.length === 1 ? onBorrow(book.id, borrowableHoldings[0].id) : onOpenBook(book.id)
                  }
                  disabled={!canBorrow || book.availableQuantity === 0}
                >
                  {borrowableHoldings.length <= 1 ? "Borrow" : "Choose copy"}
                </button>
                <button className="button-secondary" onClick={() => onReserve(book.id)} disabled={!canReserve}>
                  Reserve
                </button>
                {canManageCatalog ? (
                  <button className="button-secondary" onClick={() => onStartEdit(book)}>
                    Edit
                  </button>
                ) : null}
              </div>
            </article>
          );
        })}
      </div>

      {books.length === 0 ? <p className="empty-state">No books match the current filters.</p> : null}
    </section>
  );
}
