import { useDeferredValue, useEffect, useMemo, useState, type FormEvent } from "react";

import {
  applyUserDiscipline,
  borrowBook,
  cancelReservation,
  collectReservation,
  createBranch,
  createBook,
  createInventoryHolding,
  createLocation,
  createNotification,
  createReservation,
  createUpcomingBook,
  deleteBook,
  deleteUpcomingBook,
  fetchActivityLogs,
  fetchAllActivityLogs,
  fetchAllBorrowings,
  fetchAllFines,
  fetchAllReservations,
  fetchBranches,
  fetchBook,
  fetchBookFilters,
  fetchBooks,
  fetchBorrowings,
  fetchDiscovery,
  fetchDigitalAccess,
  fetchInventoryHoldings,
  fetchLocations,
  fetchNotifications,
  fetchFines,
  fetchPolicy,
  fetchProfile,
  fetchPublicBranches,
  fetchUpcomingBooks,
  fetchReservations,
  fetchUserDisciplineHistory,
  fetchUser,
  fetchUserAccessOptions,
  fetchUsers,
  markNotificationRead,
  markReservationNoShow,
  markReservationReady,
  prepareReservation,
  recordBookView,
  renewBorrowing,
  returnBook,
  staffCheckoutBook,
  submitDisciplineRequest,
  updateBook,
  updateBranch,
  updateInventoryHolding,
  updateLocation,
  updateUpcomingBook,
  updatePolicy,
  updateUserAccess,
  uploadBookCover,
  waiveFine,
  expireReservation,
} from "./api";
import { AdminPage } from "./components/AdminPage";
import { BookDetailPage } from "./components/BookDetailPage";
import { BooksWorkspacePage } from "./components/BooksWorkspacePage";
import { NavigationBar } from "./components/NavigationBar";
import { NotificationTray } from "./components/NotificationTray";
import { UserHubPage } from "./components/UserHubPage";
import { WelcomePage } from "./components/WelcomePage";
import { initAuth, login, logout, manageAccount, register, username } from "./auth";
import { humanizeToken } from "./lib/format";
import type {
  AccessOptions,
  ActivityLog,
  Book,
  BookHolding,
  BookFilters,
  DigitalAccessLink,
  LibraryBranch,
  LibraryLocation,
  Borrowing,
  DiscoveryResponse,
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
import type {
  AccessFormState,
  BookFormState,
  HoldingFormState,
  BranchFormState,
  BorrowingStats,
  InventoryStats,
  LocationFormState,
  Message,
  NotificationFormState,
  PolicyFormState,
  UpcomingBookFormState,
  DisciplineRequestFormState,
} from "./view-models";

const emptyBookForm: BookFormState = {
  title: "",
  author: "",
  category: "",
  isbn: "",
  tags: "",
  coverImageUrl: null,
};

const emptyBranchForm: BranchFormState = {
  code: "",
  name: "",
  address: "",
  phone: "",
  active: true,
};

const emptyLocationForm: LocationFormState = {
  branchId: "",
  code: "",
  name: "",
  floorLabel: "",
  zoneLabel: "",
  active: true,
};

const emptyHoldingForm: HoldingFormState = {
  bookId: "",
  branchId: "",
  locationId: "",
  format: "PHYSICAL",
  totalQuantity: 1,
  availableQuantity: 1,
  accessUrl: "",
  active: true,
};

const emptyUpcomingBookForm: UpcomingBookFormState = {
  title: "",
  author: "",
  category: "",
  isbn: "",
  summary: "",
  expectedAt: "",
  branchId: "",
  tags: "",
};

const emptyNotificationForm: NotificationFormState = {
  title: "",
  message: "",
  branchId: "",
  targetRoles: [],
};

const emptyDisciplineRequestForm: DisciplineRequestFormState = {
  targetUsername: "",
  action: "SUSPEND",
  reason: "POLICY_VIOLATION",
  note: "",
};

type RouteState =
  | { name: "home" }
  | { name: "books" }
  | { name: "account" }
  | { name: "admin" }
  | { name: "book"; bookId: number };

function resolveRoute(pathname: string): RouteState {
  const bookMatch = pathname.match(/^\/books\/(\d+)$/);
  if (bookMatch) {
    return { name: "book", bookId: Number(bookMatch[1]) };
  }

  if (pathname === "/books") {
    return { name: "books" };
  }

  if (pathname === "/me") {
    return { name: "account" };
  }

  if (pathname === "/admin") {
    return { name: "admin" };
  }

  return { name: "home" };
}

function parseTagInput(value: string) {
  return value
    .split(",")
    .map((tag) => tag.trim().toLowerCase())
    .filter(Boolean);
}

function toAccessForm(user: UserAccess): AccessFormState {
  return {
    role: user.role,
    accountStatus: user.accountStatus,
    membershipStatus: user.membershipStatus,
    branchId: user.branchId?.toString() ?? "",
    homeBranchId: user.homeBranchId?.toString() ?? "",
  };
}

function toPolicyForm(policy: LibraryPolicy): PolicyFormState {
  return {
    standardLoanDays: policy.standardLoanDays,
    renewalDays: policy.renewalDays,
    maxRenewals: policy.maxRenewals,
    finePerOverdueDay: policy.finePerOverdueDay.toFixed(2),
    fineWaiverLimit: policy.fineWaiverLimit.toFixed(2),
    allowRenewalWithActiveReservations: policy.allowRenewalWithActiveReservations,
  };
}

function toLocationForm(location: LibraryLocation): LocationFormState {
  return {
    branchId: location.branch.id.toString(),
    code: location.code,
    name: location.name,
    floorLabel: location.floorLabel ?? "",
    zoneLabel: location.zoneLabel ?? "",
    active: location.active,
  };
}

function toHoldingForm(holding: BookHolding): HoldingFormState {
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

function toUpcomingBookForm(upcomingBook: UpcomingBook): UpcomingBookFormState {
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

function parseOptionalNumber(value: string) {
  if (!value.trim()) {
    return null;
  }

  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function parseOptionalString(value: string) {
  const trimmed = value.trim();
  return trimmed ? trimmed : undefined;
}

function toDateTimeInput(value: string | null | undefined) {
  if (!value) {
    return "";
  }
  return new Date(value).toISOString().slice(0, 16);
}

export default function App() {
  const [ready, setReady] = useState(false);
  const [signedIn, setSignedIn] = useState(false);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<Message | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [books, setBooks] = useState<Book[]>([]);
  const [filters, setFilters] = useState<BookFilters>({ categories: [], tags: [] });
  const [discovery, setDiscovery] = useState<DiscoveryResponse>({
    recommendations: [],
    mostBorrowedThisWeek: [],
    mostViewedThisWeek: [],
  });
  const [upcomingBooks, setUpcomingBooks] = useState<UpcomingBook[]>([]);
  const [borrowings, setBorrowings] = useState<Borrowing[]>([]);
  const [allBorrowings, setAllBorrowings] = useState<Borrowing[]>([]);
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [allReservations, setAllReservations] = useState<Reservation[]>([]);
  const [fines, setFines] = useState<Fine[]>([]);
  const [allFines, setAllFines] = useState<Fine[]>([]);
  const [activityLogs, setActivityLogs] = useState<ActivityLog[]>([]);
  const [allActivityLogs, setAllActivityLogs] = useState<ActivityLog[]>([]);
  const [users, setUsers] = useState<UserAccess[]>([]);
  const [holdings, setHoldings] = useState<BookHolding[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [selectedUser, setSelectedUser] = useState<UserAccess | null>(null);
  const [disciplineHistory, setDisciplineHistory] = useState<UserDisciplineRecord[]>([]);
  const [accessOptions, setAccessOptions] = useState<AccessOptions | null>(null);
  const [accessForm, setAccessForm] = useState<AccessFormState | null>(null);
  const [policy, setPolicy] = useState<LibraryPolicy | null>(null);
  const [policyForm, setPolicyForm] = useState<PolicyFormState | null>(null);
  const [branches, setBranches] = useState<LibraryBranch[]>([]);
  const [publicBranches, setPublicBranches] = useState<LibraryBranch[]>([]);
  const [locations, setLocations] = useState<LibraryLocation[]>([]);
  const [notifications, setNotifications] = useState<StaffNotification[]>([]);
  const [query, setQuery] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("all");
  const [tagFilter, setTagFilter] = useState("all");
  const [bookForm, setBookForm] = useState<BookFormState>(emptyBookForm);
  const [branchForm, setBranchForm] = useState<BranchFormState>(emptyBranchForm);
  const [locationForm, setLocationForm] = useState<LocationFormState>(emptyLocationForm);
  const [holdingForm, setHoldingForm] = useState<HoldingFormState>(emptyHoldingForm);
  const [upcomingBookForm, setUpcomingBookForm] = useState<UpcomingBookFormState>(emptyUpcomingBookForm);
  const [notificationForm, setNotificationForm] = useState<NotificationFormState>(emptyNotificationForm);
  const [disciplineRequestForm, setDisciplineRequestForm] =
    useState<DisciplineRequestFormState>(emptyDisciplineRequestForm);
  const [editingBookId, setEditingBookId] = useState<number | null>(null);
  const [editingBranchId, setEditingBranchId] = useState<number | null>(null);
  const [editingLocationId, setEditingLocationId] = useState<number | null>(null);
  const [editingHoldingId, setEditingHoldingId] = useState<number | null>(null);
  const [editingUpcomingBookId, setEditingUpcomingBookId] = useState<number | null>(null);
  const [pendingCoverFile, setPendingCoverFile] = useState<File | null>(null);
  const [route, setRoute] = useState<RouteState>(() => resolveRoute(window.location.pathname));
  const [selectedBook, setSelectedBook] = useState<Book | null>(null);
  const [selectedPickupBranchId, setSelectedPickupBranchId] = useState<number | null>(null);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const deferredQuery = useDeferredValue(query);

  const permissionSet = useMemo(() => new Set(profile?.permissions ?? []), [profile]);
  const canManageCatalog = permissionSet.has("BOOK_CREATE") || permissionSet.has("BOOK_UPDATE");
  const canDeleteCatalog = profile?.role === "ADMIN";
  const canReadPersonalHistory = permissionSet.has("LOAN_SELF_READ");
  const canReadOperationalBorrowings = permissionSet.has("LOAN_READ_GLOBAL") || permissionSet.has("REPORT_BRANCH_READ");
  const canReadOperationalActivity =
    permissionSet.has("AUDIT_GLOBAL_READ") ||
    permissionSet.has("REPORT_GLOBAL_READ") ||
    (profile?.role === "BRANCH_MANAGER" && permissionSet.has("REPORT_BRANCH_READ"));
  const canReturnOwnBorrowings = permissionSet.has("LOAN_SELF_RETURN");
  const canForceReturn = permissionSet.has("LOAN_CLOSE_BRANCH") || profile?.role === "ADMIN";
  const canRenewOwnBorrowings =
    Boolean(
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
  const roleLabel = profile ? humanizeToken(profile.role) : "Authenticated";

  const inventoryStats = useMemo<InventoryStats>(() => {
    const totalTitles = books.length;
    const availableCopies = books.reduce((sum, book) => sum + book.availableQuantity, 0);
    const totalCopies = books.reduce((sum, book) => sum + book.totalQuantity, 0);
    const outOfStock = books.filter((book) => book.availableQuantity === 0).length;

    return { totalTitles, availableCopies, totalCopies, outOfStock };
  }, [books]);

  const myBorrowingStats = useMemo<BorrowingStats>(() => {
    const active = borrowings.filter((item) => item.status === "BORROWED").length;
    const returned = borrowings.filter((item) => item.status === "RETURNED").length;
    return { active, returned };
  }, [borrowings]);

  useEffect(() => {
    initAuth()
      .then((authenticated) => {
        setSignedIn(authenticated);
      })
      .catch((error: unknown) => {
        setMessage({
          tone: "error",
          text: error instanceof Error ? error.message : "Authentication initialization failed",
        });
      })
      .finally(() => setReady(true));
  }, []);

  useEffect(() => {
    const handlePopState = () => setRoute(resolveRoute(window.location.pathname));
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  useEffect(() => {
    return () => {
      if (bookForm.coverImageUrl?.startsWith("blob:")) {
        URL.revokeObjectURL(bookForm.coverImageUrl);
      }
    };
  }, [bookForm.coverImageUrl]);

  useEffect(() => {
    if (!ready) {
      return;
    }

    void loadPublicData();
  }, [ready, deferredQuery, categoryFilter, tagFilter]);

  useEffect(() => {
    if (!ready) {
      return;
    }

    void loadPrivateData();
  }, [ready, signedIn]);

  useEffect(() => {
    if (route.name !== "book") {
      setSelectedBook(null);
      return;
    }

    void (async () => {
      try {
        const book = await fetchBook(route.bookId);
        setSelectedBook(book);
        if (signedIn && profile && profile.role !== "AUDITOR") {
          await recordBookView(route.bookId);
        }
      } catch (error) {
        showError(error);
      }
    })();
  }, [route, signedIn, profile?.role]);

  useEffect(() => {
    if (!canReadUsers) {
      setSelectedUserId(null);
      setSelectedUser(null);
      setAccessForm(null);
      return;
    }

    if (users.length > 0 && (selectedUserId === null || !users.some((user) => user.id === selectedUserId))) {
      setSelectedUserId(users[0].id);
    }
  }, [canReadUsers, users, selectedUserId]);

  useEffect(() => {
    if (!canReadUsers || selectedUserId === null) {
      setSelectedUser(null);
      setDisciplineHistory([]);
      setAccessForm(null);
      setAccessOptions(null);
      return;
    }

    void (async () => {
      try {
        const [user, options, nextDisciplineHistory] = await Promise.all([
          fetchUser(selectedUserId),
          fetchUserAccessOptions(selectedUserId),
          fetchUserDisciplineHistory(selectedUserId),
        ]);
        setSelectedUser(user);
        setDisciplineHistory(nextDisciplineHistory);
        setAccessForm(toAccessForm(user));
        setAccessOptions(options);
      } catch (error) {
        showError(error);
      }
    })();
  }, [canReadUsers, selectedUserId]);

  useEffect(() => {
    if (!policy) {
      setPolicyForm(null);
      return;
    }
    setPolicyForm(toPolicyForm(policy));
  }, [policy]);

  useEffect(() => {
    if (publicBranches.length === 0) {
      setSelectedPickupBranchId(null);
      return;
    }

    if (selectedPickupBranchId !== null && publicBranches.some((branch) => branch.id === selectedPickupBranchId)) {
      return;
    }

    setSelectedPickupBranchId(profile?.homeBranch?.id ?? profile?.branch?.id ?? publicBranches[0]?.id ?? null);
  }, [profile?.branch?.id, profile?.homeBranch?.id, publicBranches, selectedPickupBranchId]);

  async function loadPublicData() {
    setLoading(true);

    try {
      const [nextBooks, nextDiscovery, nextFilters, nextUpcomingBooks, nextPublicBranches] = await Promise.all([
        fetchBooks(
          deferredQuery,
          categoryFilter === "all" ? undefined : categoryFilter,
          tagFilter === "all" ? undefined : tagFilter,
        ),
        fetchDiscovery(),
        fetchBookFilters(),
        fetchUpcomingBooks(),
        fetchPublicBranches(),
      ]);
      setBooks(nextBooks);
      setDiscovery(nextDiscovery);
      setFilters(nextFilters);
      setUpcomingBooks(nextUpcomingBooks);
      setPublicBranches(nextPublicBranches);
    } catch (error) {
      showError(error);
    } finally {
      setLoading(false);
    }
  }

  function clearPrivateState() {
    setProfile(null);
    setBorrowings([]);
    setAllBorrowings([]);
    setReservations([]);
    setAllReservations([]);
    setFines([]);
    setAllFines([]);
    setActivityLogs([]);
    setAllActivityLogs([]);
    setUsers([]);
    setHoldings([]);
    setSelectedUserId(null);
    setSelectedUser(null);
    setDisciplineHistory([]);
    setAccessForm(null);
    setAccessOptions(null);
    setPolicy(null);
    setBranches([]);
    setPublicBranches([]);
    setLocations([]);
    setNotifications([]);
    setBranchForm(emptyBranchForm);
    setEditingBranchId(null);
    setLocationForm(emptyLocationForm);
    setEditingLocationId(null);
    setHoldingForm(emptyHoldingForm);
    setEditingHoldingId(null);
    setUpcomingBookForm(emptyUpcomingBookForm);
    setEditingUpcomingBookId(null);
    setNotificationForm(emptyNotificationForm);
    setDisciplineRequestForm(emptyDisciplineRequestForm);
    setNotificationsOpen(false);
  }

  async function loadPrivateData() {
    if (!signedIn) {
      clearPrivateState();
      return;
    }

    try {
      const nextProfile = await fetchProfile();
      const nextPermissions = new Set(nextProfile.permissions);

      const nextCanReadPersonalHistory = nextPermissions.has("LOAN_SELF_READ");
      const nextCanReadOperationalBorrowings =
        nextPermissions.has("LOAN_READ_GLOBAL") || nextPermissions.has("REPORT_BRANCH_READ");
      const nextCanReadOperationalActivity =
        nextPermissions.has("AUDIT_GLOBAL_READ") ||
        nextPermissions.has("REPORT_GLOBAL_READ") ||
        (nextProfile.role === "BRANCH_MANAGER" && nextPermissions.has("REPORT_BRANCH_READ"));
      const nextCanReadOwnReservations =
        nextPermissions.has("RESERVATION_SELF_CREATE") || nextPermissions.has("RESERVATION_SELF_CANCEL");
      const nextCanReadOperationalReservations =
        nextPermissions.has("RESERVATION_MANAGE_BRANCH") ||
        nextPermissions.has("LOAN_READ_GLOBAL") ||
        nextPermissions.has("REPORT_GLOBAL_READ");
      const nextCanReadOwnFines = nextPermissions.has("FINE_SELF_READ");
      const nextCanReadOperationalFines =
        nextPermissions.has("FINE_READ_BRANCH") || nextPermissions.has("FINE_READ_GLOBAL");
      const nextCanReadUsers =
        nextPermissions.has("USER_READ_GLOBAL") ||
        nextPermissions.has("USER_MANAGE_GLOBAL") ||
        nextPermissions.has("MEMBER_READ_BRANCH");
      const nextCanReadPolicies =
        nextPermissions.has("POLICY_READ") || nextPermissions.has("POLICY_MANAGE_GLOBAL");
      const nextCanManageBranches = nextPermissions.has("BRANCH_MANAGE_GLOBAL");
      const nextCanManageInventory =
        nextPermissions.has("COPY_CREATE") || nextPermissions.has("COPY_UPDATE") || nextProfile.role === "ADMIN";
      const nextCanReadNotifications = nextProfile.accountStatus === "ACTIVE";
      const nextCanReadBranches = nextProfile.role !== "MEMBER";

      const [
        nextBorrowings,
        nextActivityLogs,
        nextAllBorrowings,
        nextAllActivityLogs,
        nextReservations,
        nextAllReservations,
        nextFines,
        nextAllFines,
        nextUsers,
        nextPolicy,
        nextBranches,
        nextHoldings,
        nextLocations,
        nextNotifications,
      ] = await Promise.all([
        nextCanReadPersonalHistory ? fetchBorrowings() : Promise.resolve([]),
        nextCanReadPersonalHistory ? fetchActivityLogs() : Promise.resolve([]),
        nextCanReadOperationalBorrowings ? fetchAllBorrowings() : Promise.resolve([]),
        nextCanReadOperationalActivity ? fetchAllActivityLogs() : Promise.resolve([]),
        nextCanReadOwnReservations ? fetchReservations() : Promise.resolve([]),
        nextCanReadOperationalReservations ? fetchAllReservations() : Promise.resolve([]),
        nextCanReadOwnFines ? fetchFines() : Promise.resolve([]),
        nextCanReadOperationalFines ? fetchAllFines() : Promise.resolve([]),
        nextCanReadUsers ? fetchUsers() : Promise.resolve([]),
        nextCanReadPolicies ? fetchPolicy() : Promise.resolve(null),
        nextCanReadBranches ? fetchBranches() : Promise.resolve([]),
        nextCanManageInventory ? fetchInventoryHoldings() : Promise.resolve([]),
        nextCanManageInventory ? fetchLocations() : Promise.resolve([]),
        nextCanReadNotifications ? fetchNotifications() : Promise.resolve([]),
      ]);

      setProfile(nextProfile);
      setBorrowings(nextBorrowings);
      setActivityLogs(nextActivityLogs);
      setAllBorrowings(nextAllBorrowings);
      setAllActivityLogs(nextAllActivityLogs);
      setReservations(nextReservations);
      setAllReservations(nextAllReservations);
      setFines(nextFines);
      setAllFines(nextAllFines);
      setUsers(nextUsers);
      setPolicy(nextPolicy);
      setBranches(nextBranches);
      setHoldings(nextHoldings);
      setLocations(nextLocations);
      setNotifications(nextNotifications);
    } catch (error) {
      showError(error);
    }
  }

  function showError(error: unknown) {
    setMessage({
      tone: "error",
      text: error instanceof Error ? error.message : "Unexpected error",
    });
  }

  function showSuccess(text: string) {
    setMessage({ tone: "success", text });
  }

  function resetBookForm() {
    if (bookForm.coverImageUrl?.startsWith("blob:")) {
      URL.revokeObjectURL(bookForm.coverImageUrl);
    }
    setBookForm(emptyBookForm);
    setEditingBookId(null);
    setPendingCoverFile(null);
  }

  function navigateTo(path: string) {
    window.history.pushState({}, "", path);
    setRoute(resolveRoute(path));
  }

  function startEditBook(book: Book) {
    setEditingBookId(book.id);
    setBookForm({
      title: book.title,
      author: book.author,
      category: book.category ?? "",
      isbn: book.isbn ?? "",
      tags: book.tags.join(", "),
      coverImageUrl: book.coverImageUrl,
    });
    setPendingCoverFile(null);
  }

  function updateBookFormField<K extends keyof BookFormState>(field: K, value: BookFormState[K]) {
    setBookForm((current) => ({ ...current, [field]: value }));
  }

  function updateAccessField<K extends keyof AccessFormState>(field: K, value: AccessFormState[K]) {
    setAccessForm((current) => (current ? { ...current, [field]: value } : current));
  }

  function updateBranchFormField<K extends keyof BranchFormState>(field: K, value: BranchFormState[K]) {
    setBranchForm((current) => ({ ...current, [field]: value }));
  }

  function updateLocationFormField<K extends keyof LocationFormState>(field: K, value: LocationFormState[K]) {
    setLocationForm((current) => ({ ...current, [field]: value }));
  }

  function updateHoldingFormField<K extends keyof HoldingFormState>(field: K, value: HoldingFormState[K]) {
    setHoldingForm((current) => ({ ...current, [field]: value }));
  }

  function updateUpcomingBookFormField<K extends keyof UpcomingBookFormState>(
    field: K,
    value: UpcomingBookFormState[K],
  ) {
    setUpcomingBookForm((current) => ({ ...current, [field]: value }));
  }

  function updateNotificationFormField<K extends keyof NotificationFormState>(
    field: K,
    value: NotificationFormState[K],
  ) {
    setNotificationForm((current) => ({ ...current, [field]: value }));
  }

  function updateDisciplineRequestField<K extends keyof DisciplineRequestFormState>(
    field: K,
    value: DisciplineRequestFormState[K],
  ) {
    setDisciplineRequestForm((current) => ({ ...current, [field]: value }));
  }

  function updatePolicyField<K extends keyof PolicyFormState>(field: K, value: PolicyFormState[K]) {
    setPolicyForm((current) => (current ? { ...current, [field]: value } : current));
  }

  function updateBookCover(file: File | null) {
    if (!file) {
      return;
    }

    if (bookForm.coverImageUrl?.startsWith("blob:")) {
      URL.revokeObjectURL(bookForm.coverImageUrl);
    }

    const previewUrl = URL.createObjectURL(file);
    setPendingCoverFile(file);
    setBookForm((current) => ({ ...current, coverImageUrl: previewUrl }));
  }

  async function refreshSelectedUserState() {
    if (!canReadUsers || selectedUserId === null) {
      return;
    }

    const [user, options, nextDisciplineHistory] = await Promise.all([
      fetchUser(selectedUserId),
      fetchUserAccessOptions(selectedUserId),
      fetchUserDisciplineHistory(selectedUserId),
    ]);
    setSelectedUser(user);
    setDisciplineHistory(nextDisciplineHistory);
    setAccessForm(toAccessForm(user));
    setAccessOptions(options);
  }

  async function refreshAfterMutation(successText: string) {
    showSuccess(successText);
    await Promise.all([loadPublicData(), loadPrivateData()]);
    await refreshSelectedUserState();

    if (route.name === "book") {
      const refreshedBook = await fetchBook(route.bookId);
      setSelectedBook(refreshedBook);
    }
  }

  async function onBorrowWithHolding(bookId: number, holdingId: number | null) {
    if (!canBorrowForSelf) {
      showError(new Error("This account cannot borrow books in its current role or status."));
      return;
    }

    try {
      await borrowBook(bookId, holdingId);
      await refreshAfterMutation("Book borrowed successfully.");
    } catch (error) {
      showError(error);
    }
  }

  async function onReserve(bookId: number) {
    try {
      await createReservation(bookId, selectedPickupBranchId);
      await refreshAfterMutation("Reservation created successfully.");
    } catch (error) {
      showError(error);
    }
  }

  async function onCollectReservation(reservationId: number) {
    try {
      await collectReservation(reservationId);
      await refreshAfterMutation("Reservation collected successfully.");
    } catch (error) {
      showError(error);
    }
  }

  async function onReturn(transactionId: number) {
    try {
      await returnBook(transactionId);
      await refreshAfterMutation("Book returned successfully.");
    } catch (error) {
      showError(error);
    }
  }

  async function onRenew(transactionId: number) {
    try {
      await renewBorrowing(transactionId);
      await refreshAfterMutation("Borrowing renewed successfully.");
    } catch (error) {
      showError(error);
    }
  }

  async function onCancelReservation(reservationId: number) {
    try {
      await cancelReservation(reservationId);
      await refreshAfterMutation("Reservation cancelled successfully.");
    } catch (error) {
      showError(error);
    }
  }

  async function onNoShowReservation(reservationId: number) {
    try {
      await markReservationNoShow(reservationId);
      await refreshAfterMutation("Reservation marked as no-show.");
    } catch (error) {
      showError(error);
    }
  }

  async function onPrepareReservation(reservationId: number, holdingId?: number | null) {
    try {
      await prepareReservation(reservationId, holdingId ?? null);
      await refreshAfterMutation("Reservation prepared successfully.");
    } catch (error) {
      showError(error);
    }
  }

  async function onReadyReservation(reservationId: number) {
    try {
      await markReservationReady(reservationId);
      await refreshAfterMutation("Reservation marked ready for pickup.");
    } catch (error) {
      showError(error);
    }
  }

  async function onExpireReservation(reservationId: number) {
    try {
      await expireReservation(reservationId);
      await refreshAfterMutation("Reservation expired successfully.");
    } catch (error) {
      showError(error);
    }
  }

  async function onWaiveFine(fineId: number, note: string) {
    try {
      await waiveFine(fineId, note);
      await refreshAfterMutation("Fine waived successfully.");
    } catch (error) {
      showError(error);
    }
  }

  async function onSaveAccess() {
    if (selectedUserId === null || !accessForm) {
      return;
    }

    try {
      await updateUserAccess(selectedUserId, {
        role: accessForm.role,
        accountStatus: accessForm.accountStatus,
        membershipStatus: accessForm.membershipStatus,
        branchId: parseOptionalNumber(accessForm.branchId),
        homeBranchId: parseOptionalNumber(accessForm.homeBranchId),
      });
      await refreshAfterMutation("Access updated successfully.");
    } catch (error) {
      showError(error);
    }
  }

  async function onApplyUserDiscipline(
    userId: number,
    action: UserDisciplineActionType,
    reason: UserDisciplineReason,
    note: string,
  ) {
    try {
      await applyUserDiscipline(userId, {
        action,
        reason,
        note: note.trim() || undefined,
      });
      const successMessage =
        action === "REINSTATE"
          ? "User reinstated successfully."
          : action === "BAN"
            ? "User banned successfully."
            : "User suspended successfully.";
      await refreshAfterMutation(successMessage);
    } catch (error) {
      showError(error);
    }
  }

  async function onSavePolicy() {
    if (!policyForm) {
      return;
    }

    try {
      await updatePolicy({
        standardLoanDays: policyForm.standardLoanDays,
        renewalDays: policyForm.renewalDays,
        maxRenewals: policyForm.maxRenewals,
        finePerOverdueDay: Number(policyForm.finePerOverdueDay),
        fineWaiverLimit: Number(policyForm.fineWaiverLimit),
        allowRenewalWithActiveReservations: policyForm.allowRenewalWithActiveReservations,
      });
      await refreshAfterMutation("Policy updated successfully.");
    } catch (error) {
      showError(error);
    }
  }

  function startEditBranch(branch: LibraryBranch) {
    setEditingBranchId(branch.id);
    setBranchForm({
      code: branch.code,
      name: branch.name,
      address: branch.address ?? "",
      phone: branch.phone ?? "",
      active: branch.active,
    });
  }

  function resetBranchForm() {
    setEditingBranchId(null);
    setBranchForm(emptyBranchForm);
  }

  function startEditLocation(location: LibraryLocation) {
    setEditingLocationId(location.id);
    setLocationForm(toLocationForm(location));
  }

  function resetLocationForm() {
    setEditingLocationId(null);
    setLocationForm(emptyLocationForm);
  }

  function startEditHolding(holding: BookHolding) {
    setEditingHoldingId(holding.id);
    setHoldingForm(toHoldingForm(holding));
  }

  function resetHoldingForm() {
    setEditingHoldingId(null);
    setHoldingForm(emptyHoldingForm);
  }

  function startEditUpcoming(upcomingBook: UpcomingBook) {
    setEditingUpcomingBookId(upcomingBook.id);
    setUpcomingBookForm(toUpcomingBookForm(upcomingBook));
  }

  function resetUpcomingBookForm() {
    setEditingUpcomingBookId(null);
    setUpcomingBookForm(emptyUpcomingBookForm);
  }

  async function onSubmitBranchForm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const payload = {
      code: branchForm.code,
      name: branchForm.name,
      address: branchForm.address,
      phone: branchForm.phone,
      active: branchForm.active,
    };

    try {
      if (editingBranchId === null) {
        await createBranch(payload);
        resetBranchForm();
        await refreshAfterMutation("Branch created successfully.");
        return;
      }

      await updateBranch(editingBranchId, payload);
      resetBranchForm();
      await refreshAfterMutation("Branch updated successfully.");
    } catch (error) {
      showError(error);
    }
  }

  async function onSubmitLocationForm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const branchId = parseOptionalNumber(locationForm.branchId);
    if (branchId === null) {
      showError(new Error("Select a branch before saving a location."));
      return;
    }

    const payload = {
      branchId,
      code: locationForm.code,
      name: locationForm.name,
      floorLabel: locationForm.floorLabel,
      zoneLabel: locationForm.zoneLabel,
      active: locationForm.active,
    };

    try {
      if (editingLocationId === null) {
        await createLocation(payload);
        resetLocationForm();
        await refreshAfterMutation("Location created successfully.");
        return;
      }

      await updateLocation(editingLocationId, payload);
      resetLocationForm();
      await refreshAfterMutation("Location updated successfully.");
    } catch (error) {
      showError(error);
    }
  }

  async function onSubmitHoldingForm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const bookId = parseOptionalNumber(holdingForm.bookId);
    const branchId = parseOptionalNumber(holdingForm.branchId);
    if (bookId === null || branchId === null) {
      showError(new Error("Select both a title and a branch before saving a holding."));
      return;
    }

    const payload = {
      bookId,
      branchId,
      locationId: parseOptionalNumber(holdingForm.locationId),
      format: holdingForm.format,
      totalQuantity: holdingForm.totalQuantity,
      availableQuantity: holdingForm.availableQuantity,
      accessUrl: parseOptionalString(holdingForm.accessUrl),
      active: holdingForm.active,
    };

    try {
      if (editingHoldingId === null) {
        await createInventoryHolding(payload);
        resetHoldingForm();
        await refreshAfterMutation("Holding created successfully.");
        return;
      }

      await updateInventoryHolding(editingHoldingId, payload);
      resetHoldingForm();
      await refreshAfterMutation("Holding updated successfully.");
    } catch (error) {
      showError(error);
    }
  }

  async function onSubmitUpcomingBookForm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!upcomingBookForm.expectedAt) {
      showError(new Error("Choose an expected arrival date before saving."));
      return;
    }

    const payload = {
      title: upcomingBookForm.title,
      author: upcomingBookForm.author,
      category: upcomingBookForm.category,
      isbn: upcomingBookForm.isbn,
      summary: upcomingBookForm.summary,
      expectedAt: new Date(upcomingBookForm.expectedAt).toISOString(),
      branchId: parseOptionalNumber(upcomingBookForm.branchId),
      tags: parseTagInput(upcomingBookForm.tags),
    };

    try {
      if (editingUpcomingBookId === null) {
        await createUpcomingBook(payload);
        resetUpcomingBookForm();
        await refreshAfterMutation("Upcoming title created successfully.");
        return;
      }

      await updateUpcomingBook(editingUpcomingBookId, payload);
      resetUpcomingBookForm();
      await refreshAfterMutation("Upcoming title updated successfully.");
    } catch (error) {
      showError(error);
    }
  }

  async function onDeleteUpcomingBook(upcomingBook: UpcomingBook) {
    if (!window.confirm(`Remove "${upcomingBook.title}" from the upcoming list?`)) {
      return;
    }

    try {
      await deleteUpcomingBook(upcomingBook.id);
      if (editingUpcomingBookId === upcomingBook.id) {
        resetUpcomingBookForm();
      }
      await refreshAfterMutation("Upcoming title removed successfully.");
    } catch (error) {
      showError(error);
    }
  }

  async function onSendStaffNotification() {
    try {
      await createNotification({
        title: notificationForm.title,
        message: notificationForm.message,
        branchId: parseOptionalNumber(notificationForm.branchId),
        targetRoles: notificationForm.targetRoles,
      });
      setNotificationForm(emptyNotificationForm);
      await refreshAfterMutation("Notification sent successfully.");
    } catch (error) {
      showError(error);
    }
  }

  async function onSubmitDisciplineRequest() {
    try {
      const response = await submitDisciplineRequest({
        targetUsername: disciplineRequestForm.targetUsername.trim(),
        action: disciplineRequestForm.action,
        reason: disciplineRequestForm.reason,
        note: disciplineRequestForm.note.trim() || undefined,
      });
      setDisciplineRequestForm(emptyDisciplineRequestForm);
      await refreshAfterMutation(
        `Request sent for ${response.targetUsername}. Notified: ${response.notifiedRecipients.join(", ")}.`,
      );
    } catch (error) {
      showError(error);
    }
  }

  async function onMarkNotificationAsRead(notificationId: number) {
    try {
      await markNotificationRead(notificationId);
      await loadPrivateData();
    } catch (error) {
      showError(error);
    }
  }

  async function onOpenDigitalAccess(transactionId: number) {
    try {
      const access = await fetchDigitalAccess(transactionId);
      window.open(access.url, "_blank", "noopener,noreferrer");
    } catch (error) {
      showError(error);
    }
  }

  async function onStaffCheckout(
    userId: number,
    bookId: number,
    holdingId?: number | null,
    reservationId?: number | null,
  ) {
    try {
      await staffCheckoutBook({
        userId,
        bookId,
        holdingId: holdingId ?? null,
        reservationId: reservationId ?? null,
      });
      await refreshAfterMutation("Staff checkout completed successfully.");
    } catch (error) {
      showError(error);
    }
  }

  async function onSubmitBookForm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    try {
      const payload = {
        title: bookForm.title,
        author: bookForm.author,
        category: bookForm.category,
        isbn: bookForm.isbn,
        tags: parseTagInput(bookForm.tags),
      };

      if (editingBookId === null) {
        const createdBook = await createBook(payload);
        if (pendingCoverFile) {
          await uploadBookCover(createdBook.id, pendingCoverFile);
        }
        resetBookForm();
        await refreshAfterMutation("Book created successfully.");
        return;
      }

      await updateBook(editingBookId, payload);
      if (pendingCoverFile) {
        await uploadBookCover(editingBookId, pendingCoverFile);
      }
      resetBookForm();
      await refreshAfterMutation("Book updated successfully.");
    } catch (error) {
      showError(error);
    }
  }

  async function onDeleteBook(book: Book) {
    if (!window.confirm(`Delete "${book.title}" from the catalog?`)) {
      return;
    }

    try {
      await deleteBook(book.id);
      if (editingBookId === book.id) {
        resetBookForm();
      }
      await refreshAfterMutation("Book deleted successfully.");
    } catch (error) {
      showError(error);
    }
  }

  if (!ready) {
    return <main className="app-shell loading-shell">Initializing security context...</main>;
  }

  const detailBorrowings = (canReadOperationalBorrowings ? allBorrowings : borrowings).filter(
    (item) => item.bookId === selectedBook?.id,
  );
  const detailLogs = (canReadOperationalActivity ? allActivityLogs : activityLogs)
    .filter((log) => log.bookId === selectedBook?.id)
    .slice(0, 8);

  return (
    <main className="app-shell">
      {message ? <div className={`banner banner-${message.tone}`}>{message.text}</div> : null}

      <NavigationBar
        signedIn={signedIn}
        canAccessOperations={canAccessOperations}
        canViewNotifications={canReadStaffNotifications}
        unreadNotificationCount={notifications.filter((notification) => !notification.readAt).length}
        username={username()}
        roleLabel={roleLabel}
        currentPath={route.name}
        notificationsOpen={notificationsOpen}
        onNavigateHome={() => navigateTo("/")}
        onNavigateBooks={() => navigateTo("/books")}
        onNavigateAccount={() => navigateTo("/me")}
        onNavigateAdmin={() => navigateTo("/admin")}
        onToggleNotifications={() => setNotificationsOpen((current) => !current)}
        onLogin={() => void login()}
        onRegister={() => void register()}
        onLogout={() => void logout()}
      />

      <NotificationTray
        open={notificationsOpen}
        notifications={notifications}
        onMarkRead={(notificationId) => void onMarkNotificationAsRead(notificationId)}
      />

      {route.name === "home" ? (
        <WelcomePage
          signedIn={signedIn}
          canAccessOperations={canAccessOperations}
          username={username()}
          roleLabel={roleLabel}
          inventoryStats={inventoryStats}
          myBorrowingStats={myBorrowingStats}
          recommendations={discovery.recommendations}
          mostBorrowed={discovery.mostBorrowedThisWeek}
          mostViewed={discovery.mostViewedThisWeek}
          upcomingBooks={upcomingBooks}
          onOpenBook={(bookId) => navigateTo(`/books/${bookId}`)}
          onNavigateBooks={() => navigateTo("/books")}
          onNavigateAccount={() => navigateTo("/me")}
          onLogin={() => void login()}
          onRegister={() => void register()}
          onLogout={() => void logout()}
        />
      ) : route.name === "books" ? (
        <BooksWorkspacePage
          loading={loading}
          canBorrow={canBorrowForSelf}
          canReserve={canReserveForSelf}
          canManageCatalog={canManageCatalog}
          query={query}
          categoryFilter={categoryFilter}
          tagFilter={tagFilter}
          categories={filters.categories}
          tags={filters.tags}
          books={books}
          upcomingBooks={upcomingBooks}
          onQueryChange={setQuery}
          onCategoryChange={setCategoryFilter}
          onTagChange={setTagFilter}
          onBorrow={(bookId, holdingId) => void onBorrowWithHolding(bookId, holdingId ?? null)}
          onReserve={(bookId) => void onReserve(bookId)}
          onStartEdit={startEditBook}
          onOpenBook={(bookId) => navigateTo(`/books/${bookId}`)}
        />
      ) : route.name === "account" ? (
        <UserHubPage
          signedIn={signedIn}
          canViewPersonalHistory={canReadPersonalHistory}
          canReturnOwnBorrowings={canReturnOwnBorrowings}
          canRenewOwnBorrowings={canRenewOwnBorrowings}
          canViewReservations={canReadOwnReservations}
          canCancelOwnReservations={canCancelOwnReservations}
          canViewFines={canReadOwnFines}
          profile={profile}
          borrowings={borrowings}
          reservations={reservations}
          fines={fines}
          notifications={notifications}
          logs={activityLogs}
          stats={myBorrowingStats}
          onLogin={() => void login()}
          onRegister={() => void register()}
          onManageAccount={() => void manageAccount()}
          onOpenBook={(bookId) => navigateTo(`/books/${bookId}`)}
          onReturn={(transactionId) => void onReturn(transactionId)}
          onRenew={(transactionId) => void onRenew(transactionId)}
          onOpenDigitalAccess={(transactionId) => void onOpenDigitalAccess(transactionId)}
          onCollectReservation={(reservationId) => void onCollectReservation(reservationId)}
          onCancelReservation={(reservationId) => void onCancelReservation(reservationId)}
          onMarkNotificationRead={(notificationId) => void onMarkNotificationAsRead(notificationId)}
        />
      ) : route.name === "admin" ? (
        canAccessOperations ? (
          <AdminPage
            roleLabel={roleLabel}
            canManageCatalog={canManageCatalog}
            canDeleteCatalog={Boolean(canDeleteCatalog)}
            canManageInventory={canManageInventory}
            canReadNotifications={canReadStaffNotifications}
            canSendNotifications={canSendStaffNotifications}
            canRequestDisciplineReview={canRequestDisciplineReview}
            canSeeBorrowings={canReadOperationalBorrowings}
            canForceReturn={Boolean(canForceReturn)}
            canReadReservations={canReadOperationalReservations}
            canManageReservations={Boolean(canManageOperationalReservations)}
            canReadFines={canReadOperationalFines}
            canWaiveFines={Boolean(canWaiveOperationalFines)}
            canReadUsers={canReadUsers}
            canManageUsers={canManageUsers}
            canReadPolicies={canReadPolicies}
            canManagePolicies={canManagePolicies}
            canManageBranches={Boolean(canManageBranches)}
            editingBookId={editingBookId}
            editingBranchId={editingBranchId}
            editingLocationId={editingLocationId}
            editingHoldingId={editingHoldingId}
            editingUpcomingBookId={editingUpcomingBookId}
            bookForm={bookForm}
            branchForm={branchForm}
            locationForm={locationForm}
            holdingForm={holdingForm}
            upcomingBookForm={upcomingBookForm}
            notificationForm={notificationForm}
            disciplineRequestForm={disciplineRequestForm}
            books={books}
            holdings={holdings}
            borrowings={allBorrowings}
            reservations={allReservations}
            fines={allFines}
            branches={branches}
            locations={locations}
            notifications={notifications}
            unreadNotifications={notifications.filter((notification) => !notification.readAt).length}
            upcomingBooks={upcomingBooks}
            users={users}
            selectedUserId={selectedUserId}
            selectedUser={selectedUser}
            disciplineHistory={disciplineHistory}
            accessOptions={accessOptions}
            accessForm={accessForm}
            policy={policy}
            policyForm={policyForm}
            coverPreviewUrl={bookForm.coverImageUrl}
            onUpdateField={updateBookFormField}
            onCoverSelected={updateBookCover}
            onSubmit={(event) => void onSubmitBookForm(event)}
            onReset={resetBookForm}
            onStartEdit={startEditBook}
            onDelete={(book) => void onDeleteBook(book)}
            onUpdateLocationField={updateLocationFormField}
            onSubmitLocation={(event) => void onSubmitLocationForm(event)}
            onResetLocation={resetLocationForm}
            onStartEditLocation={startEditLocation}
            onUpdateHoldingField={updateHoldingFormField}
            onSubmitHolding={(event) => void onSubmitHoldingForm(event)}
            onResetHolding={resetHoldingForm}
            onStartEditHolding={startEditHolding}
            onUpdateUpcomingField={updateUpcomingBookFormField}
            onSubmitUpcoming={(event) => void onSubmitUpcomingBookForm(event)}
            onResetUpcoming={resetUpcomingBookForm}
            onStartEditUpcoming={startEditUpcoming}
            onDeleteUpcoming={(upcomingBook) => void onDeleteUpcomingBook(upcomingBook)}
            onUpdateNotificationField={updateNotificationFormField}
            onSendNotification={() => void onSendStaffNotification()}
            onUpdateDisciplineRequestField={updateDisciplineRequestField}
            onSubmitDisciplineRequest={() => void onSubmitDisciplineRequest()}
            onMarkNotificationRead={(notificationId) => void onMarkNotificationAsRead(notificationId)}
            onReturn={(transactionId) => void onReturn(transactionId)}
            onStaffCheckout={(userId, bookId, holdingId, reservationId) =>
              void onStaffCheckout(userId, bookId, holdingId ?? null, reservationId ?? null)
            }
            onPrepareReservation={(reservationId, holdingId) => void onPrepareReservation(reservationId, holdingId ?? null)}
            onReadyReservation={(reservationId) => void onReadyReservation(reservationId)}
            onExpireReservation={(reservationId) => void onExpireReservation(reservationId)}
            onNoShowReservation={(reservationId) => void onNoShowReservation(reservationId)}
            onWaiveFine={(fineId, note) => void onWaiveFine(fineId, note)}
            onSelectUser={setSelectedUserId}
            onUpdateAccessField={updateAccessField}
            onSaveAccess={() => void onSaveAccess()}
            onApplyUserDiscipline={(userId, action, reason, note) =>
              void onApplyUserDiscipline(userId, action, reason, note)
            }
            onUpdateBranchField={updateBranchFormField}
            onSubmitBranch={(event) => void onSubmitBranchForm(event)}
            onResetBranch={resetBranchForm}
            onStartEditBranch={startEditBranch}
            onUpdatePolicyField={updatePolicyField}
            onSavePolicy={() => void onSavePolicy()}
          />
        ) : (
          <section className="surface forbidden-page">
            <p className="section-label">Restricted</p>
            <h2>Operations access is required for this page.</h2>
            <p className="hero-text">
              The operations workspace is isolated from the public catalog. Sign in with a staff or
              admin account to manage inventory, access, and circulation operations.
            </p>
          </section>
        )
      ) : selectedBook ? (
        <BookDetailPage
          book={selectedBook}
          canBorrow={canBorrowForSelf}
          canReserve={canReserveForSelf}
          canManageCatalog={canManageCatalog}
          pickupBranches={publicBranches}
          pickupBranchId={selectedPickupBranchId}
          relatedBorrowings={detailBorrowings}
          relatedLogs={detailLogs}
          onBack={() => navigateTo("/books")}
          onBorrow={(bookId, holdingId) => void onBorrowWithHolding(bookId, holdingId ?? null)}
          onReserve={(bookId) => void onReserve(bookId)}
          onPickupBranchChange={setSelectedPickupBranchId}
          onStartEdit={(book) => {
            startEditBook(book);
            navigateTo("/admin");
          }}
        />
      ) : (
        <section className="surface">
          <p className="section-label">Book Detail</p>
          <h2>Loading selected book...</h2>
        </section>
      )}
    </main>
  );
}
