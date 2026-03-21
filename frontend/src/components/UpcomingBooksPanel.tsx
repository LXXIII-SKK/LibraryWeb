import type { FormEvent } from "react";

import { formatDateTime } from "../lib/format";
import { BookTagChips } from "./BookTagChips";
import type { LibraryBranch, UpcomingBook } from "../types";
import type { UpcomingBookFormState } from "../view-models";

type UpcomingBooksPanelProps = {
  editingUpcomingBookId: number | null;
  upcomingBookForm: UpcomingBookFormState;
  upcomingBooks: UpcomingBook[];
  branches: LibraryBranch[];
  onUpdateField: <K extends keyof UpcomingBookFormState>(field: K, value: UpcomingBookFormState[K]) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onReset: () => void;
  onStartEdit: (upcomingBook: UpcomingBook) => void;
  onDelete: (upcomingBook: UpcomingBook) => void;
};

export function UpcomingBooksPanel({
  editingUpcomingBookId,
  upcomingBookForm,
  upcomingBooks,
  branches,
  onUpdateField,
  onSubmit,
  onReset,
  onStartEdit,
  onDelete,
}: UpcomingBooksPanelProps) {
  return (
    <div className="admin-panel admin-panel-wide">
      <div className="section-heading">
        <div>
          <p className="section-label">Upcoming</p>
          <h3>Planned acquisitions and arrivals</h3>
        </div>
      </div>

      <form className="book-form" onSubmit={onSubmit}>
        <div className="command-grid">
          <label className="field">
            <span>Title</span>
            <input value={upcomingBookForm.title} onChange={(event) => onUpdateField("title", event.target.value)} required maxLength={255} />
          </label>
          <label className="field">
            <span>Author</span>
            <input value={upcomingBookForm.author} onChange={(event) => onUpdateField("author", event.target.value)} required maxLength={255} />
          </label>
          <label className="field">
            <span>Category</span>
            <input value={upcomingBookForm.category} onChange={(event) => onUpdateField("category", event.target.value)} maxLength={100} />
          </label>
          <label className="field">
            <span>ISBN</span>
            <input value={upcomingBookForm.isbn} onChange={(event) => onUpdateField("isbn", event.target.value)} maxLength={50} />
          </label>
          <label className="field">
            <span>Expected arrival</span>
            <input
              type="datetime-local"
              value={upcomingBookForm.expectedAt}
              onChange={(event) => onUpdateField("expectedAt", event.target.value)}
              required
            />
          </label>
          <label className="field">
            <span>Branch</span>
            <select value={upcomingBookForm.branchId} onChange={(event) => onUpdateField("branchId", event.target.value)}>
              <option value="">Global or shared acquisition</option>
              {branches.map((branch) => (
                <option key={branch.id} value={branch.id}>
                  {branch.name}
                </option>
              ))}
            </select>
          </label>
          <label className="field field-wide">
            <span>Summary</span>
            <textarea value={upcomingBookForm.summary} onChange={(event) => onUpdateField("summary", event.target.value)} rows={3} maxLength={600} />
          </label>
          <label className="field field-wide">
            <span>Tags</span>
            <input value={upcomingBookForm.tags} onChange={(event) => onUpdateField("tags", event.target.value)} placeholder="upcoming, architecture, data" />
          </label>
        </div>

        <div className="form-actions">
          <button type="submit">{editingUpcomingBookId === null ? "Create upcoming title" : "Save upcoming title"}</button>
          {editingUpcomingBookId !== null ? (
            <button type="button" className="button-secondary" onClick={onReset}>
              Cancel edit
            </button>
          ) : null}
        </div>
      </form>

      <div className="stack-list">
        {upcomingBooks.map((book) => (
          <article key={book.id} className="list-card">
            <div>
              <strong>{book.title}</strong>
              <p>{book.author}</p>
              <p>{book.summary ?? "No summary provided yet."}</p>
              <BookTagChips tags={book.tags} className="tag-strip compact-tags" />
              <small>
                {book.branch?.name ?? "Shared acquisition"} | arriving {formatDateTime(book.expectedAt)}
              </small>
            </div>
            <div className="inline-actions">
              <button className="button-secondary" onClick={() => onStartEdit(book)}>
                Edit
              </button>
              <button className="button-danger" onClick={() => onDelete(book)}>
                Remove
              </button>
            </div>
          </article>
        ))}
      </div>

      {upcomingBooks.length === 0 ? <p className="empty-state">No upcoming acquisitions are being tracked yet.</p> : null}
    </div>
  );
}
