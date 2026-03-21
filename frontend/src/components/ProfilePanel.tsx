import { humanizeToken } from "../lib/format";
import type { ProfilePanelProps } from "../view-models";

export function ProfilePanel({ profile, onManageAccount }: ProfilePanelProps) {
  return (
    <div className="surface">
      <div className="section-heading">
        <div>
          <p className="section-label">Profile</p>
          <h2>Access context</h2>
        </div>
        <button className="button-secondary" onClick={onManageAccount}>
          Manage profile & password
        </button>
      </div>
      <div className="profile-card">
        <div>
          <span className="profile-label">User</span>
          <strong>{profile.username}</strong>
        </div>
        <div>
          <span className="profile-label">Email</span>
          <strong>{profile.email ?? "No email claim"}</strong>
        </div>
        <div>
          <span className="profile-label">Role</span>
          <strong>{humanizeToken(profile.role)}</strong>
        </div>
        <div>
          <span className="profile-label">Account status</span>
          <strong>{humanizeToken(profile.accountStatus)}</strong>
        </div>
        <div>
          <span className="profile-label">Membership status</span>
          <strong>{humanizeToken(profile.membershipStatus)}</strong>
        </div>
        <div>
          <span className="profile-label">Scope</span>
          <strong>{humanizeToken(profile.scope)}</strong>
        </div>
        <div>
          <span className="profile-label">Branch</span>
          <strong>{profile.branch?.name ?? "Global"}</strong>
        </div>
        <div>
          <span className="profile-label">Home branch</span>
          <strong>{profile.homeBranch?.name ?? "Not assigned"}</strong>
        </div>
        <div>
          <span className="profile-label">Permissions</span>
          <strong>{profile.permissions.length}</strong>
        </div>
      </div>
    </div>
  );
}
