export type HoldingFormat = "PHYSICAL" | "DIGITAL";
export type BookCopyStatus =
  | "AVAILABLE"
  | "UNAVAILABLE"
  | "RESERVED_FOR_PICKUP"
  | "IN_TRANSIT"
  | "BORROWED"
  | "CLAIMED_RETURNED"
  | "LOST"
  | "DAMAGED";
export type BookTransferStatus = "IN_TRANSIT" | "READY_FOR_PICKUP" | "COMPLETED" | "CANCELLED" | "EXPIRED";
export type Book = {
  id: number;
  title: string;
  author: string;
  category: string | null;
  isbn: string | null;
  totalQuantity: number;
  availableQuantity: number;
  viewCount: number;
  tags: string[];
  coverImageUrl: string | null;
  hasOnlineAccess: boolean;
  availability: BookHolding[];
};

export type BookFilters = {
  categories: string[];
  tags: string[];
};

export type AppRole = "MEMBER" | "LIBRARIAN" | "BRANCH_MANAGER" | "ADMIN" | "AUDITOR";
export type AccountStatus = "PENDING_VERIFICATION" | "ACTIVE" | "SUSPENDED" | "LOCKED" | "ARCHIVED";
export type MembershipStatus = "GOOD_STANDING" | "OVERDUE_RESTRICTED" | "BORROW_BLOCKED" | "EXPIRED";
export type AccessScope = "SELF" | "BRANCH" | "GLOBAL";
export type UserDisciplineActionType = "SUSPEND" | "BAN" | "REINSTATE";
export type BorrowingStatus = "BORROWED" | "CLAIMED_RETURNED" | "LOST" | "DAMAGED" | "RETURNED";
export type BorrowingExceptionAction = "CLAIM_RETURNED" | "MARK_LOST" | "MARK_DAMAGED";
export type UserDisciplineReason =
  | "OVERDUE_ABUSE"
  | "UNPAID_FEES"
  | "LOST_ITEMS"
  | "DAMAGED_ITEMS"
  | "IDENTITY_MISUSE"
  | "CONDUCT_VIOLATION"
  | "SECURITY_REVIEW"
  | "SPAM_OR_SYSTEM_ABUSE"
  | "POLICY_VIOLATION"
  | "APPEAL_APPROVED"
  | "ISSUE_RESOLVED"
  | "MANUAL_ADMIN_ACTION";

export type BranchSummary = {
  id: number;
  code: string;
  name: string;
  active: boolean;
};

export type LibraryBranch = BranchSummary & {
  address: string | null;
  phone: string | null;
};

export type LocationSummary = {
  id: number;
  code: string;
  name: string;
  floorLabel: string | null;
  zoneLabel: string | null;
  active: boolean;
};

export type LibraryLocation = LocationSummary & {
  branch: BranchSummary;
};

export type BookHolding = {
  id: number;
  bookId: number;
  bookTitle: string;
  format: HoldingFormat;
  branch: BranchSummary | null;
  location: LocationSummary | null;
  totalQuantity: number;
  availableQuantity: number;
  trackedCopyCount: number;
  active: boolean;
  onlineAccess: boolean;
};

export type BookCopy = {
  id: number;
  holdingId: number;
  bookId: number;
  bookTitle: string;
  barcode: string;
  status: BookCopyStatus;
  currentBranch: BranchSummary | null;
  currentLocation: LocationSummary | null;
};

export type BookTransfer = {
  id: number;
  copyId: number;
  copyBarcode: string;
  bookId: number;
  bookTitle: string;
  sourceHoldingId: number;
  sourceBranch: BranchSummary | null;
  sourceLocation: LocationSummary | null;
  destinationBranch: BranchSummary | null;
  status: BookTransferStatus;
  requestedAt: string;
  dispatchedAt: string;
  readyAt: string | null;
  completedAt: string | null;
  closedAt: string | null;
};

export type Profile = {
  id: number;
  username: string;
  email: string | null;
  role: AppRole;
  accountStatus: AccountStatus;
  membershipStatus: MembershipStatus;
  scope: AccessScope;
  branchId: number | null;
  homeBranchId: number | null;
  branch: BranchSummary | null;
  homeBranch: BranchSummary | null;
  permissions: string[];
};

export type Borrowing = {
  id: number;
  bookId: number;
  bookTitle: string;
  holdingId: number | null;
  copyId: number | null;
  copyBarcode: string | null;
  holdingFormat: HoldingFormat | null;
  branchName: string | null;
  locationName: string | null;
  digitalAccessAvailable: boolean;
  userId: number;
  username: string;
  borrowedAt: string;
  dueAt: string;
  lastRenewedAt: string | null;
  renewalCount: number;
  lastRenewalOverride: boolean;
  lastRenewalReason: string | null;
  exceptionRecordedAt: string | null;
  exceptionNote: string | null;
  returnedAt: string | null;
  status: BorrowingStatus;
};

export type Reservation = {
  id: number;
  bookId: number;
  bookTitle: string;
  userId: number;
  username: string;
  pickupBranch: BranchSummary | null;
  reservedHoldingId: number | null;
  reservedCopyId: number | null;
  reservedHoldingBranchName: string | null;
  transferId: number | null;
  transferStatus: BookTransferStatus | null;
  transferDestinationBranchName: string | null;
  reservedAt: string;
  transferRequestedAt: string | null;
  readyAt: string | null;
  expiresAt: string | null;
  updatedAt: string;
  status: "ACTIVE" | "IN_TRANSIT" | "READY_FOR_PICKUP" | "EXPIRED" | "CANCELLED" | "FULFILLED" | "NO_SHOW";
};

export type Fine = {
  id: number;
  userId: number;
  username: string;
  transactionId: number | null;
  bookId: number | null;
  bookTitle: string | null;
  amount: number;
  reason: string;
  status: "OPEN" | "WAIVED" | "PAID";
  createdAt: string;
  resolvedAt: string | null;
  resolvedByUsername: string | null;
  resolutionNote: string | null;
};

export type UserAccess = {
  id: number;
  username: string;
  email: string | null;
  role: AppRole;
  accountStatus: AccountStatus;
  membershipStatus: MembershipStatus;
  scope: AccessScope;
  branchId: number | null;
  homeBranchId: number | null;
  branch: BranchSummary | null;
  homeBranch: BranchSummary | null;
  permissions: string[];
};

export type AccessOptions = {
  roles: AppRole[];
  accountStatuses: AccountStatus[];
  membershipStatuses: MembershipStatus[];
  branches: BranchSummary[];
  disciplineActions: UserDisciplineActionType[];
  disciplineReasons: UserDisciplineReason[];
};

export type UserDisciplineRecord = {
  id: number;
  targetUserId: number;
  targetUsername: string;
  actorUserId: number;
  actorUsername: string;
  action: UserDisciplineActionType;
  reason: UserDisciplineReason;
  note: string | null;
  previousAccountStatus: AccountStatus;
  resultingAccountStatus: AccountStatus;
  createdAt: string;
};

export type UserDisciplineRequestSubmission = {
  targetUsername: string;
  action: UserDisciplineActionType;
  reason: UserDisciplineReason;
  note: string | null;
  notifiedRecipients: string[];
  createdAt: string;
};

export type LibraryPolicy = {
  standardLoanDays: number;
  renewalDays: number;
  maxRenewals: number;
  finePerOverdueDay: number;
  fineWaiverLimit: number;
  allowRenewalWithActiveReservations: boolean;
  updatedAt: string;
};

export type ActivityLog = {
  id: number;
  userId: number;
  username: string;
  bookId: number | null;
  bookTitle: string | null;
  activityType:
    | "VIEWED"
    | "BORROWED"
    | "RETURNED"
    | "RENEWED"
    | "BORROWING_EXCEPTION_RECORDED"
    | "RESERVED"
    | "RESERVATION_CANCELLED"
    | "RESERVATION_NO_SHOW"
    | "FINE_WAIVED"
    | "POLICY_UPDATED"
    | "TRANSFER_UPDATED"
    | "ACCESS_UPDATED"
    | "ACCESS_DISCIPLINE";
  message: string;
  occurredAt: string;
};

export type UpcomingBook = {
  id: number;
  title: string;
  author: string;
  category: string | null;
  isbn: string | null;
  summary: string | null;
  expectedAt: string;
  branch: BranchSummary | null;
  tags: string[];
};

export type StaffNotification = {
  id: number;
  title: string;
  message: string;
  branch: BranchSummary | null;
  targetUserId: number | null;
  targetUsername: string | null;
  targetRoles: AppRole[];
  createdByUsername: string;
  createdAt: string;
  readAt: string | null;
};

export type DigitalAccessLink = {
  transactionId: number;
  bookId: number;
  bookTitle: string;
  url: string;
};

export type DiscoveryBook = {
  id: number;
  title: string;
  author: string;
  category: string | null;
  isbn: string | null;
  totalQuantity: number;
  availableQuantity: number;
  tags: string[];
  coverImageUrl: string | null;
  weeklyCount: number;
  spotlight: string;
};

export type DiscoveryResponse = {
  recommendations: DiscoveryBook[];
  mostBorrowedThisWeek: DiscoveryBook[];
  mostViewedThisWeek: DiscoveryBook[];
};
