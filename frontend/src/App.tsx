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
  fetchInventoryCopies,
  fetchInventoryHoldings,
  fetchLocations,
  fetchNotifications,
  fetchFines,
  fetchPolicy,
  fetchProfile,
  fetchPublicBranches,
  fetchUpcomingBooks,
  fetchReservations,
  fetchStaffRegistrationOptions,
  fetchTransfers,
  fetchUserDisciplineHistory,
  fetchUser,
  fetchUserAccessOptions,
  fetchUsers,
  markNotificationRead,
  recordBorrowingException,
  markReservationNoShow,
  markReservationReady,
  prepareReservation,
  recordBookView,
  registerStaff,
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
import { AppView } from "./app/AppView";
import {
  createStaffRegistrationForm,
  emptyBookForm,
  emptyBranchForm,
  emptyDisciplineRequestForm,
  emptyHoldingForm,
  emptyLocationForm,
  emptyNotificationForm,
  emptyUpcomingBookForm,
  parseOptionalNumber,
  parseOptionalString,
  parseTagInput,
  toAccessForm,
  toHoldingForm,
  toLocationForm,
  toPolicyForm,
  toUpcomingBookForm,
} from "./app/forms";
import { deriveAppPermissions } from "./app/permissions";
import { resolveRoute, type RouteState } from "./app/routing";
import { initAuth, login, logout, manageAccount, register, username } from "./auth";
import type {
  AccessOptions,
  ActivityLog,
  Book,
  BookCopy,
  BookHolding,
  BookFilters,
  BookTransfer,
  BorrowingExceptionAction,
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
  StaffRegistrationFormState,
  UpcomingBookFormState,
  DisciplineRequestFormState,
} from "./view-models";

export default function App() {
  const [ready, setReady] = useState(false);
  const [signedIn, setSignedIn] = useState(false);
  const [floatingNavVisible, setFloatingNavVisible] = useState(true);
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
  const [copies, setCopies] = useState<BookCopy[]>([]);
  const [transfers, setTransfers] = useState<BookTransfer[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [selectedUser, setSelectedUser] = useState<UserAccess | null>(null);
  const [disciplineHistory, setDisciplineHistory] = useState<UserDisciplineRecord[]>([]);
  const [accessOptions, setAccessOptions] = useState<AccessOptions | null>(null);
  const [accessForm, setAccessForm] = useState<AccessFormState | null>(null);
  const [staffRegistrationOptions, setStaffRegistrationOptions] = useState<AccessOptions | null>(null);
  const [staffRegistrationForm, setStaffRegistrationForm] = useState<StaffRegistrationFormState | null>(null);
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

  const permissions = useMemo(() => deriveAppPermissions(profile, signedIn), [profile, signedIn]);
  const {
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
    roleLabel,
  } = permissions;

  const inventoryStats = useMemo<InventoryStats>(() => {
    const totalTitles = books.length;
    const availableCopies = books.reduce((sum, book) => sum + book.availableQuantity, 0);
    const totalCopies = books.reduce((sum, book) => sum + book.totalQuantity, 0);
    const outOfStock = books.filter((book) => book.availableQuantity === 0).length;

    return { totalTitles, availableCopies, totalCopies, outOfStock };
  }, [books]);

  const myBorrowingStats = useMemo<BorrowingStats>(() => {
    const active = borrowings.filter((item) => item.status !== "RETURNED").length;
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
    let showTimer: number | undefined;

    const handleScroll = () => {
      if (showTimer !== undefined) {
        window.clearTimeout(showTimer);
      }

      setFloatingNavVisible(false);
      showTimer = window.setTimeout(() => setFloatingNavVisible(true), 220);
    };

    window.addEventListener("scroll", handleScroll, { passive: true });

    return () => {
      if (showTimer !== undefined) {
        window.clearTimeout(showTimer);
      }
      window.removeEventListener("scroll", handleScroll);
    };
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

    let cancelled = false;

    void (async () => {
      try {
        const book = await fetchBook(route.bookId);
        if (cancelled) {
          return;
        }
        setSelectedBook(book);

        if (signedIn && profile && profile.role !== "AUDITOR") {
          const viewRecord = await recordBookView(route.bookId);
          if (cancelled) {
            return;
          }

          setBooks((current) =>
            current.map((entry) => (entry.id === route.bookId ? { ...entry, viewCount: viewRecord.viewCount } : entry)),
          );
          setSelectedBook((current) =>
            current && current.id === route.bookId ? { ...current, viewCount: viewRecord.viewCount } : current,
          );
        }
      } catch (error) {
        if (!cancelled) {
          showError(error);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [route, signedIn, profile?.id, profile?.role]);

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
    setCopies([]);
    setTransfers([]);
    setSelectedUserId(null);
    setSelectedUser(null);
    setDisciplineHistory([]);
    setAccessForm(null);
    setAccessOptions(null);
    setStaffRegistrationForm(null);
    setStaffRegistrationOptions(null);
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
      const nextPermissions = deriveAppPermissions(nextProfile, true);
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
        nextStaffRegistrationOptions,
        nextPolicy,
        nextBranches,
        nextHoldings,
        nextCopies,
        nextLocations,
        nextNotifications,
        nextTransfers,
      ] = await Promise.all([
        nextPermissions.canReadPersonalHistory ? fetchBorrowings() : Promise.resolve([]),
        nextPermissions.canReadPersonalHistory ? fetchActivityLogs() : Promise.resolve([]),
        nextPermissions.canReadOperationalBorrowings ? fetchAllBorrowings() : Promise.resolve([]),
        nextPermissions.canReadOperationalActivity ? fetchAllActivityLogs() : Promise.resolve([]),
        nextPermissions.canReadOwnReservations ? fetchReservations() : Promise.resolve([]),
        nextPermissions.canReadOperationalReservations ? fetchAllReservations() : Promise.resolve([]),
        nextPermissions.canReadOwnFines ? fetchFines() : Promise.resolve([]),
        nextPermissions.canReadOperationalFines ? fetchAllFines() : Promise.resolve([]),
        nextPermissions.canReadUsers ? fetchUsers() : Promise.resolve([]),
        nextPermissions.canRegisterStaff ? fetchStaffRegistrationOptions() : Promise.resolve(null),
        nextPermissions.canReadPolicies ? fetchPolicy() : Promise.resolve(null),
        nextCanReadBranches ? fetchBranches() : Promise.resolve([]),
        nextPermissions.canManageInventory ? fetchInventoryHoldings() : Promise.resolve([]),
        nextPermissions.canManageInventory ? fetchInventoryCopies() : Promise.resolve([]),
        nextPermissions.canManageInventory ? fetchLocations() : Promise.resolve([]),
        nextPermissions.canReadStaffNotifications ? fetchNotifications() : Promise.resolve([]),
        nextPermissions.canReadOperationalReservations ? fetchTransfers() : Promise.resolve([]),
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
      setStaffRegistrationOptions(nextStaffRegistrationOptions);
      setStaffRegistrationForm(
        nextStaffRegistrationOptions ? createStaffRegistrationForm(nextStaffRegistrationOptions) : null,
      );
      setPolicy(nextPolicy);
      setBranches(nextBranches);
      setHoldings(nextHoldings);
      setCopies(nextCopies);
      setLocations(nextLocations);
      setNotifications(nextNotifications);
      setTransfers(nextTransfers);
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

  function updateStaffRegistrationField<K extends keyof StaffRegistrationFormState>(
    field: K,
    value: StaffRegistrationFormState[K],
  ) {
    setStaffRegistrationForm((current) => (current ? { ...current, [field]: value } : current));
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

  async function onReserve(bookId: number, pickupBranchId?: number | null) {
    try {
      await createReservation(bookId, pickupBranchId ?? selectedPickupBranchId);
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

  async function onOverrideBorrowing(transactionId: number, dueAt: string | null, reason: string) {
    try {
      let normalizedDueAt: string | null = null;
      if (dueAt?.trim()) {
        const parsedDueAt = new Date(dueAt);
        if (Number.isNaN(parsedDueAt.getTime())) {
          showError(new Error("Override due date is invalid."));
          return;
        }
        normalizedDueAt = parsedDueAt.toISOString();
      }
      if (normalizedDueAt && Number.isNaN(new Date(normalizedDueAt).getTime())) {
        showError(new Error("Override due date is invalid."));
        return;
      }
      await renewBorrowing(transactionId, normalizedDueAt, reason);
      await refreshAfterMutation("Borrowing override applied successfully.");
    } catch (error) {
      showError(error);
    }
  }

  async function onRecordBorrowingException(
    transactionId: number,
    action: BorrowingExceptionAction,
    note: string,
  ) {
    try {
      await recordBorrowingException(transactionId, action, note);
      await refreshAfterMutation("Borrowing exception recorded successfully.");
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

  async function onRegisterStaff() {
    if (!staffRegistrationForm) {
      return;
    }

    if (!staffRegistrationForm.username.trim() || !staffRegistrationForm.email.trim() || !staffRegistrationForm.password) {
      showError(new Error("Username, email, and password are required for staff registration."));
      return;
    }

    const branchAssignmentRequired = ["LIBRARIAN", "BRANCH_MANAGER"].includes(staffRegistrationForm.role);
    if (branchAssignmentRequired && parseOptionalNumber(staffRegistrationForm.branchId) === null) {
      showError(new Error("Select a branch for librarian and branch manager accounts."));
      return;
    }

    try {
      const registeredUser = await registerStaff({
        username: staffRegistrationForm.username.trim(),
        email: staffRegistrationForm.email.trim(),
        password: staffRegistrationForm.password,
        role: staffRegistrationForm.role,
        accountStatus: staffRegistrationForm.accountStatus,
        branchId: parseOptionalNumber(staffRegistrationForm.branchId),
        homeBranchId: parseOptionalNumber(staffRegistrationForm.homeBranchId),
        requirePasswordChange: staffRegistrationForm.requirePasswordChange,
      });
      setStaffRegistrationForm(
        staffRegistrationOptions ? createStaffRegistrationForm(staffRegistrationOptions) : staffRegistrationForm,
      );
      showSuccess("Staff account registered successfully.");
      await Promise.all([loadPublicData(), loadPrivateData()]);
      setSelectedUserId(registeredUser.id);
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

  const detailBorrowings = (canReadOperationalBorrowings ? allBorrowings : borrowings).filter(
    (item) => item.bookId === selectedBook?.id,
  );
  const detailLogs = (canReadOperationalActivity ? allActivityLogs : activityLogs)
    .filter((log) => log.bookId === selectedBook?.id)
    .slice(0, 8);
  const unreadNotificationCount = notifications.filter((notification) => !notification.readAt).length;
  const currentUsername = username();
  const welcomePageProps = {
    inventoryStats,
    myBorrowingStats,
    recommendations: discovery.recommendations,
    mostBorrowed: discovery.mostBorrowedThisWeek,
    mostViewed: discovery.mostViewedThisWeek,
    upcomingBooks,
    onOpenBook: (bookId: number) => navigateTo(`/books/${bookId}`),
    onNavigateUpcoming: () => navigateTo("/upcoming"),
  };
  const booksWorkspaceProps = {
    loading,
    canBorrow: canBorrowForSelf,
    canReserve: canReserveForSelf,
    canManageCatalog,
    query,
    categoryFilter,
    tagFilter,
    categories: filters.categories,
    tags: filters.tags,
    books,
    onQueryChange: setQuery,
    onCategoryChange: setCategoryFilter,
    onTagChange: setTagFilter,
    onBorrow: (bookId: number, holdingId?: number | null) => void onBorrowWithHolding(bookId, holdingId ?? null),
    onReserve: (bookId: number) => void onReserve(bookId),
    onStartEdit: startEditBook,
    onOpenBook: (bookId: number) => navigateTo(`/books/${bookId}`),
    onNavigateUpcoming: () => navigateTo("/upcoming"),
  };
  const upcomingWorkspaceProps = {
    upcomingBooks,
    onNavigateBooks: () => navigateTo("/books"),
  };
  const userHubProps = {
    signedIn,
    canViewPersonalHistory: canReadPersonalHistory,
    canReturnOwnBorrowings,
    canRenewOwnBorrowings,
    canViewReservations: canReadOwnReservations,
    canCancelOwnReservations,
    canViewFines: canReadOwnFines,
    profile,
    borrowings,
    reservations,
    fines,
    notifications,
    logs: activityLogs,
    stats: myBorrowingStats,
    onLogin: () => void login(),
    onRegister: () => void register(),
    onNavigateBooks: () => navigateTo("/books"),
    onManageAccount: () => void manageAccount(),
    onOpenBook: (bookId: number) => navigateTo(`/books/${bookId}`),
    onReturn: (transactionId: number) => void onReturn(transactionId),
    onRenew: (transactionId: number) => void onRenew(transactionId),
    onOpenDigitalAccess: (transactionId: number) => void onOpenDigitalAccess(transactionId),
    onCollectReservation: (reservationId: number) => void onCollectReservation(reservationId),
    onCancelReservation: (reservationId: number) => void onCancelReservation(reservationId),
    onMarkNotificationRead: (notificationId: number) => void onMarkNotificationAsRead(notificationId),
  };
  const adminPageProps = {
    roleLabel,
    canManageCatalog,
    canDeleteCatalog: Boolean(canDeleteCatalog),
    canManageInventory,
    canReadNotifications: canReadStaffNotifications,
    canSendNotifications: canSendStaffNotifications,
    canRequestDisciplineReview,
    canSeeBorrowings: canReadOperationalBorrowings,
    canStaffCheckout: Boolean(canStaffCheckout),
    canForceReturn: Boolean(canForceReturn),
    canOverrideBorrowings: Boolean(canOverrideBorrowings),
    canManageBorrowingExceptions: Boolean(canManageBorrowingExceptions),
    canReadReservations: canReadOperationalReservations,
    canManageReservations: Boolean(canManageOperationalReservations),
    canReadFines: canReadOperationalFines,
    canWaiveFines: Boolean(canWaiveOperationalFines),
    canReadUsers,
    canManageUsers,
    canRegisterStaff: Boolean(canRegisterStaff),
    canReadPolicies,
    canManagePolicies,
    canManageBranches: Boolean(canManageBranches),
    editingBookId,
    editingBranchId,
    editingLocationId,
    editingHoldingId,
    editingUpcomingBookId,
    bookForm,
    branchForm,
    locationForm,
    holdingForm,
    upcomingBookForm,
    notificationForm,
    disciplineRequestForm,
    books,
    holdings,
    copies,
    borrowings: allBorrowings,
    reservations: allReservations,
    transfers,
    fines: allFines,
    branches,
    locations,
    notifications,
    unreadNotifications: unreadNotificationCount,
    upcomingBooks,
    users,
    selectedUserId,
    selectedUser,
    disciplineHistory,
    accessOptions,
    accessForm,
    staffRegistrationOptions,
    staffRegistrationForm,
    policy,
    policyForm,
    coverPreviewUrl: bookForm.coverImageUrl,
    onUpdateField: updateBookFormField,
    onCoverSelected: updateBookCover,
    onSubmit: (event: FormEvent<HTMLFormElement>) => void onSubmitBookForm(event),
    onReset: resetBookForm,
    onStartEdit: startEditBook,
    onDelete: (book: Book) => void onDeleteBook(book),
    onUpdateLocationField: updateLocationFormField,
    onSubmitLocation: (event: FormEvent<HTMLFormElement>) => void onSubmitLocationForm(event),
    onResetLocation: resetLocationForm,
    onStartEditLocation: startEditLocation,
    onUpdateHoldingField: updateHoldingFormField,
    onSubmitHolding: (event: FormEvent<HTMLFormElement>) => void onSubmitHoldingForm(event),
    onResetHolding: resetHoldingForm,
    onStartEditHolding: startEditHolding,
    onUpdateUpcomingField: updateUpcomingBookFormField,
    onSubmitUpcoming: (event: FormEvent<HTMLFormElement>) => void onSubmitUpcomingBookForm(event),
    onResetUpcoming: resetUpcomingBookForm,
    onStartEditUpcoming: startEditUpcoming,
    onDeleteUpcoming: (upcomingBook: UpcomingBook) => void onDeleteUpcomingBook(upcomingBook),
    onUpdateNotificationField: updateNotificationFormField,
    onSendNotification: () => void onSendStaffNotification(),
    onUpdateDisciplineRequestField: updateDisciplineRequestField,
    onSubmitDisciplineRequest: () => void onSubmitDisciplineRequest(),
    onMarkNotificationRead: (notificationId: number) => void onMarkNotificationAsRead(notificationId),
    onReturn: (transactionId: number) => void onReturn(transactionId),
    onStaffCheckout: (userId: number, bookId: number, holdingId?: number | null, reservationId?: number | null) =>
      void onStaffCheckout(userId, bookId, holdingId ?? null, reservationId ?? null),
    onOverrideBorrowing: (transactionId: number, dueAt: string | null, reason: string) =>
      void onOverrideBorrowing(transactionId, dueAt, reason),
    onRecordBorrowingException: (transactionId: number, action: BorrowingExceptionAction, note: string) =>
      void onRecordBorrowingException(transactionId, action, note),
    onPrepareReservation: (reservationId: number, holdingId?: number | null) =>
      void onPrepareReservation(reservationId, holdingId ?? null),
    onReadyReservation: (reservationId: number) => void onReadyReservation(reservationId),
    onExpireReservation: (reservationId: number) => void onExpireReservation(reservationId),
    onNoShowReservation: (reservationId: number) => void onNoShowReservation(reservationId),
    onWaiveFine: (fineId: number, note: string) => void onWaiveFine(fineId, note),
    onSelectUser: setSelectedUserId,
    onUpdateAccessField: updateAccessField,
    onSaveAccess: () => void onSaveAccess(),
    onUpdateStaffRegistrationField: updateStaffRegistrationField,
    onRegisterStaff: () => void onRegisterStaff(),
    onApplyUserDiscipline: (userId: number, action: UserDisciplineActionType, reason: UserDisciplineReason, note: string) =>
      void onApplyUserDiscipline(userId, action, reason, note),
    onUpdateBranchField: updateBranchFormField,
    onSubmitBranch: (event: FormEvent<HTMLFormElement>) => void onSubmitBranchForm(event),
    onResetBranch: resetBranchForm,
    onStartEditBranch: startEditBranch,
    onUpdatePolicyField: updatePolicyField,
    onSavePolicy: () => void onSavePolicy(),
  };

  return (
    <AppView
      ready={ready}
      message={message}
      signedIn={signedIn}
      floatingNavVisible={floatingNavVisible}
      notificationsOpen={notificationsOpen}
      notifications={notifications}
      unreadNotificationCount={unreadNotificationCount}
      route={route}
      currentUsername={currentUsername}
      permissions={permissions}
      welcomePageProps={welcomePageProps}
      booksWorkspaceProps={booksWorkspaceProps}
      upcomingWorkspaceProps={upcomingWorkspaceProps}
      userHubProps={userHubProps}
      adminPageProps={adminPageProps}
      selectedBook={selectedBook}
      publicBranches={publicBranches}
      selectedPickupBranchId={selectedPickupBranchId}
      detailBorrowings={detailBorrowings}
      detailLogs={detailLogs}
      onNavigateHome={() => navigateTo("/")}
      onNavigateBooks={() => navigateTo("/books")}
      onNavigateUpcoming={() => navigateTo("/upcoming")}
      onNavigateAccount={() => navigateTo("/me")}
      onNavigateAdmin={() => navigateTo("/admin")}
      onToggleNotifications={() => setNotificationsOpen((current) => !current)}
      onLogin={() => void login()}
      onRegister={() => void register()}
      onLogout={() => void logout()}
      onMarkNotificationRead={(notificationId) => void onMarkNotificationAsRead(notificationId)}
      onBorrowWithHolding={(bookId, holdingId) => void onBorrowWithHolding(bookId, holdingId ?? null)}
      onReserve={(bookId, pickupBranchId) => void onReserve(bookId, pickupBranchId ?? null)}
      onPickupBranchChange={setSelectedPickupBranchId}
      onStartEditBook={startEditBook}
    />
  );
}
