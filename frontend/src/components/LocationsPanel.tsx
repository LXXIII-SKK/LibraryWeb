import type { FormEvent } from "react";

import type { LibraryBranch, LibraryLocation } from "../types";
import type { LocationFormState } from "../view-models";

type LocationsPanelProps = {
  editingLocationId: number | null;
  locationForm: LocationFormState;
  branches: LibraryBranch[];
  locations: LibraryLocation[];
  onUpdateField: <K extends keyof LocationFormState>(field: K, value: LocationFormState[K]) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onReset: () => void;
  onStartEdit: (location: LibraryLocation) => void;
};

export function LocationsPanel({
  editingLocationId,
  locationForm,
  branches,
  locations,
  onUpdateField,
  onSubmit,
  onReset,
  onStartEdit,
}: LocationsPanelProps) {
  return (
    <div className="admin-panel admin-panel-wide">
      <div className="section-heading">
        <div>
          <p className="section-label">Locations</p>
          <h3>Shelf and storage locations by branch</h3>
        </div>
      </div>

      <form className="book-form" onSubmit={onSubmit}>
        <div className="command-grid">
          <label className="field">
            <span>Branch</span>
            <select
              value={locationForm.branchId}
              onChange={(event) => onUpdateField("branchId", event.target.value)}
              required
            >
              <option value="">Select branch</option>
              {branches.map((branch) => (
                <option key={branch.id} value={branch.id}>
                  {branch.name} ({branch.code})
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Code</span>
            <input value={locationForm.code} onChange={(event) => onUpdateField("code", event.target.value)} required maxLength={30} />
          </label>
          <label className="field">
            <span>Name</span>
            <input value={locationForm.name} onChange={(event) => onUpdateField("name", event.target.value)} required maxLength={120} />
          </label>
          <label className="field">
            <span>Floor</span>
            <input value={locationForm.floorLabel} onChange={(event) => onUpdateField("floorLabel", event.target.value)} maxLength={50} />
          </label>
          <label className="field">
            <span>Zone</span>
            <input value={locationForm.zoneLabel} onChange={(event) => onUpdateField("zoneLabel", event.target.value)} maxLength={100} />
          </label>
          <label className="field field-checkbox">
            <span>Active</span>
            <input
              type="checkbox"
              checked={locationForm.active}
              onChange={(event) => onUpdateField("active", event.target.checked)}
            />
          </label>
        </div>

        <div className="form-actions">
          <button type="submit">{editingLocationId === null ? "Create location" : "Save location"}</button>
          {editingLocationId !== null ? (
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
              <th>Branch</th>
              <th>Code</th>
              <th>Name</th>
              <th>Floor</th>
              <th>Zone</th>
              <th>Status</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {locations.map((location) => (
              <tr key={location.id}>
                <td>{location.branch.name}</td>
                <td>{location.code}</td>
                <td>{location.name}</td>
                <td>{location.floorLabel ?? "Not set"}</td>
                <td>{location.zoneLabel ?? "Not set"}</td>
                <td>{location.active ? "Active" : "Inactive"}</td>
                <td className="table-actions">
                  <button className="button-secondary" onClick={() => onStartEdit(location)}>
                    Edit
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {locations.length === 0 ? <p className="empty-state">No branch locations are configured for this scope.</p> : null}
    </div>
  );
}
