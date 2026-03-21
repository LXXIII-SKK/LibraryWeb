import type { FormEvent } from "react";

import type { Book, BookHolding, LibraryBranch, LibraryLocation } from "../types";
import type { HoldingFormState } from "../view-models";

type InventoryPanelProps = {
  editingHoldingId: number | null;
  holdingForm: HoldingFormState;
  books: Book[];
  branches: LibraryBranch[];
  locations: LibraryLocation[];
  holdings: BookHolding[];
  onUpdateField: <K extends keyof HoldingFormState>(field: K, value: HoldingFormState[K]) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onReset: () => void;
  onStartEdit: (holding: BookHolding) => void;
};

export function InventoryPanel({
  editingHoldingId,
  holdingForm,
  books,
  branches,
  locations,
  holdings,
  onUpdateField,
  onSubmit,
  onReset,
  onStartEdit,
}: InventoryPanelProps) {
  const branchLocations = locations.filter(
    (location) => !holdingForm.branchId || location.branch.id === Number(holdingForm.branchId),
  );

  return (
    <div className="admin-panel admin-panel-wide">
      <div className="section-heading">
        <div>
          <p className="section-label">Inventory</p>
          <h3>Branch and online availability per title</h3>
        </div>
      </div>

      <form className="book-form" onSubmit={onSubmit}>
        <div className="command-grid">
          <label className="field">
            <span>Title</span>
            <select value={holdingForm.bookId} onChange={(event) => onUpdateField("bookId", event.target.value)} required>
              <option value="">Select title</option>
              {books.map((book) => (
                <option key={book.id} value={book.id}>
                  {book.title}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Branch</span>
            <select value={holdingForm.branchId} onChange={(event) => onUpdateField("branchId", event.target.value)} required>
              <option value="">Select branch</option>
              {branches.map((branch) => (
                <option key={branch.id} value={branch.id}>
                  {branch.name}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Format</span>
            <select value={holdingForm.format} onChange={(event) => onUpdateField("format", event.target.value as HoldingFormState["format"])}>
              <option value="PHYSICAL">Physical</option>
              <option value="DIGITAL">Digital</option>
            </select>
          </label>
          <label className="field">
            <span>Location</span>
            <select
              value={holdingForm.locationId}
              onChange={(event) => onUpdateField("locationId", event.target.value)}
              disabled={holdingForm.format === "DIGITAL"}
            >
              <option value="">{holdingForm.format === "DIGITAL" ? "Not required for digital" : "Select location"}</option>
              {branchLocations.map((location) => (
                <option key={location.id} value={location.id}>
                  {location.name} ({location.code})
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Total quantity</span>
            <input
              type="number"
              min={0}
              value={holdingForm.totalQuantity}
              onChange={(event) => onUpdateField("totalQuantity", Number(event.target.value))}
            />
          </label>
          <label className="field">
            <span>Available quantity</span>
            <input
              type="number"
              min={0}
              value={holdingForm.availableQuantity}
              onChange={(event) => onUpdateField("availableQuantity", Number(event.target.value))}
            />
          </label>
          <label className="field">
            <span>Digital access URL</span>
            <input
              value={holdingForm.accessUrl}
              onChange={(event) => onUpdateField("accessUrl", event.target.value)}
              disabled={holdingForm.format !== "DIGITAL"}
              placeholder="https://..."
            />
          </label>
          <label className="field field-checkbox">
            <span>Active</span>
            <input type="checkbox" checked={holdingForm.active} onChange={(event) => onUpdateField("active", event.target.checked)} />
          </label>
        </div>

        <div className="form-actions">
          <button type="submit">{editingHoldingId === null ? "Create holding" : "Save holding"}</button>
          {editingHoldingId !== null ? (
            <button type="button" className="button-secondary" onClick={onReset}>
              Cancel edit
            </button>
          ) : null}
        </div>
      </form>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Title</th>
              <th>Format</th>
              <th>Branch</th>
              <th>Location</th>
              <th>Available</th>
              <th>Status</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {holdings.map((holding) => (
              <tr key={holding.id}>
                <td>{holding.bookTitle}</td>
                <td>{holding.format}</td>
                <td>{holding.branch?.name ?? "Global"}</td>
                <td>{holding.location?.name ?? (holding.format === "DIGITAL" ? "Online access" : "Unassigned")}</td>
                <td>
                  {holding.availableQuantity}/{holding.totalQuantity}
                </td>
                <td>{holding.active ? "Active" : "Inactive"}</td>
                <td className="table-actions">
                  <button className="button-secondary" onClick={() => onStartEdit(holding)}>
                    Edit
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {holdings.length === 0 ? <p className="empty-state">No inventory holdings are configured in this scope.</p> : null}
    </div>
  );
}
