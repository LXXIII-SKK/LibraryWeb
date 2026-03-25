import type { Page, Route } from "@playwright/test";

const branches = [
  { id: 1, code: "CENTRAL", name: "Central Library", active: true, address: "1 Main St", phone: "555-0100" },
  { id: 2, code: "EAST", name: "East Library", active: true, address: "2 East St", phone: "555-0101" },
];

const branchSummaries = branches.map(({ id, code, name, active }) => ({ id, code, name, active }));

const discoveryBook = {
  id: 1,
  title: "Domain-Driven Design",
  author: "Eric Evans",
  category: "Architecture",
  isbn: "9780321125217",
  totalQuantity: 6,
  availableQuantity: 4,
  tags: ["architecture", "ddd"],
  coverImageUrl: null,
  weeklyCount: 3,
  spotlight: "Frequently borrowed by architecture-focused readers.",
};

const books = [
  {
    id: 1,
    title: "Domain-Driven Design",
    author: "Eric Evans",
    category: "Architecture",
    isbn: "9780321125217",
    totalQuantity: 6,
    availableQuantity: 4,
    viewCount: 12,
    tags: ["architecture", "ddd"],
    coverImageUrl: null,
    hasOnlineAccess: false,
    availability: [
      {
        id: 11,
        bookId: 1,
        bookTitle: "Domain-Driven Design",
        format: "PHYSICAL",
        branch: branchSummaries[0],
        location: { id: 21, code: "A-1", name: "Architecture Shelf", floorLabel: "1", zoneLabel: "A", active: true },
        totalQuantity: 6,
        availableQuantity: 4,
        active: true,
        onlineAccess: false,
      },
    ],
  },
  {
    id: 2,
    title: "Designing Data-Intensive Applications",
    author: "Martin Kleppmann",
    category: "Architecture",
    isbn: "9781449373320",
    totalQuantity: 5,
    availableQuantity: 3,
    viewCount: 9,
    tags: ["architecture", "data"],
    coverImageUrl: null,
    hasOnlineAccess: false,
    availability: [],
  },
];

const upcomingBooks = [
  {
    id: 91,
    title: "Staff Engineering",
    author: "Will Larson",
    category: "Leadership",
    isbn: "9781736417914",
    summary: "Incoming leadership title for branch planning.",
    expectedAt: "2026-04-01T08:00:00Z",
    branch: branchSummaries[0],
    tags: ["leadership", "upcoming"],
  },
];

const adminProfile = {
  id: 1,
  username: "admin",
  email: "admin@library.local",
  role: "ADMIN",
  accountStatus: "ACTIVE",
  membershipStatus: "GOOD_STANDING",
  scope: "GLOBAL",
  branchId: null,
  homeBranchId: null,
  branch: null,
  homeBranch: null,
  permissions: [
    "USER_MANAGE_GLOBAL",
    "USER_READ_GLOBAL",
    "BOOK_CREATE",
    "BOOK_UPDATE",
    "COPY_CREATE",
    "COPY_UPDATE",
    "POLICY_READ",
    "POLICY_MANAGE_GLOBAL",
    "BRANCH_MANAGE_GLOBAL",
    "LOAN_READ_GLOBAL",
    "REPORT_GLOBAL_READ",
    "FINE_READ_GLOBAL",
  ],
};

const managedUsers = [
  {
    id: 1,
    username: "admin",
    email: "admin@library.local",
    role: "ADMIN",
    accountStatus: "ACTIVE",
    membershipStatus: "GOOD_STANDING",
    scope: "GLOBAL",
    branchId: null,
    homeBranchId: null,
    branch: null,
    homeBranch: null,
    permissions: adminProfile.permissions,
  },
  {
    id: 6,
    username: "branch.librarian",
    email: "branch.librarian@library.local",
    role: "LIBRARIAN",
    accountStatus: "ACTIVE",
    membershipStatus: "GOOD_STANDING",
    scope: "BRANCH",
    branchId: 1,
    homeBranchId: 1,
    branch: branchSummaries[0],
    homeBranch: branchSummaries[0],
    permissions: ["BOOK_CREATE", "BOOK_UPDATE", "COPY_CREATE", "COPY_UPDATE"],
  },
];

const accessOptions = {
  roles: ["ADMIN", "LIBRARIAN", "BRANCH_MANAGER", "AUDITOR", "MEMBER"],
  accountStatuses: ["ACTIVE", "SUSPENDED", "LOCKED"],
  membershipStatuses: ["GOOD_STANDING"],
  branches: branchSummaries,
  disciplineActions: [],
  disciplineReasons: [],
};

const staffRegistrationOptions = {
  roles: ["LIBRARIAN", "BRANCH_MANAGER", "ADMIN", "AUDITOR"],
  accountStatuses: ["ACTIVE", "SUSPENDED", "LOCKED"],
  membershipStatuses: ["GOOD_STANDING"],
  branches: branchSummaries,
  disciplineActions: [],
  disciplineReasons: [],
};

const policy = {
  standardLoanDays: 14,
  renewalDays: 7,
  maxRenewals: 2,
  finePerOverdueDay: 0.5,
  fineWaiverLimit: 10,
  allowRenewalWithActiveReservations: false,
  updatedAt: "2026-03-23T00:00:00Z",
};

const holdings = [
  {
    id: 11,
    bookId: 1,
    bookTitle: "Domain-Driven Design",
    format: "PHYSICAL",
    branch: branchSummaries[0],
    location: { id: 21, code: "A-1", name: "Architecture Shelf", floorLabel: "1", zoneLabel: "A", active: true },
    totalQuantity: 6,
    availableQuantity: 4,
    trackedCopyCount: 6,
    active: true,
    onlineAccess: false,
  },
];

const locations = [
  {
    id: 21,
    code: "A-1",
    name: "Architecture Shelf",
    floorLabel: "1",
    zoneLabel: "A",
    active: true,
    branch: branchSummaries[0],
  },
];

export async function installGuestAuth(page: Page) {
  await page.addInitScript(() => {
    window.__LIBRARY_E2E_AUTH__ = {
      initAuth: async () => false,
      isAuthenticated: () => false,
      getAccessToken: async () => undefined,
      username: () => "guest",
      hasRole: () => false,
      login: async () => undefined,
      register: async () => undefined,
      logout: async () => undefined,
      manageAccount: async () => undefined,
    };
  });
}

export async function installAdminAuth(page: Page) {
  await page.addInitScript(() => {
    window.__LIBRARY_E2E_AUTH__ = {
      initAuth: async () => true,
      isAuthenticated: () => true,
      getAccessToken: async () => "playwright-token",
      username: () => "admin",
      hasRole: (role: string) => role.toLowerCase() === "admin",
      login: async () => undefined,
      register: async () => undefined,
      logout: async () => undefined,
      manageAccount: async () => undefined,
    };
  });
}

export async function mockPublicApi(page: Page) {
  await page.route("**/api/**", async (route) => {
    await fulfillRoute(route);
  });
}

async function fulfillRoute(route: Route) {
  const url = new URL(route.request().url());
  const { pathname } = url;

  if (pathname === "/api/discovery") {
    return route.fulfill({ json: {
      recommendations: [discoveryBook],
      mostBorrowedThisWeek: [discoveryBook],
      mostViewedThisWeek: [discoveryBook],
    } });
  }
  if (pathname === "/api/books/filters") {
    return route.fulfill({ json: { categories: ["Architecture", "Leadership"], tags: ["architecture", "ddd", "upcoming"] } });
  }
  if (pathname === "/api/books") {
    return route.fulfill({ json: books });
  }
  if (pathname === "/api/books/1") {
    return route.fulfill({ json: books[0] });
  }
  if (pathname === "/api/upcoming-books") {
    return route.fulfill({ json: upcomingBooks });
  }
  if (pathname === "/api/branches/public") {
    return route.fulfill({ json: branches });
  }

  if (pathname === "/api/profile") {
    return route.fulfill({ json: adminProfile });
  }
  if (pathname === "/api/borrowings/me" || pathname === "/api/borrowings") {
    return route.fulfill({ json: [] });
  }
  if (pathname === "/api/activity-logs/me" || pathname === "/api/activity-logs") {
    return route.fulfill({ json: [] });
  }
  if (pathname === "/api/reservations/me" || pathname === "/api/reservations") {
    return route.fulfill({ json: [] });
  }
  if (pathname === "/api/fines/me" || pathname === "/api/fines") {
    return route.fulfill({ json: [] });
  }
  if (pathname === "/api/users") {
    return route.fulfill({ json: managedUsers });
  }
  if (pathname === "/api/users/staff-registration/options") {
    return route.fulfill({ json: staffRegistrationOptions });
  }
  if (pathname === "/api/users/1") {
    return route.fulfill({ json: managedUsers[0] });
  }
  if (pathname === "/api/users/1/options") {
    return route.fulfill({ json: accessOptions });
  }
  if (pathname === "/api/users/1/discipline") {
    return route.fulfill({ json: [] });
  }
  if (pathname === "/api/policies/current") {
    return route.fulfill({ json: policy });
  }
  if (pathname === "/api/branches") {
    return route.fulfill({ json: branches });
  }
  if (pathname === "/api/inventory/holdings") {
    return route.fulfill({ json: holdings });
  }
  if (pathname === "/api/inventory/copies") {
    return route.fulfill({ json: [] });
  }
  if (pathname === "/api/locations") {
    return route.fulfill({ json: locations });
  }
  if (pathname === "/api/notifications") {
    return route.fulfill({ json: [] });
  }
  if (pathname === "/api/transfers") {
    return route.fulfill({ json: [] });
  }
  if (pathname === "/api/books/1/view") {
    return route.fulfill({ json: { bookId: 1, viewCount: 13, counted: true } });
  }

  return route.fulfill({ status: 404, json: { title: "Unhandled route", detail: pathname } });
}
