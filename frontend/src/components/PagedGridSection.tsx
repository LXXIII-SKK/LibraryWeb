import { useEffect, useState, type ReactNode } from "react";

type PagedGridSectionProps<T> = {
  label: string;
  title: string;
  description?: string;
  items: T[];
  emptyMessage: string;
  getKey: (item: T) => number | string;
  renderItem: (item: T) => ReactNode;
  itemsPerPage?: number;
  resetKey?: string;
  headerAction?: ReactNode;
  sectionClassName?: string;
  gridClassName?: string;
};

export function PagedGridSection<T>({
  label,
  title,
  description,
  items,
  emptyMessage,
  getKey,
  renderItem,
  itemsPerPage = 4,
  resetKey,
  headerAction,
  sectionClassName,
  gridClassName,
}: PagedGridSectionProps<T>) {
  const [page, setPage] = useState(0);
  const totalPages = Math.ceil(items.length / itemsPerPage);

  useEffect(() => {
    setPage(0);
  }, [resetKey]);

  useEffect(() => {
    if (totalPages === 0) {
      if (page !== 0) {
        setPage(0);
      }
      return;
    }

    if (page > totalPages - 1) {
      setPage(totalPages - 1);
    }
  }, [page, totalPages]);

  const visibleItems =
    totalPages === 0 ? [] : items.slice(page * itemsPerPage, page * itemsPerPage + itemsPerPage);
  const pageLabel = totalPages === 0 ? "Page 0 / 0" : `Page ${page + 1} / ${totalPages}`;

  return (
    <section className={`surface paged-section${sectionClassName ? ` ${sectionClassName}` : ""}`}>
      <div className="section-heading">
        <div>
          <p className="section-label">{label}</p>
          <h2>{title}</h2>
          {description ? <p className="books-hero-text paged-section-copy">{description}</p> : null}
        </div>
        <div className="section-tools">
          {headerAction}
          <div className="pager-controls" aria-label={`${label} pagination`}>
            <span className="page-badge">{pageLabel}</span>
            <button
              type="button"
              className="button-secondary pager-button"
              onClick={() => setPage((current) => Math.max(0, current - 1))}
              disabled={page === 0}
            >
              Prev
            </button>
            <button
              type="button"
              className="button-secondary pager-button"
              onClick={() => setPage((current) => Math.min(Math.max(totalPages - 1, 0), current + 1))}
              disabled={totalPages <= 1 || page >= totalPages - 1}
            >
              Next
            </button>
          </div>
        </div>
      </div>

      {visibleItems.length > 0 ? (
        <div className={`paged-grid${gridClassName ? ` ${gridClassName}` : ""}`}>
          {visibleItems.map((item) => (
            <div key={getKey(item)}>{renderItem(item)}</div>
          ))}
        </div>
      ) : (
        <p className="empty-state">{emptyMessage}</p>
      )}
    </section>
  );
}
