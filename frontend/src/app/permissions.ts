import { humanizeToken } from "../lib/format";
import type { Profile } from "../types";

export type AppPermissions = {
  canManageCatalog: boolean;
  canDeleteCatalog: boolean;
  canReadPersonalHistory: boolean;
  canReadOperationalBorrowings: boolean;
  canReadOperationalActivity: boolean;
  canReturnOwnBorrowings: boolean;
  canStaffCheckout: boolean;
  canForceReturn: boolean;
  canOverrideBorrowings: boolean;
  canManageBorrowingExceptions: boolean;
  canRenewOwnBorrowings: boolean;
  canReserveForSelf: boolean;
  canReadOwnReservations: boolean;
  canCancelOwnReservations: boolean;
  canReadOperationalReservations: boolean;
  canManageOperationalReservations: boolean;
  canReadOwnFines: boolean;
  canReadOperationalFines: boolean;
  canWaiveOperationalFines: boolean;
  canReadUsers: boolean;
  canManageUsers: boolean;
  canRegisterStaff: boolean;
  canReadPolicies: boolean;
  canManagePolicies: boolean;
  canManageBranches: boolean;
  canManageInventory: boolean;
  canRequestDisciplineReview: boolean;
  canReadStaffNotifications: boolean;
  canSendStaffNotifications: boolean;
  canBorrowForSelf: boolean;
  canAccessOperations: boolean;
  roleLabel: string;
};

export function deriveAppPermissions(profile: Profile | null, signedIn: boolean): AppPermissions {
  const permissionSet = new Set(profile?.permissions ?? []);
  const canManageCatalog = permissionSet.has("BOOK_CREATE") || permissionSet.has("BOOK_UPDATE");
  const canDeleteCatalog = profile?.role === "ADMIN";
  const canReadPersonalHistory = permissionSet.has("LOAN_SELF_READ");
  const canReadOperationalBorrowings = permissionSet.has("LOAN_READ_GLOBAL") || permissionSet.has("REPORT_BRANCH_READ");
  const canReadOperationalActivity =
    permissionSet.has("AUDIT_GLOBAL_READ") ||
    permissionSet.has("REPORT_GLOBAL_READ") ||
    (profile?.role === "BRANCH_MANAGER" && permissionSet.has("REPORT_BRANCH_READ"));
  const canReturnOwnBorrowings = permissionSet.has("LOAN_SELF_RETURN");
  const canStaffCheckout = permissionSet.has("LOAN_CREATE_BRANCH") || profile?.role === "ADMIN";
  const canForceReturn = permissionSet.has("LOAN_CLOSE_BRANCH") || profile?.role === "ADMIN";
  const canOverrideBorrowings = permissionSet.has("LOAN_OVERRIDE_BRANCH") || profile?.role === "ADMIN";
  const canManageBorrowingExceptions = canForceReturn;
  const canRenewOwnBorrowings = Boolean(
    signedIn &&
      profile &&
      profile.role === "MEMBER" &&
      permissionSet.has("LOAN_SELF_RENEW") &&
      profile.accountStatus === "ACTIVE" &&
      profile.membershipStatus === "GOOD_STANDING",
  );
  const canReserveForSelf = Boolean(
    signedIn &&
      profile &&
      profile.role === "MEMBER" &&
      permissionSet.has("RESERVATION_SELF_CREATE") &&
      profile.accountStatus === "ACTIVE" &&
      profile.membershipStatus === "GOOD_STANDING",
  );
  const canReadOwnReservations =
    permissionSet.has("RESERVATION_SELF_CREATE") || permissionSet.has("RESERVATION_SELF_CANCEL");
  const canCancelOwnReservations = permissionSet.has("RESERVATION_SELF_CANCEL");
  const canReadOperationalReservations =
    permissionSet.has("RESERVATION_MANAGE_BRANCH") ||
    permissionSet.has("LOAN_READ_GLOBAL") ||
    permissionSet.has("REPORT_GLOBAL_READ");
  const canManageOperationalReservations = permissionSet.has("RESERVATION_MANAGE_BRANCH") || profile?.role === "ADMIN";
  const canReadOwnFines = permissionSet.has("FINE_SELF_READ");
  const canReadOperationalFines = permissionSet.has("FINE_READ_BRANCH") || permissionSet.has("FINE_READ_GLOBAL");
  const canWaiveOperationalFines = permissionSet.has("FINE_WAIVE_BRANCH") || profile?.role === "ADMIN";
  const canReadUsers =
    permissionSet.has("USER_READ_GLOBAL") ||
    permissionSet.has("USER_MANAGE_GLOBAL") ||
    permissionSet.has("MEMBER_READ_BRANCH");
  const canManageUsers =
    permissionSet.has("USER_MANAGE_GLOBAL") ||
    permissionSet.has("MEMBER_VERIFY_BRANCH") ||
    permissionSet.has("APPROVAL_BRANCH");
  const canRegisterStaff =
    profile?.role === "ADMIN" && profile.accountStatus === "ACTIVE" && permissionSet.has("USER_MANAGE_GLOBAL");
  const canReadPolicies = permissionSet.has("POLICY_READ") || permissionSet.has("POLICY_MANAGE_GLOBAL");
  const canManagePolicies = permissionSet.has("POLICY_MANAGE_GLOBAL");
  const canManageBranches = permissionSet.has("BRANCH_MANAGE_GLOBAL");
  const canManageInventory =
    permissionSet.has("COPY_CREATE") || permissionSet.has("COPY_UPDATE") || profile?.role === "ADMIN";
  const canRequestDisciplineReview = permissionSet.has("DISCIPLINE_REQUEST_BRANCH");
  const canReadStaffNotifications = Boolean(signedIn && profile && profile.accountStatus === "ACTIVE");
  const canSendStaffNotifications = Boolean(signedIn && profile && ["BRANCH_MANAGER", "ADMIN"].includes(profile.role));
  const canBorrowForSelf = Boolean(
    signedIn &&
      profile &&
      profile.role === "MEMBER" &&
      permissionSet.has("LOAN_SELF_CREATE") &&
      profile.accountStatus === "ACTIVE" &&
      profile.membershipStatus === "GOOD_STANDING",
  );
  const canAccessOperations =
    canManageCatalog ||
    canReadOperationalBorrowings ||
    canReadOperationalActivity ||
    canReadOperationalReservations ||
    canReadOperationalFines ||
    canReadUsers ||
    canReadPolicies ||
    canManageBranches ||
    canManageInventory ||
    canRequestDisciplineReview;

  return {
    canManageCatalog,
    canDeleteCatalog,
    canReadPersonalHistory,
    canReadOperationalBorrowings,
    canReadOperationalActivity,
    canReturnOwnBorrowings,
    canStaffCheckout,
    canForceReturn,
    canOverrideBorrowings,
    canManageBorrowingExceptions,
    canRenewOwnBorrowings,
    canReserveForSelf,
    canReadOwnReservations,
    canCancelOwnReservations,
    canReadOperationalReservations,
    canManageOperationalReservations,
    canReadOwnFines,
    canReadOperationalFines,
    canWaiveOperationalFines,
    canReadUsers,
    canManageUsers,
    canRegisterStaff,
    canReadPolicies,
    canManagePolicies,
    canManageBranches,
    canManageInventory,
    canRequestDisciplineReview,
    canReadStaffNotifications,
    canSendStaffNotifications,
    canBorrowForSelf,
    canAccessOperations,
    roleLabel: profile ? humanizeToken(profile.role) : "Authenticated",
  };
}
