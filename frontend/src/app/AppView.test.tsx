import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ComponentProps } from "react";
import { describe, expect, it, vi } from "vitest";

import { AppView } from "./AppView";

function createProps(overrides: Partial<ComponentProps<typeof AppView>> = {}): ComponentProps<typeof AppView> {
  const onNavigateHome = vi.fn();
  const onNavigateBooks = vi.fn();
  const onNavigateUpcoming = vi.fn();
  const onNavigateAccount = vi.fn();
  const onNavigateAdmin = vi.fn();
  const onToggleNotifications = vi.fn();
  const onLogin = vi.fn();
  const onRegister = vi.fn();
  const onLogout = vi.fn();
  const onMarkNotificationRead = vi.fn();
  const onBorrowWithHolding = vi.fn();
  const onReserve = vi.fn();
  const onPickupBranchChange = vi.fn();
  const onStartEditBook = vi.fn();

  return {
    ready: true,
    message: null,
    signedIn: true,
    floatingNavVisible: true,
    notificationsOpen: false,
    notifications: [],
    unreadNotificationCount: 0,
    route: { name: "home" },
    currentUsername: "reader",
    permissions: {
      canManageCatalog: false,
      canDeleteCatalog: false,
      canReadPersonalHistory: false,
      canReadOperationalBorrowings: false,
      canReadOperationalActivity: false,
      canReturnOwnBorrowings: false,
      canStaffCheckout: false,
      canForceReturn: false,
      canOverrideBorrowings: false,
      canManageBorrowingExceptions: false,
      canRenewOwnBorrowings: false,
      canReserveForSelf: false,
      canReadOwnReservations: false,
      canCancelOwnReservations: false,
      canReadOperationalReservations: false,
      canManageOperationalReservations: false,
      canReadOwnFines: false,
      canReadOperationalFines: false,
      canWaiveOperationalFines: false,
      canReadUsers: false,
      canManageUsers: false,
      canRegisterStaff: false,
      canReadPolicies: false,
      canManagePolicies: false,
      canManageBranches: false,
      canManageInventory: false,
      canRequestDisciplineReview: false,
      canReadStaffNotifications: true,
      canSendStaffNotifications: false,
      canBorrowForSelf: false,
      canAccessOperations: false,
      roleLabel: "Member",
    },
    welcomePageProps: {
      inventoryStats: { totalTitles: 1, availableCopies: 1, totalCopies: 1, outOfStock: 0 },
      myBorrowingStats: { active: 0, returned: 0 },
      recommendations: [],
      mostBorrowed: [],
      mostViewed: [],
      upcomingBooks: [],
      onOpenBook: vi.fn(),
      onNavigateUpcoming: vi.fn(),
    },
    selectedBook: null,
    publicBranches: [],
    selectedPickupBranchId: null,
    detailBorrowings: [],
    detailLogs: [],
    onNavigateHome,
    onNavigateBooks,
    onNavigateUpcoming,
    onNavigateAccount,
    onNavigateAdmin,
    onToggleNotifications,
    onLogin,
    onRegister,
    onLogout,
    onMarkNotificationRead,
    onBorrowWithHolding,
    onReserve,
    onPickupBranchChange,
    onStartEditBook,
    ...overrides,
  };
}

describe("AppView", () => {
  it("shows the loading shell before app initialization completes", () => {
    render(<AppView {...createProps({ ready: false })} />);

    expect(screen.getByText("Initializing security context...")).toBeInTheDocument();
  });

  it("routes primary navigation clicks and notification actions", async () => {
    const user = userEvent.setup();
    const props = createProps({
      notificationsOpen: true,
      unreadNotificationCount: 1,
      notifications: [
        {
          id: 7,
          title: "Desk notice",
          message: "Pickup shelf updated.",
          branch: null,
          targetUserId: null,
          targetUsername: null,
          targetRoles: ["LIBRARIAN"],
          createdByUsername: "branch.manager",
          createdAt: "2026-03-23T08:00:00Z",
          readAt: null,
        },
      ],
      permissions: {
        ...createProps().permissions,
        canAccessOperations: true,
      },
    });

    render(<AppView {...props} />);

    await user.click(screen.getByRole("button", { name: "Books" }));
    await user.click(screen.getByRole("button", { name: "Mark read" }));

    expect(props.onNavigateBooks).toHaveBeenCalledTimes(1);
    expect(props.onMarkNotificationRead).toHaveBeenCalledWith(7);
  });

  it("shows the restricted operations state when the user lacks access", () => {
    render(<AppView {...createProps({ route: { name: "admin" } })} />);

    expect(screen.getByText("Operations access is required for this page.")).toBeInTheDocument();
  });

  it("routes book-detail editing into the operations workspace", async () => {
    const user = userEvent.setup();
    const book = {
      id: 42,
      title: "Domain-Driven Design",
      author: "Eric Evans",
      category: "Architecture",
      isbn: "9780321125217",
      totalQuantity: 3,
      availableQuantity: 2,
      viewCount: 5,
      tags: ["ddd"],
      coverImageUrl: null,
      hasOnlineAccess: false,
      availability: [],
    } satisfies ComponentProps<typeof AppView>["selectedBook"] & { id: number };
    const props = createProps({
      route: { name: "book", bookId: 42 },
      selectedBook: book,
      publicBranches: [
        {
          id: 1,
          code: "CENTRAL",
          name: "Central Library",
          active: true,
          address: null,
          phone: null,
        },
      ],
      selectedPickupBranchId: 1,
      permissions: {
        ...createProps().permissions,
        canManageCatalog: true,
      },
    });

    render(<AppView {...props} />);

    await user.click(screen.getByRole("button", { name: "Edit in operations workspace" }));

    expect(props.onStartEditBook).toHaveBeenCalledWith(expect.objectContaining({ id: 42 }));
    expect(props.onNavigateAdmin).toHaveBeenCalledTimes(1);
  });
});
