import { formatDateTime, pluralize } from "../lib/format";
import type { Book, BookHolding, Borrowing, Fine, LibraryBranch, LibraryLocation, Reservation, StaffNotification, UpcomingBook } from "../types";

type AdminDashboardPanelProps = {
  roleLabel: string;
  books: Book[];
  holdings: BookHolding[];
  branches: LibraryBranch[];
  locations: LibraryLocation[];
  borrowings: Borrowing[];
  reservations: Reservation[];
  fines: Fine[];
  notifications: StaffNotification[];
  upcomingBooks: UpcomingBook[];
};

export function AdminDashboardPanel({
  roleLabel,
  books,
  holdings,
  branches,
  locations,
  borrowings,
  reservations,
  fines,
  notifications,
  upcomingBooks,
}: AdminDashboardPanelProps) {
  const activeBorrowings = borrowings.filter((borrowing) => borrowing.status === "BORROWED").length;
  const openReservations = reservations.filter((reservation) => reservation.status === "ACTIVE").length;
  const openFines = fines.filter((fine) => fine.status === "OPEN").length;
  const unreadNotifications = notifications.filter((notification) => !notification.readAt).length;
  const digitalHoldings = holdings.filter((holding) => holding.format === "DIGITAL" && holding.active).length;
  const physicalHoldings = holdings.filter((holding) => holding.format === "PHYSICAL" && holding.active).length;

  return (
    <div className="admin-panel admin-panel-wide">
      <div className="section-heading">
        <div>
          <p className="section-label">Dashboard</p>
          <h3>{roleLabel} oversight at a glance</h3>
        </div>
        <div className="status-chip">{pluralize(unreadNotifications, "unread alert")}</div>
      </div>

      <div className="metric-grid">
        <div className="metric-card">
          <span>Total titles</span>
          <strong>{books.length}</strong>
        </div>
        <div className="metric-card">
          <span>Physical holdings</span>
          <strong>{physicalHoldings}</strong>
        </div>
        <div className="metric-card">
          <span>Digital holdings</span>
          <strong>{digitalHoldings}</strong>
        </div>
        <div className="metric-card">
          <span>Upcoming titles</span>
          <strong>{upcomingBooks.length}</strong>
        </div>
        <div className="metric-card">
          <span>Active borrowings</span>
          <strong>{activeBorrowings}</strong>
        </div>
        <div className="metric-card">
          <span>Open reservations</span>
          <strong>{openReservations}</strong>
        </div>
        <div className="metric-card">
          <span>Open fines</span>
          <strong>{openFines}</strong>
        </div>
        <div className="metric-card">
          <span>Locations managed</span>
          <strong>{locations.length}</strong>
        </div>
      </div>

      <div className="dashboard-grid">
        <div className="list-card dashboard-card">
          <h4>Branches in service</h4>
          <div className="stack-list">
            {branches.slice(0, 5).map((branch) => (
              <article key={branch.id} className="dashboard-line">
                <strong>{branch.name}</strong>
                <small>{branch.active ? "Active" : "Inactive"}</small>
              </article>
            ))}
          </div>
        </div>

        <div className="list-card dashboard-card">
          <h4>Upcoming arrivals</h4>
          <div className="stack-list">
            {upcomingBooks.slice(0, 4).map((book) => (
              <article key={book.id} className="dashboard-line">
                <strong>{book.title}</strong>
                <small>
                  {book.branch?.name ?? "Global"} | arriving {formatDateTime(book.expectedAt)}
                </small>
              </article>
            ))}
          </div>
        </div>

        <div className="list-card dashboard-card">
          <h4>Recent notifications</h4>
          <div className="stack-list">
            {notifications.slice(0, 4).map((notification) => (
              <article key={notification.id} className="dashboard-line">
                <strong>{notification.title}</strong>
                <small>{formatDateTime(notification.createdAt)}</small>
              </article>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
