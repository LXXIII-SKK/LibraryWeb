import { useState } from "react";

import { formatDateTime } from "../lib/format";
import type { Fine } from "../types";

type FinesPanelProps = {
  title: string;
  label: string;
  fines: Fine[];
  canWaive: boolean;
  onWaive?: (fineId: number, note: string) => void;
};

export function FinesPanel({ title, label, fines, canWaive, onWaive }: FinesPanelProps) {
  const [waiverTargetId, setWaiverTargetId] = useState<number | null>(null);
  const [waiverNote, setWaiverNote] = useState("");

  function openWaiverEditor(fineId: number) {
    setWaiverTargetId(fineId);
    setWaiverNote("");
  }

  function closeWaiverEditor() {
    setWaiverTargetId(null);
    setWaiverNote("");
  }

  function submitWaiver() {
    if (waiverTargetId === null || !onWaive || !waiverNote.trim()) {
      return;
    }
    onWaive(waiverTargetId, waiverNote.trim());
    closeWaiverEditor();
  }

  return (
    <div className="admin-panel admin-panel-wide">
      <div className="section-heading">
        <div>
          <p className="section-label">{label}</p>
          <h3>{title}</h3>
        </div>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>User</th>
              <th>Book</th>
              <th>Amount</th>
              <th>Status</th>
              <th>Created</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {fines.map((fine) => (
              <tr key={fine.id}>
                <td>{fine.username}</td>
                <td>{fine.bookTitle ?? "Manual record"}</td>
                <td>${fine.amount.toFixed(2)}</td>
                <td>
                  <span className={`pill pill-${fine.status.toLowerCase()}`}>{fine.status}</span>
                </td>
                <td>{formatDateTime(fine.createdAt)}</td>
                <td className="table-actions">
                  {canWaive && onWaive ? (
                    <button
                      className="button-secondary"
                      onClick={() => openWaiverEditor(fine.id)}
                      disabled={fine.status !== "OPEN"}
                    >
                      {fine.status === "OPEN" ? "Waive" : "Closed"}
                    </button>
                  ) : null}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {canWaive && onWaive && waiverTargetId !== null ? (
        <div className="command-grid">
          <label className="field">
            <span>Waiver note</span>
            <textarea
              value={waiverNote}
              onChange={(event) => setWaiverNote(event.target.value)}
              rows={3}
              maxLength={255}
              placeholder="Document the waiver reason for the audit trail."
            />
          </label>
          <div className="form-actions">
            <button onClick={submitWaiver} disabled={!waiverNote.trim()}>
              Confirm waiver
            </button>
            <button className="button-secondary" onClick={closeWaiverEditor}>
              Cancel
            </button>
          </div>
        </div>
      ) : null}

      {fines.length === 0 ? <p className="empty-state">No fine records are available in this scope.</p> : null}
    </div>
  );
}
