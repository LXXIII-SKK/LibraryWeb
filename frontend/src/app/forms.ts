import type {
  AccessFormState,
  BookFormState,
  BranchFormState,
  DisciplineRequestFormState,
  HoldingFormState,
  LocationFormState,
  NotificationFormState,
  PolicyFormState,
  StaffRegistrationFormState,
  UpcomingBookFormState,
} from "../view-models";
import type { AccessOptions, BookHolding, LibraryLocation, LibraryPolicy, UpcomingBook, UserAccess } from "../types";

export const emptyBookForm: BookFormState = {
  title: "",
  author: "",
  category: "",
  isbn: "",
  tags: "",
  coverImageUrl: null,
};

export const emptyBranchForm: BranchFormState = {
  code: "",
  name: "",
  address: "",
  phone: "",
  active: true,
};

export const emptyLocationForm: LocationFormState = {
  branchId: "",
  code: "",
  name: "",
  floorLabel: "",
  zoneLabel: "",
  active: true,
};

export const emptyHoldingForm: HoldingFormState = {
  bookId: "",
  branchId: "",
  locationId: "",
  format: "PHYSICAL",
  totalQuantity: 1,
  availableQuantity: 1,
  accessUrl: "",
  active: true,
};

export const emptyUpcomingBookForm: UpcomingBookFormState = {
  title: "",
  author: "",
  category: "",
  isbn: "",
  summary: "",
  expectedAt: "",
  branchId: "",
  tags: "",
};

export const emptyNotificationForm: NotificationFormState = {
  title: "",
  message: "",
  branchId: "",
  targetRoles: [],
};

export const emptyDisciplineRequestForm: DisciplineRequestFormState = {
  targetUsername: "",
  action: "SUSPEND",
  reason: "POLICY_VIOLATION",
  note: "",
};

export function createStaffRegistrationForm(options: AccessOptions): StaffRegistrationFormState {
  return {
    username: "",
    email: "",
    password: "",
    role: options.roles[0] ?? "LIBRARIAN",
    accountStatus: options.accountStatuses[0] ?? "ACTIVE",
    branchId: "",
    homeBranchId: "",
    requirePasswordChange: true,
  };
}

export function parseTagInput(value: string) {
  return value
    .split(",")
    .map((tag) => tag.trim().toLowerCase())
    .filter(Boolean);
}

export function toAccessForm(user: UserAccess): AccessFormState {
  return {
    role: user.role,
    accountStatus: user.accountStatus,
    membershipStatus: user.membershipStatus,
    branchId: user.branchId?.toString() ?? "",
    homeBranchId: user.homeBranchId?.toString() ?? "",
  };
}

export function toPolicyForm(policy: LibraryPolicy): PolicyFormState {
  return {
    standardLoanDays: policy.standardLoanDays,
    renewalDays: policy.renewalDays,
    maxRenewals: policy.maxRenewals,
    finePerOverdueDay: policy.finePerOverdueDay.toFixed(2),
    fineWaiverLimit: policy.fineWaiverLimit.toFixed(2),
    allowRenewalWithActiveReservations: policy.allowRenewalWithActiveReservations,
  };
}

export function toLocationForm(location: LibraryLocation): LocationFormState {
  return {
    branchId: location.branch.id.toString(),
    code: location.code,
    name: location.name,
    floorLabel: location.floorLabel ?? "",
    zoneLabel: location.zoneLabel ?? "",
    active: location.active,
  };
}

export function toHoldingForm(holding: BookHolding): HoldingFormState {
  return {
    bookId: holding.bookId.toString(),
    branchId: holding.branch?.id?.toString() ?? "",
    locationId: holding.location?.id?.toString() ?? "",
    format: holding.format,
    totalQuantity: holding.totalQuantity,
    availableQuantity: holding.availableQuantity,
    accessUrl: "",
    active: holding.active,
  };
}

export function toUpcomingBookForm(upcomingBook: UpcomingBook): UpcomingBookFormState {
  return {
    title: upcomingBook.title,
    author: upcomingBook.author,
    category: upcomingBook.category ?? "",
    isbn: upcomingBook.isbn ?? "",
    summary: upcomingBook.summary ?? "",
    expectedAt: toDateTimeInput(upcomingBook.expectedAt),
    branchId: upcomingBook.branch?.id?.toString() ?? "",
    tags: upcomingBook.tags.join(", "),
  };
}

export function parseOptionalNumber(value: string) {
  if (!value.trim()) {
    return null;
  }

  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

export function parseOptionalString(value: string) {
  const trimmed = value.trim();
  return trimmed ? trimmed : undefined;
}

export function toDateTimeInput(value: string | null | undefined) {
  if (!value) {
    return "";
  }
  return new Date(value).toISOString().slice(0, 16);
}
