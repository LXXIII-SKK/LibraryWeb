import { AdminPage } from "../components/AdminPage";
import { BookDetailPage } from "../components/BookDetailPage";
import { BooksWorkspacePage } from "../components/BooksWorkspacePage";
import { NavigationBar } from "../components/NavigationBar";
import { NotificationTray } from "../components/NotificationTray";
import { UpcomingWorkspacePage } from "../components/UpcomingWorkspacePage";
import { UserHubPage } from "../components/UserHubPage";
import { WelcomePage } from "../components/WelcomePage";
import type { ActivityLog, Book, Borrowing, LibraryBranch, StaffNotification } from "../types";
import type {
  AdminConsoleProps,
  BooksWorkspacePageProps,
  Message,
  UpcomingWorkspacePageProps,
  UserHubPageProps,
  WelcomePageProps,
} from "../view-models";
import type { AppPermissions } from "./permissions";
import type { RouteState } from "./routing";

type AppViewProps = {
  ready: boolean;
  message: Message | null;
  signedIn: boolean;
  floatingNavVisible: boolean;
  notificationsOpen: boolean;
  notifications: StaffNotification[];
  unreadNotificationCount: number;
  route: RouteState;
  currentUsername: string;
  permissions: AppPermissions;
  welcomePageProps?: WelcomePageProps;
  booksWorkspaceProps?: BooksWorkspacePageProps;
  upcomingWorkspaceProps?: UpcomingWorkspacePageProps;
  userHubProps?: UserHubPageProps;
  adminPageProps?: AdminConsoleProps;
  selectedBook: Book | null;
  publicBranches: LibraryBranch[];
  selectedPickupBranchId: number | null;
  detailBorrowings: Borrowing[];
  detailLogs: ActivityLog[];
  onNavigateHome: () => void;
  onNavigateBooks: () => void;
  onNavigateUpcoming: () => void;
  onNavigateAccount: () => void;
  onNavigateAdmin: () => void;
  onToggleNotifications: () => void;
  onLogin: () => void;
  onRegister: () => void;
  onLogout: () => void;
  onMarkNotificationRead: (notificationId: number) => void;
  onBorrowWithHolding: (bookId: number, holdingId?: number | null) => void;
  onReserve: (bookId: number, pickupBranchId?: number | null) => void;
  onPickupBranchChange: (branchId: number) => void;
  onStartEditBook: (book: Book) => void;
};

export function AppView({
  ready,
  message,
  signedIn,
  floatingNavVisible,
  notificationsOpen,
  notifications,
  unreadNotificationCount,
  route,
  currentUsername,
  permissions,
  welcomePageProps,
  booksWorkspaceProps,
  upcomingWorkspaceProps,
  userHubProps,
  adminPageProps,
  selectedBook,
  publicBranches,
  selectedPickupBranchId,
  detailBorrowings,
  detailLogs,
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
}: AppViewProps) {
  if (!ready) {
    return <main className="app-shell loading-shell">Initializing security context...</main>;
  }

  return (
    <main className="app-shell">
      {message ? <div className={`banner banner-${message.tone}`}>{message.text}</div> : null}

      <NavigationBar
        visible={floatingNavVisible}
        signedIn={signedIn}
        canAccessOperations={permissions.canAccessOperations}
        canViewNotifications={permissions.canReadStaffNotifications}
        unreadNotificationCount={unreadNotificationCount}
        username={currentUsername}
        roleLabel={permissions.roleLabel}
        currentPath={route.name}
        notificationsOpen={notificationsOpen}
        onNavigateHome={onNavigateHome}
        onNavigateBooks={onNavigateBooks}
        onNavigateUpcoming={onNavigateUpcoming}
        onNavigateAccount={onNavigateAccount}
        onNavigateAdmin={onNavigateAdmin}
        onToggleNotifications={onToggleNotifications}
        onLogin={onLogin}
        onRegister={onRegister}
        onLogout={onLogout}
      />

      <NotificationTray
        open={notificationsOpen}
        notifications={notifications}
        onMarkRead={onMarkNotificationRead}
      />

      {route.name === "home" ? (
        welcomePageProps ? <WelcomePage {...welcomePageProps} /> : null
      ) : route.name === "books" ? (
        booksWorkspaceProps ? <BooksWorkspacePage {...booksWorkspaceProps} /> : null
      ) : route.name === "upcoming" ? (
        upcomingWorkspaceProps ? <UpcomingWorkspacePage {...upcomingWorkspaceProps} /> : null
      ) : route.name === "account" ? (
        userHubProps ? <UserHubPage {...userHubProps} /> : null
      ) : route.name === "admin" ? (
        permissions.canAccessOperations ? (
          adminPageProps ? <AdminPage {...adminPageProps} /> : null
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
          canBorrow={permissions.canBorrowForSelf}
          canReserve={permissions.canReserveForSelf}
          canManageCatalog={permissions.canManageCatalog}
          pickupBranches={publicBranches}
          pickupBranchId={selectedPickupBranchId}
          relatedBorrowings={detailBorrowings}
          relatedLogs={detailLogs}
          onBack={onNavigateBooks}
          onBorrow={onBorrowWithHolding}
          onReserve={onReserve}
          onPickupBranchChange={onPickupBranchChange}
          onStartEdit={(book) => {
            onStartEditBook(book);
            onNavigateAdmin();
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
