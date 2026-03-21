import type { FormEvent } from "react";

import type { BranchFormState } from "../view-models";
import type { LibraryBranch } from "../types";

type BranchesPanelProps = {
  editingBranchId: number | null;
  branchForm: BranchFormState;
  branches: LibraryBranch[];
  onUpdateField: <K extends keyof BranchFormState>(field: K, value: BranchFormState[K]) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onReset: () => void;
  onStartEdit: (branch: LibraryBranch) => void;
};

export function BranchesPanel({
  editingBranchId,
  branchForm,
  branches,
  onUpdateField,
  onSubmit,
  onReset,
  onStartEdit,
}: BranchesPanelProps) {
  return (
    <div className="admin-panel admin-panel-wide">
      <div className="section-heading">
        <div>
          <p className="section-label">Branch Directory</p>
          <h3>Library centers and branch metadata</h3>
        </div>
      </div>

      <form className="book-form" onSubmit={onSubmit}>
        <div className="command-grid">
          <label className="field">
            <span>Code</span>
            <input
              value={branchForm.code}
              onChange={(event) => onUpdateField("code", event.target.value)}
              required
              maxLength={30}
            />
          </label>
          <label className="field">
            <span>Name</span>
            <input
              value={branchForm.name}
              onChange={(event) => onUpdateField("name", event.target.value)}
              required
              maxLength={120}
            />
          </label>
          <label className="field">
            <span>Address</span>
            <input
              value={branchForm.address}
              onChange={(event) => onUpdateField("address", event.target.value)}
              maxLength={255}
            />
          </label>
          <label className="field">
            <span>Phone</span>
            <input
              value={branchForm.phone}
              onChange={(event) => onUpdateField("phone", event.target.value)}
              maxLength={50}
            />
          </label>
          <label className="field field-checkbox">
            <span>Active</span>
            <input
              type="checkbox"
              checked={branchForm.active}
              onChange={(event) => onUpdateField("active", event.target.checked)}
            />
          </label>
        </div>

        <div className="form-actions">
          <button type="submit">{editingBranchId === null ? "Create branch" : "Save branch"}</button>
          {editingBranchId !== null ? (
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
              <th>Code</th>
              <th>Name</th>
              <th>Address</th>
              <th>Phone</th>
              <th>Status</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {branches.map((branch) => (
              <tr key={branch.id}>
                <td>{branch.code}</td>
                <td>{branch.name}</td>
                <td>{branch.address ?? "No address"}</td>
                <td>{branch.phone ?? "No phone"}</td>
                <td>{branch.active ? "Active" : "Inactive"}</td>
                <td className="table-actions">
                  <button className="button-secondary" onClick={() => onStartEdit(branch)}>
                    Edit
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
