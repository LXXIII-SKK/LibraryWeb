import { useEffect, useState } from "react";

import { formatDateTime, humanizeToken } from "../lib/format";
import type { AccessFormState } from "../view-models";
import type {
  AccessOptions,
  UserAccess,
  UserDisciplineActionType,
  UserDisciplineReason,
  UserDisciplineRecord,
} from "../types";

type AccessManagementPanelProps = {
  canManageUsers: boolean;
  users: UserAccess[];
  selectedUserId: number | null;
  selectedUser: UserAccess | null;
  disciplineHistory: UserDisciplineRecord[];
  accessOptions: AccessOptions | null;
  accessForm: AccessFormState | null;
  onSelectUser: (userId: number) => void;
  onUpdateField: <K extends keyof AccessFormState>(field: K, value: AccessFormState[K]) => void;
  onSave: () => void;
  onApplyUserDiscipline: (
    userId: number,
    action: UserDisciplineActionType,
    reason: UserDisciplineReason,
    note: string,
  ) => void;
};

export function AccessManagementPanel({
  canManageUsers,
  users,
  selectedUserId,
  selectedUser,
  disciplineHistory,
  accessOptions,
  accessForm,
  onSelectUser,
  onUpdateField,
  onSave,
  onApplyUserDiscipline,
}: AccessManagementPanelProps) {
  const [disciplineAction, setDisciplineAction] = useState<UserDisciplineActionType | "">("");
  const [disciplineReason, setDisciplineReason] = useState<UserDisciplineReason | "">("");
  const [disciplineNote, setDisciplineNote] = useState("");

  useEffect(() => {
    setDisciplineAction(accessOptions?.disciplineActions[0] ?? "");
    setDisciplineReason(accessOptions?.disciplineReasons[0] ?? "");
    setDisciplineNote("");
  }, [accessOptions?.disciplineActions, accessOptions?.disciplineReasons, selectedUserId]);

  return (
    <div className="admin-panel admin-panel-wide">
      <div className="section-heading">
        <div>
          <p className="section-label">Access Control</p>
          <h3>Roles, statuses, branch scope, and member discipline</h3>
        </div>
      </div>

      <div className="access-grid">
        <div className="stack-list access-list">
          {users.map((user) => (
            <button
              key={user.id}
              className={`access-card${selectedUserId === user.id ? " access-card-active" : ""}`}
              onClick={() => onSelectUser(user.id)}
            >
              <strong>{user.username}</strong>
              <span>{humanizeToken(user.role)}</span>
              <small>
                {humanizeToken(user.accountStatus)} / {humanizeToken(user.membershipStatus)}
              </small>
              <small>{user.branch?.name ?? user.homeBranch?.name ?? "Global scope"}</small>
            </button>
          ))}
        </div>

        <div className="access-detail">
          {selectedUser && accessOptions && accessForm ? (
            <>
              <div className="profile-card access-summary">
                <div>
                  <span className="profile-label">User</span>
                  <strong>{selectedUser.username}</strong>
                </div>
                <div>
                  <span className="profile-label">Email</span>
                  <strong>{selectedUser.email ?? "No email"}</strong>
                </div>
                <div>
                  <span className="profile-label">Scope</span>
                  <strong>{humanizeToken(selectedUser.scope)}</strong>
                </div>
                <div>
                  <span className="profile-label">Branch</span>
                  <strong>{selectedUser.branch?.name ?? "No branch"}</strong>
                </div>
                <div>
                  <span className="profile-label">Home branch</span>
                  <strong>{selectedUser.homeBranch?.name ?? "No home branch"}</strong>
                </div>
                <div>
                  <span className="profile-label">Permissions</span>
                  <strong>{selectedUser.permissions.length}</strong>
                </div>
              </div>

              <div className="command-grid">
                <label className="field">
                  <span>Role</span>
                  <select
                    value={accessForm.role}
                    onChange={(event) => onUpdateField("role", event.target.value)}
                    disabled={!canManageUsers}
                  >
                    {accessOptions.roles.map((role) => (
                      <option key={role} value={role}>
                        {humanizeToken(role)}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="field">
                  <span>Account status</span>
                  <select
                    value={accessForm.accountStatus}
                    onChange={(event) => onUpdateField("accountStatus", event.target.value)}
                    disabled={!canManageUsers}
                  >
                    {accessOptions.accountStatuses.map((status) => (
                      <option key={status} value={status}>
                        {humanizeToken(status)}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="field">
                  <span>Membership status</span>
                  <select
                    value={accessForm.membershipStatus}
                    onChange={(event) => onUpdateField("membershipStatus", event.target.value)}
                    disabled={!canManageUsers}
                  >
                    {accessOptions.membershipStatuses.map((status) => (
                      <option key={status} value={status}>
                        {humanizeToken(status)}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="field">
                  <span>Branch</span>
                  <select
                    value={accessForm.branchId}
                    onChange={(event) => onUpdateField("branchId", event.target.value)}
                    disabled={!canManageUsers}
                  >
                    <option value="">No branch assignment</option>
                    {accessOptions.branches.map((branch) => (
                      <option key={branch.id} value={branch.id}>
                        {branch.name} ({branch.code})
                      </option>
                    ))}
                  </select>
                </label>
                <label className="field">
                  <span>Home branch</span>
                  <select
                    value={accessForm.homeBranchId}
                    onChange={(event) => onUpdateField("homeBranchId", event.target.value)}
                    disabled={!canManageUsers}
                  >
                    <option value="">No home branch</option>
                    {accessOptions.branches.map((branch) => (
                      <option key={branch.id} value={branch.id}>
                        {branch.name} ({branch.code})
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              {canManageUsers ? (
                <div className="form-actions">
                  <button onClick={onSave}>Save access changes</button>
                </div>
              ) : null}

              <div className="admin-panel access-discipline-panel">
                <h4>User discipline</h4>
                <p className="hero-text">
                  Suspend, ban, or reinstate a member with a recorded reason and audit trail.
                </p>

                {canManageUsers && accessOptions.disciplineActions.length > 0 ? (
                  <>
                    <div className="command-grid">
                      <label className="field">
                        <span>Action</span>
                        <select
                          value={disciplineAction}
                          onChange={(event) => setDisciplineAction(event.target.value as UserDisciplineActionType)}
                        >
                          {accessOptions.disciplineActions.map((action) => (
                            <option key={action} value={action}>
                              {humanizeToken(action)}
                            </option>
                          ))}
                        </select>
                      </label>
                      <label className="field">
                        <span>Reason</span>
                        <select
                          value={disciplineReason}
                          onChange={(event) => setDisciplineReason(event.target.value as UserDisciplineReason)}
                        >
                          {accessOptions.disciplineReasons.map((reason) => (
                            <option key={reason} value={reason}>
                              {humanizeToken(reason)}
                            </option>
                          ))}
                        </select>
                      </label>
                      <label className="field field-wide">
                        <span>Note</span>
                        <textarea
                          value={disciplineNote}
                          onChange={(event) => setDisciplineNote(event.target.value)}
                          rows={3}
                          maxLength={500}
                          placeholder="Optional supporting detail for the action"
                        />
                      </label>
                    </div>

                    <div className="form-actions">
                      <button
                        onClick={() => {
                          if (!selectedUserId || !disciplineAction || !disciplineReason) {
                            return;
                          }
                          onApplyUserDiscipline(selectedUserId, disciplineAction, disciplineReason, disciplineNote);
                        }}
                        disabled={!selectedUserId || !disciplineAction || !disciplineReason}
                      >
                        Apply discipline action
                      </button>
                    </div>
                  </>
                ) : (
                  <p className="empty-state">
                    No discipline action is available for this user in the current state and scope.
                  </p>
                )}

                <div className="stack-list">
                  {disciplineHistory.map((record) => (
                    <article key={record.id} className="list-card">
                      <div>
                        <strong>{humanizeToken(record.action)} | {humanizeToken(record.reason)}</strong>
                        <p>
                          {record.actorUsername} changed {record.targetUsername} from{" "}
                          {humanizeToken(record.previousAccountStatus)} to {humanizeToken(record.resultingAccountStatus)}.
                        </p>
                        {record.note ? <p>{record.note}</p> : null}
                        <small>{formatDateTime(record.createdAt)}</small>
                      </div>
                    </article>
                  ))}
                </div>
                {disciplineHistory.length === 0 ? (
                  <p className="empty-state">No discipline actions have been recorded for this user.</p>
                ) : null}
              </div>
            </>
          ) : (
            <p className="empty-state">Select a user to inspect individual access.</p>
          )}
        </div>
      </div>
    </div>
  );
}
