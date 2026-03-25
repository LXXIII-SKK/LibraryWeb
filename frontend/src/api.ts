import type {
  AccessOptions,
  ActivityLog,
  Book,
  BookCopy,
  BookHolding,
  BookFilters,
  BookTransfer,
  BorrowingExceptionAction,
  Borrowing,
  DigitalAccessLink,
  DiscoveryBook,
  DiscoveryResponse,
  Fine,
  LibraryBranch,
  LibraryLocation,
  LibraryPolicy,
  Profile,
  Reservation,
  StaffNotification,
  UpcomingBook,
  UserDisciplineRequestSubmission,
  UserDisciplineRecord,
  UserDisciplineActionType,
  UserDisciplineReason,
  UserAccess,
} from "./types";
import { getAccessToken } from "./auth";

function normalizeBaseUrl(value: string | undefined, fallback: string) {
  if (value === undefined) {
    return fallback;
  }

  const baseUrl = value.trim();
  if (!baseUrl) {
    return "";
  }

  if (/^https?:\/\//.test(baseUrl)) {
    return baseUrl.replace(/\/$/, "");
  }

  if (baseUrl.startsWith("/")) {
    return baseUrl.replace(/\/$/, "");
  }

  return fallback;
}

const apiBaseUrl = normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL, "http://localhost:8080");

function toApiUrl(path: string | null | undefined) {
  if (!path) {
    return null;
  }

  if (/^https?:\/\//.test(path)) {
    return path;
  }

  return `${apiBaseUrl}${path}`;
}

function mapBook<T extends Book>(book: T): T {
  return {
    ...book,
    coverImageUrl: toApiUrl(book.coverImageUrl),
  } as T;
}

function mapDiscovery(response: DiscoveryResponse): DiscoveryResponse {
  return {
    recommendations: response.recommendations.map(mapDiscoveryBook),
    mostBorrowedThisWeek: response.mostBorrowedThisWeek.map(mapDiscoveryBook),
    mostViewedThisWeek: response.mostViewedThisWeek.map(mapDiscoveryBook),
  };
}

function mapDiscoveryBook(book: DiscoveryBook): DiscoveryBook {
  return {
    ...book,
    coverImageUrl: toApiUrl(book.coverImageUrl),
  };
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  return requestWithAuth<T>(path, true, init);
}

async function requestWithAuth<T>(path: string, includeAuth: boolean, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  if (init?.body && !(init.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }

  if (includeAuth) {
    const bearerToken = await getAccessToken();
    if (bearerToken) {
      headers.set("Authorization", `Bearer ${bearerToken}`);
    }
  }

  let response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    headers,
  });

  if (response.status === 401 && includeAuth) {
    const refreshedToken = await getAccessToken(true);
    if (refreshedToken) {
      headers.set("Authorization", `Bearer ${refreshedToken}`);
      response = await fetch(`${apiBaseUrl}${path}`, {
        ...init,
        headers,
      });
    }
  }

  if (!response.ok) {
    const problem = await response.json().catch(() => ({}));
    throw new Error(problem.detail ?? problem.title ?? "Request failed");
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  if (!text) {
    return undefined as T;
  }

  return JSON.parse(text) as T;
}

function buildBooksPath(query = "", category?: string, tag?: string) {
  const search = new URLSearchParams();

  if (query.trim()) {
    search.set("query", query.trim());
  }

  if (category?.trim()) {
    search.set("category", category.trim());
  }

  if (tag?.trim()) {
    search.set("tag", tag.trim());
  }

  return `/api/books${search.size > 0 ? `?${search.toString()}` : ""}`;
}

export function fetchBooks(query = "", category?: string, tag?: string): Promise<Book[]> {
  return requestWithAuth<Book[]>(buildBooksPath(query, category, tag), false).then((books) => books.map(mapBook));
}

export function fetchBookFilters(): Promise<BookFilters> {
  return requestWithAuth<BookFilters>("/api/books/filters", false);
}

export function fetchBook(id: number): Promise<Book> {
  return requestWithAuth<Book>(`/api/books/${id}`, false).then(mapBook);
}

export function fetchDiscovery(): Promise<DiscoveryResponse> {
  return requestWithAuth<DiscoveryResponse>("/api/discovery", false).then(mapDiscovery);
}

type BookViewRecord = {
  bookId: number;
  viewCount: number;
  counted: boolean;
};

export function recordBookView(bookId: number): Promise<BookViewRecord> {
  return request<BookViewRecord>(`/api/books/${bookId}/view`, {
    method: "POST",
  });
}

export function fetchProfile(): Promise<Profile> {
  return request<Profile>("/api/profile");
}

export function fetchPublicBranches(): Promise<LibraryBranch[]> {
  return requestWithAuth<LibraryBranch[]>("/api/branches/public", false);
}

export function fetchBorrowings(): Promise<Borrowing[]> {
  return request<Borrowing[]>("/api/borrowings/me");
}

export function fetchAllBorrowings(): Promise<Borrowing[]> {
  return request<Borrowing[]>("/api/borrowings");
}

export function fetchActivityLogs(): Promise<ActivityLog[]> {
  return request<ActivityLog[]>("/api/activity-logs/me");
}

export function fetchAllActivityLogs(): Promise<ActivityLog[]> {
  return request<ActivityLog[]>("/api/activity-logs");
}

export function borrowBook(bookId: number, holdingId?: number | null): Promise<Borrowing> {
  return request<Borrowing>("/api/borrowings", {
    method: "POST",
    body: JSON.stringify({ bookId, holdingId: holdingId ?? null }),
  });
}

type StaffCheckoutPayload = {
  userId: number;
  bookId: number;
  holdingId?: number | null;
  reservationId?: number | null;
};

export function staffCheckoutBook(payload: StaffCheckoutPayload): Promise<Borrowing> {
  return request<Borrowing>("/api/borrowings/staff-checkout", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function returnBook(transactionId: number): Promise<Borrowing> {
  return request<Borrowing>(`/api/borrowings/${transactionId}/return`, {
    method: "POST",
  });
}

export function renewBorrowing(transactionId: number, dueAt?: string | null, reason?: string): Promise<Borrowing> {
  const payload: Record<string, string> = {};
  if (dueAt) {
    payload.dueAt = dueAt;
  }
  if (reason?.trim()) {
    payload.reason = reason.trim();
  }

  return request<Borrowing>(`/api/borrowings/${transactionId}/renew`, {
    method: "POST",
    body: Object.keys(payload).length > 0 ? JSON.stringify(payload) : undefined,
  });
}

export function recordBorrowingException(
  transactionId: number,
  action: BorrowingExceptionAction,
  note: string,
): Promise<Borrowing> {
  return request<Borrowing>(`/api/borrowings/${transactionId}/exception`, {
    method: "POST",
    body: JSON.stringify({ action, note }),
  });
}

export function fetchDigitalAccess(transactionId: number): Promise<DigitalAccessLink> {
  return request<DigitalAccessLink>(`/api/inventory/digital-access/${transactionId}`);
}

export function fetchReservations(): Promise<Reservation[]> {
  return request<Reservation[]>("/api/reservations/me");
}

export function fetchAllReservations(): Promise<Reservation[]> {
  return request<Reservation[]>("/api/reservations");
}

export function createReservation(bookId: number, pickupBranchId?: number | null): Promise<Reservation> {
  return request<Reservation>("/api/reservations", {
    method: "POST",
    body: JSON.stringify({ bookId, pickupBranchId: pickupBranchId ?? null }),
  });
}

export function cancelReservation(reservationId: number): Promise<Reservation> {
  return request<Reservation>(`/api/reservations/${reservationId}/cancel`, {
    method: "POST",
  });
}

export function markReservationNoShow(reservationId: number): Promise<Reservation> {
  return request<Reservation>(`/api/reservations/${reservationId}/no-show`, {
    method: "POST",
  });
}

export function prepareReservation(reservationId: number, holdingId?: number | null): Promise<Reservation> {
  return request<Reservation>(`/api/reservations/${reservationId}/prepare`, {
    method: "POST",
    body: holdingId ? JSON.stringify({ holdingId }) : undefined,
  });
}

export function markReservationReady(reservationId: number): Promise<Reservation> {
  return request<Reservation>(`/api/reservations/${reservationId}/ready`, {
    method: "POST",
  });
}

export function expireReservation(reservationId: number): Promise<Reservation> {
  return request<Reservation>(`/api/reservations/${reservationId}/expire`, {
    method: "POST",
  });
}

export function collectReservation(reservationId: number): Promise<Borrowing> {
  return request<Borrowing>(`/api/reservations/${reservationId}/collect`, {
    method: "POST",
  });
}

export function fetchFines(): Promise<Fine[]> {
  return request<Fine[]>("/api/fines/me");
}

export function fetchAllFines(): Promise<Fine[]> {
  return request<Fine[]>("/api/fines");
}

export function waiveFine(fineId: number, note: string): Promise<Fine> {
  return request<Fine>(`/api/fines/${fineId}/waive`, {
    method: "POST",
    body: JSON.stringify({ note }),
  });
}

export function fetchUsers(): Promise<UserAccess[]> {
  return request<UserAccess[]>("/api/users");
}

export function fetchUser(userId: number): Promise<UserAccess> {
  return request<UserAccess>(`/api/users/${userId}`);
}

export function fetchUserAccessOptions(userId: number): Promise<AccessOptions> {
  return request<AccessOptions>(`/api/users/${userId}/options`);
}

export function fetchStaffRegistrationOptions(): Promise<AccessOptions> {
  return request<AccessOptions>("/api/users/staff-registration/options");
}

export function fetchUserDisciplineHistory(userId: number): Promise<UserDisciplineRecord[]> {
  return request<UserDisciplineRecord[]>(`/api/users/${userId}/discipline`);
}

type UserAccessPayload = {
  role: string;
  accountStatus: string;
  membershipStatus: string;
  branchId: number | null;
  homeBranchId: number | null;
};

export function updateUserAccess(userId: number, payload: UserAccessPayload): Promise<UserAccess> {
  return request<UserAccess>(`/api/users/${userId}/access`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

type StaffRegistrationPayload = {
  username: string;
  email: string;
  password: string;
  role: string;
  accountStatus: string;
  branchId: number | null;
  homeBranchId: number | null;
  requirePasswordChange: boolean;
};

export function registerStaff(payload: StaffRegistrationPayload): Promise<UserAccess> {
  return request<UserAccess>("/api/users/staff-registration", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

type UserDisciplinePayload = {
  action: UserDisciplineActionType;
  reason: UserDisciplineReason;
  note?: string;
};

export function applyUserDiscipline(userId: number, payload: UserDisciplinePayload): Promise<UserDisciplineRecord> {
  return request<UserDisciplineRecord>(`/api/users/${userId}/discipline`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

type BranchPayload = {
  code: string;
  name: string;
  address?: string;
  phone?: string;
  active: boolean;
};

export function fetchBranches(): Promise<LibraryBranch[]> {
  return request<LibraryBranch[]>("/api/branches");
}

export function createBranch(payload: BranchPayload): Promise<LibraryBranch> {
  return request<LibraryBranch>("/api/branches", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateBranch(branchId: number, payload: BranchPayload): Promise<LibraryBranch> {
  return request<LibraryBranch>(`/api/branches/${branchId}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

type LocationPayload = {
  branchId: number;
  code: string;
  name: string;
  floorLabel?: string;
  zoneLabel?: string;
  active: boolean;
};

export function fetchLocations(): Promise<LibraryLocation[]> {
  return request<LibraryLocation[]>("/api/locations");
}

export function createLocation(payload: LocationPayload): Promise<LibraryLocation> {
  return request<LibraryLocation>("/api/locations", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateLocation(locationId: number, payload: LocationPayload): Promise<LibraryLocation> {
  return request<LibraryLocation>(`/api/locations/${locationId}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function fetchPolicy(): Promise<LibraryPolicy> {
  return request<LibraryPolicy>("/api/policies/current");
}

type PolicyPayload = {
  standardLoanDays: number;
  renewalDays: number;
  maxRenewals: number;
  finePerOverdueDay: number;
  fineWaiverLimit: number;
  allowRenewalWithActiveReservations: boolean;
};

export function updatePolicy(payload: PolicyPayload): Promise<LibraryPolicy> {
  return request<LibraryPolicy>("/api/policies/current", {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

type BookPayload = {
  title: string;
  author: string;
  category?: string;
  isbn?: string;
  tags?: string[];
};

export function createBook(payload: BookPayload): Promise<Book> {
  return request<Book>("/api/books", {
    method: "POST",
    body: JSON.stringify(payload),
  }).then(mapBook);
}

export function updateBook(id: number, payload: BookPayload): Promise<Book> {
  return request<Book>(`/api/books/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  }).then(mapBook);
}

export function deleteBook(id: number): Promise<void> {
  return request<void>(`/api/books/${id}`, {
    method: "DELETE",
  });
}

export function uploadBookCover(id: number, file: File): Promise<Book> {
  const formData = new FormData();
  formData.set("file", file);

  return request<Book>(`/api/books/${id}/cover`, {
    method: "POST",
    body: formData,
  }).then(mapBook);
}

type HoldingPayload = {
  bookId: number;
  branchId: number;
  locationId: number | null;
  format: "PHYSICAL" | "DIGITAL";
  totalQuantity: number;
  availableQuantity: number;
  accessUrl?: string;
  active: boolean;
};

export function fetchInventoryHoldings(): Promise<BookHolding[]> {
  return request<BookHolding[]>("/api/inventory/holdings");
}

export function fetchInventoryCopies(): Promise<BookCopy[]> {
  return request<BookCopy[]>("/api/inventory/copies");
}

export function createInventoryHolding(payload: HoldingPayload): Promise<BookHolding> {
  return request<BookHolding>("/api/inventory/holdings", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateInventoryHolding(holdingId: number, payload: HoldingPayload): Promise<BookHolding> {
  return request<BookHolding>(`/api/inventory/holdings/${holdingId}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

type UpcomingBookPayload = {
  title: string;
  author: string;
  category?: string;
  isbn?: string;
  summary?: string;
  expectedAt: string;
  branchId: number | null;
  tags?: string[];
};

export function fetchUpcomingBooks(): Promise<UpcomingBook[]> {
  return requestWithAuth<UpcomingBook[]>("/api/upcoming-books", false);
}

export function fetchTransfers(): Promise<BookTransfer[]> {
  return request<BookTransfer[]>("/api/transfers");
}

export function createUpcomingBook(payload: UpcomingBookPayload): Promise<UpcomingBook> {
  return request<UpcomingBook>("/api/upcoming-books", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateUpcomingBook(upcomingBookId: number, payload: UpcomingBookPayload): Promise<UpcomingBook> {
  return request<UpcomingBook>(`/api/upcoming-books/${upcomingBookId}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function deleteUpcomingBook(upcomingBookId: number): Promise<void> {
  return request<void>(`/api/upcoming-books/${upcomingBookId}`, {
    method: "DELETE",
  });
}

type StaffNotificationPayload = {
  title: string;
  message: string;
  branchId: number | null;
  targetRoles: string[];
};

export function fetchNotifications(): Promise<StaffNotification[]> {
  return request<StaffNotification[]>("/api/notifications");
}

export function createNotification(payload: StaffNotificationPayload): Promise<StaffNotification> {
  return request<StaffNotification>("/api/notifications", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

type DisciplineRequestNotificationPayload = {
  targetUsername: string;
  action: UserDisciplineActionType;
  reason: UserDisciplineReason;
  note?: string;
};

export function submitDisciplineRequest(
  payload: DisciplineRequestNotificationPayload,
): Promise<UserDisciplineRequestSubmission> {
  return request<UserDisciplineRequestSubmission>("/api/notifications/discipline-requests", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function markNotificationRead(notificationId: number): Promise<StaffNotification> {
  return request<StaffNotification>(`/api/notifications/${notificationId}/read`, {
    method: "POST",
  });
}
