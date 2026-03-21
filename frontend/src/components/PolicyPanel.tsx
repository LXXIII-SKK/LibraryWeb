import { formatDateTime } from "../lib/format";
import type { PolicyFormState } from "../view-models";
import type { LibraryPolicy } from "../types";

type PolicyPanelProps = {
  canManagePolicies: boolean;
  policy: LibraryPolicy | null;
  policyForm: PolicyFormState | null;
  onUpdateField: <K extends keyof PolicyFormState>(field: K, value: PolicyFormState[K]) => void;
  onSave: () => void;
};

export function PolicyPanel({ canManagePolicies, policy, policyForm, onUpdateField, onSave }: PolicyPanelProps) {
  return (
    <div className="admin-panel admin-panel-wide">
      <div className="section-heading">
        <div>
          <p className="section-label">Policy</p>
          <h3>Circulation and waiver configuration</h3>
        </div>
        {policy ? <div className="status-chip">Updated {formatDateTime(policy.updatedAt)}</div> : null}
      </div>

      {policyForm ? (
        <div className="command-grid">
          <label className="field">
            <span>Standard loan days</span>
            <input
              type="number"
              min={1}
              value={policyForm.standardLoanDays}
              onChange={(event) => onUpdateField("standardLoanDays", Number(event.target.value))}
              disabled={!canManagePolicies}
            />
          </label>
          <label className="field">
            <span>Renewal days</span>
            <input
              type="number"
              min={1}
              value={policyForm.renewalDays}
              onChange={(event) => onUpdateField("renewalDays", Number(event.target.value))}
              disabled={!canManagePolicies}
            />
          </label>
          <label className="field">
            <span>Max renewals</span>
            <input
              type="number"
              min={0}
              value={policyForm.maxRenewals}
              onChange={(event) => onUpdateField("maxRenewals", Number(event.target.value))}
              disabled={!canManagePolicies}
            />
          </label>
          <label className="field">
            <span>Fine per overdue day</span>
            <input
              type="number"
              min={0}
              step="0.01"
              value={policyForm.finePerOverdueDay}
              onChange={(event) => onUpdateField("finePerOverdueDay", event.target.value)}
              disabled={!canManagePolicies}
            />
          </label>
          <label className="field">
            <span>Fine waiver limit</span>
            <input
              type="number"
              min={0}
              step="0.01"
              value={policyForm.fineWaiverLimit}
              onChange={(event) => onUpdateField("fineWaiverLimit", event.target.value)}
              disabled={!canManagePolicies}
            />
          </label>
          <label className="field field-checkbox">
            <span>Allow renewal with active reservations</span>
            <input
              type="checkbox"
              checked={policyForm.allowRenewalWithActiveReservations}
              onChange={(event) => onUpdateField("allowRenewalWithActiveReservations", event.target.checked)}
              disabled={!canManagePolicies}
            />
          </label>
        </div>
      ) : (
        <p className="empty-state">Policy configuration is not loaded.</p>
      )}

      {canManagePolicies ? (
        <div className="form-actions">
          <button onClick={onSave}>Save policy</button>
        </div>
      ) : null}
    </div>
  );
}
