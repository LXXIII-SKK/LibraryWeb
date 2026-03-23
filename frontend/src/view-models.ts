import type {
  AccessOptions,
  ActivityLog,
  Book,
  BookHolding,
  BorrowingExceptionAction,
  Borrowing,
  LibraryBranch,
  LibraryLocation,
  DiscoveryBook,
  Fine,
  LibraryPolicy,
  Profile,
  Reservation,
  StaffNotification,
  UpcomingBook,
  UserDisciplineActionType,
  UserDisciplineReason,
  UserDisciplineRecord,
  UserAccess,
} from "./types";
import type { FormEvent } from "react";

export type Message = {
  tone: "success" | "error" | "info";
  text: string;
};

export type BookFormState = {
  title: string;
  author: string;
  category: string;
  isbn: string;
  tags: string;
  coverImageUrl: string | null;
};

export type AccessFormState = {
  role: string;
  accountStatus: string;
  membershipStatus: string;
  branchId: string;
  homeBranchId: string;
};

export type StaffRegistrationFormState = {
  username: string;
  email: string;
  password: string;
  role: string;
  accountStatus: string;
  branchId: string;
  homeBranchId: string;
  requirePasswordChange: boolean;
};

export type PolicyFormState = {
  standardLoanDays: number;
  renewalDays: number;
  maxRenewals: number;
  finePerOverdueDay: string;
  fineWaiverLimit: string;
  allowRenewalWithActiveReservations: boolean;
};

export type BranchFormState = {
  code: string;
  name: string;
  address: string;
  phone: string;
  active: boolean;
};

export type LocationFormState = {
  branchId: string;
  code: string;
  name: string;
  floorLabel: string;
  zoneLabel: string;
  active: boolean;
};

export type HoldingFormState = {
  bookId: string;
  branchId: string;
  locationId: string;
  format: "PHYSICAL" | "DIGITAL";
  totalQuantity: number;
  availableQuantity: number;
  accessUrl: string;
  active: boolean;
};

export type UpcomingBookFormState = {
  title: string;
  author: string;
  category: string;
  isbn: string;
  summary: string;
  expectedAt: string;
  branchId: string;
  tags: string;
};

export type NotificationFormState = {
  title: string;
  message: string;
  branchId: string;
  targetRoles: string[];
};

export type DisciplineRequestFormState = {
  targetUsername: string;
  action: UserDisciplineActionType;
  reason: UserDisciplineReason;
  note: string;
};

export type InventoryStats = {
  totalTitles: number;
  availableCopies: number;
  totalCopies: number;
  outOfStock: number;
};

export type BorrowingStats = {
  active: number;
  returned: number;
};

export type NavigationBarProps = {
  visible: boolean;
  signedIn: boolean;
  canAccessOperations: boolean;
  canViewNotifications: boolean;
  unreadNotificationCount: number;
  username: string;
  roleLabel: string;
  currentPath: "home" | "books" | "upcoming" | "account" | "admin" | "book";
  notificationsOpen: boolean;
  onNavigateHome: () => void;
  onNavigateBooks: () => void;
  onNavigateUpcoming: () => void;
  onNavigateAccount: () => void;
  onNavigateAdmin: () => void;
  onToggleNotifications: () => void;
  onLogin: () => void;
  onRegister: () => void;
  onLogout: () => void;
};

export type WelcomePageProps = {
  inventoryStats: InventoryStats;
  myBorrowingStats: BorrowingStats;
  recommendations: DiscoveryBook[];
  mostBorrowed: DiscoveryBook[];
  mostViewed: DiscoveryBook[];
  upcomingBooks: UpcomingBook[];
  onOpenBook: (bookId: number) => void;
  onNavigateUpcoming: () => void;
};

export type CatalogPanelProps = {
  loading: boolean;
  canBorrow: boolean;
  canReserve: boolean;
  canManageCatalog: boolean;
  query: string;
  categoryFilter: string;
  tagFilter: string;
  categories: string[];
  tags: string[];
  books: Book[];
  onQueryChange: (value: string) => void;
  onCategoryChange: (value: string) => void;
  onTagChange: (value: string) => void;
  onBorrow: (bookId: number, holdingId?: number | null) => void;
  onReserve: (bookId: number) => void;
  onStartEdit: (book: Book) => void;
  onOpenBook: (bookId: number) => void;
};

export type ProfilePanelProps = {
  profile: Profile;
  onManageAccount: () => void;
};

export type ActivityPanelProps = {
  label: string;
  title: string;
  emptyMessage: string;
  logs: ActivityLog[];
};

export type AdminConsoleProps = {
  roleLabel: string;
  canManageCatalog: boolean;
  canDeleteCatalog: boolean;
  canManageInventory: boolean;
  canReadNotifications: boolean;
  canSendNotifications: boolean;
  canRequestDisciplineReview: boolean;
  canSeeBorrowings: boolean;
  canStaffCheckout: boolean;
  canForceReturn: boolean;
  canOverrideBorrowings: boolean;
  canManageBorrowingExceptions: boolean;
  canReadReservations: boolean;
  canManageReservations: boolean;
  canReadFines: boolean;
  canWaiveFines: boolean;
  canReadUsers: boolean;
  canManageUsers: boolean;
  canRegisterStaff: boolean;
  canReadPolicies: boolean;
  canManagePolicies: boolean;
  canManageBranches: boolean;
  editingBookId: number | null;
  editingBranchId: number | null;
  editingLocationId: number | null;
  editingHoldingId: number | null;
  editingUpcomingBookId: number | null;
  bookForm: BookFormState;
  branchForm: BranchFormState;
  locationForm: LocationFormState;
  holdingForm: HoldingFormState;
  upcomingBookForm: UpcomingBookFormState;
  notificationForm: NotificationFormState;
  disciplineRequestForm: DisciplineRequestFormState;
  books: Book[];
  holdings: BookHolding[];
  borrowings: Borrowing[];
  reservations: Reservation[];
  fines: Fine[];
  branches: LibraryBranch[];
  locations: LibraryLocation[];
  notifications: StaffNotification[];
  unreadNotifications: number;
  upcomingBooks: UpcomingBook[];
  users: UserAccess[];
  selectedUserId: number | null;
  selectedUser: UserAccess | null;
  disciplineHistory: UserDisciplineRecord[];
  accessOptions: AccessOptions | null;
  accessForm: AccessFormState | null;
  staffRegistrationOptions: AccessOptions | null;
  staffRegistrationForm: StaffRegistrationFormState | null;
  policy: LibraryPolicy | null;
  policyForm: PolicyFormState | null;
  coverPreviewUrl: string | null;
  onUpdateField: <K extends keyof BookFormState>(field: K, value: BookFormState[K]) => void;
  onCoverSelected: (file: File | null) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onReset: () => void;
  onStartEdit: (book: Book) => void;
  onDelete: (book: Book) => void;
  onUpdateLocationField: <K extends keyof LocationFormState>(field: K, value: LocationFormState[K]) => void;
  onSubmitLocation: (event: FormEvent<HTMLFormElement>) => void;
  onResetLocation: () => void;
  onStartEditLocation: (location: LibraryLocation) => void;
  onUpdateHoldingField: <K extends keyof HoldingFormState>(field: K, value: HoldingFormState[K]) => void;
  onSubmitHolding: (event: FormEvent<HTMLFormElement>) => void;
  onResetHolding: () => void;
  onStartEditHolding: (holding: BookHolding) => void;
  onUpdateUpcomingField: <K extends keyof UpcomingBookFormState>(field: K, value: UpcomingBookFormState[K]) => void;
  onSubmitUpcoming: (event: FormEvent<HTMLFormElement>) => void;
  onResetUpcoming: () => void;
  onStartEditUpcoming: (upcomingBook: UpcomingBook) => void;
  onDeleteUpcoming: (upcomingBook: UpcomingBook) => void;
  onUpdateNotificationField: <K extends keyof NotificationFormState>(field: K, value: NotificationFormState[K]) => void;
  onSendNotification: () => void;
  onUpdateDisciplineRequestField: <K extends keyof DisciplineRequestFormState>(
    field: K,
    value: DisciplineRequestFormState[K],
  ) => void;
  onSubmitDisciplineRequest: () => void;
  onMarkNotificationRead: (notificationId: number) => void;
  onReturn: (transactionId: number) => void;
  onStaffCheckout: (userId: number, bookId: number, holdingId?: number | null, reservationId?: number | null) => void;
  onOverrideBorrowing: (transactionId: number, dueAt: string | null, reason: string) => void;
  onRecordBorrowingException: (transactionId: number, action: BorrowingExceptionAction, note: string) => void;
  onPrepareReservation: (reservationId: number, holdingId?: number | null) => void;
  onReadyReservation: (reservationId: number) => void;
  onExpireReservation: (reservationId: number) => void;
  onNoShowReservation: (reservationId: number) => void;
  onWaiveFine: (fineId: number, note: string) => void;
  onSelectUser: (userId: number) => void;
  onUpdateAccessField: <K extends keyof AccessFormState>(field: K, value: AccessFormState[K]) => void;
  onSaveAccess: () => void;
  onUpdateStaffRegistrationField: <K extends keyof StaffRegistrationFormState>(
    field: K,
    value: StaffRegistrationFormState[K],
  ) => void;
  onRegisterStaff: () => void;
  onApplyUserDiscipline: (
    userId: number,
    action: UserDisciplineActionType,
    reason: UserDisciplineReason,
    note: string,
  ) => void;
  onUpdateBranchField: <K extends keyof BranchFormState>(field: K, value: BranchFormState[K]) => void;
  onSubmitBranch: (event: FormEvent<HTMLFormElement>) => void;
  onResetBranch: () => void;
  onStartEditBranch: (branch: LibraryBranch) => void;
  onUpdatePolicyField: <K extends keyof PolicyFormState>(field: K, value: PolicyFormState[K]) => void;
  onSavePolicy: () => void;
};

export type BookDetailPageProps = {
  book: Book;
  canBorrow: boolean;
  canReserve: boolean;
  canManageCatalog: boolean;
  pickupBranches: LibraryBranch[];
  pickupBranchId: number | null;
  relatedBorrowings: Borrowing[];
  relatedLogs: ActivityLog[];
  onBack: () => void;
  onBorrow: (bookId: number, holdingId?: number | null) => void;
  onReserve: (bookId: number, pickupBranchId?: number | null) => void;
  onPickupBranchChange: (branchId: number) => void;
  onStartEdit: (book: Book) => void;
};

export type BooksWorkspacePageProps = {
  loading: boolean;
  canBorrow: boolean;
  canReserve: boolean;
  canManageCatalog: boolean;
  query: string;
  categoryFilter: string;
  tagFilter: string;
  categories: string[];
  tags: string[];
  books: Book[];
  onQueryChange: (value: string) => void;
  onCategoryChange: (value: string) => void;
  onTagChange: (value: string) => void;
  onBorrow: (bookId: number, holdingId?: number | null) => void;
  onReserve: (bookId: number) => void;
  onStartEdit: (book: Book) => void;
  onOpenBook: (bookId: number) => void;
  onNavigateUpcoming: () => void;
};

export type UpcomingWorkspacePageProps = {
  upcomingBooks: UpcomingBook[];
  onNavigateBooks: () => void;
};

export type UserHubPageProps = {
  signedIn: boolean;
  canViewPersonalHistory: boolean;
  canReturnOwnBorrowings: boolean;
  canRenewOwnBorrowings: boolean;
  canViewReservations: boolean;
  canCancelOwnReservations: boolean;
  canViewFines: boolean;
  profile: Profile | null;
  borrowings: Borrowing[];
  reservations: Reservation[];
  fines: Fine[];
  notifications: StaffNotification[];
  logs: ActivityLog[];
  stats: BorrowingStats;
  onLogin: () => void;
  onRegister: () => void;
  onNavigateBooks: () => void;
  onManageAccount: () => void;
  onOpenBook: (bookId: number) => void;
  onReturn: (transactionId: number) => void;
  onRenew: (transactionId: number) => void;
  onOpenDigitalAccess: (transactionId: number) => void;
  onCollectReservation: (reservationId: number) => void;
  onCancelReservation: (reservationId: number) => void;
  onMarkNotificationRead: (notificationId: number) => void;
};

export type NotificationTrayProps = {
  open: boolean;
  notifications: StaffNotification[];
  onMarkRead: (notificationId: number) => void;
};
