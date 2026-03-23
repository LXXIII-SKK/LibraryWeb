import { CatalogPanel } from "./CatalogPanel";
import type { BooksWorkspacePageProps } from "../view-models";

export function BooksWorkspacePage(props: BooksWorkspacePageProps) {
  return (
    <section className="page-stack">
      <section className="surface books-hero">
        <div className="section-heading">
          <div>
            <p className="section-label">Books Workspace</p>
            <h2>Search, filter, inspect, and borrow from the library catalog.</h2>
          </div>
          <div className="hero-actions books-hero-actions">
            <button type="button" className="button-secondary" onClick={props.onNavigateUpcoming}>
              Open upcoming page
            </button>
          </div>
        </div>
        <p className="hero-text books-hero-text">
          This page is focused on live catalog discovery and circulation. Browse titles, inspect
          covers, open detail pages, and execute borrowing workflows without leaving the catalog.
        </p>
      </section>

      <CatalogPanel {...props} />
    </section>
  );
}
