import { useEffect, useMemo, useState } from "react";

import { formatDateTime, pluralize } from "../lib/format";
import { validateBookForm } from "../lib/validation";
import type { AdminConsoleProps } from "../view-models";
import { AccessManagementPanel } from "./AccessManagementPanel";
import { AdminDashboardPanel } from "./AdminDashboardPanel";
import { BookCoverArt } from "./BookCoverArt";
import { BookTagChips } from "./BookTagChips";
import { BranchesPanel } from "./BranchesPanel";
import { FinesPanel } from "./FinesPanel";
import { InventoryPanel } from "./InventoryPanel";
import { LocationsPanel } from "./LocationsPanel";
import { NotificationsPanel } from "./NotificationsPanel";
import { PolicyPanel } from "./PolicyPanel";
import { ReservationsPanel } from "./ReservationsPanel";
import { TransfersPanel } from "./TransfersPanel";
import { UpcomingBooksPanel } from "./UpcomingBooksPanel";

type AdminSection =
  | "dashboard"
  | "catalog"
  | "inventory"
  | "upcoming"
  | "notifications"
  | "borrowings"
  | "reservations"
  | "transfers"
  | "fines"
  | "access"
  | "branches"
  | "locations"
  | "policies";

type SectionItem = {
  id: AdminSection;
  label: string;
  count?: number;
};

export function AdminConsole({
  roleLabel,
  canManageCatalog,
  canDeleteCatalog,
  canManageInventory,
  canReadNotifications,
  canSendNotifications,
  canRequestDisciplineReview,
  canSeeBorrowings,
  canStaffCheckout,
  canForceReturn,
  canOverrideBorrowings,
  canManageBorrowingExceptions,
  canReadReservations,
  canManageReservations,
  canReadFines,
  canWaiveFines,
  canReadUsers,
  canManageUsers,
  canRegisterStaff,
  canReadPolicies,
  canManagePolicies,
  canManageBranches,
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
  borrowings,
  reservations,
  transfers,
  fines,
  branches,
  locations,
  notifications,
  unreadNotifications,
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
  coverPreviewUrl,
  onUpdateField,
  onCoverSelected,
  onSubmit,
  onReset,
  onStartEdit,
  onDelete,
  onUpdateLocationField,
  onSubmitLocation,
  onResetLocation,
  onStartEditLocation,
  onUpdateHoldingField,
  onSubmitHolding,
  onResetHolding,
  onStartEditHolding,
  onUpdateUpcomingField,
  onSubmitUpcoming,
  onResetUpcoming,
  onStartEditUpcoming,
  onDeleteUpcoming,
  onUpdateNotificationField,
  onSendNotification,
  onUpdateDisciplineRequestField,
  onSubmitDisciplineRequest,
  onMarkNotificationRead,
  onReturn,
  onStaffCheckout,
  onOverrideBorrowing,
  onRecordBorrowingException,
  onPrepareReservation,
  onReadyReservation,
  onExpireReservation,
  onNoShowReservation,
  onWaiveFine,
  onSelectUser,
  onUpdateAccessField,
  onSaveAccess,
  onUpdateStaffRegistrationField,
  onRegisterStaff,
  onApplyUserDiscipline,
  onUpdateBranchField,
  onSubmitBranch,
  onResetBranch,
  onStartEditBranch,
  onUpdatePolicyField,
  onSavePolicy,
}: AdminConsoleProps) {
  const [touched, setTouched] = useState<Record<string, boolean>>({});
  const [activeSection, setActiveSection] = useState<AdminSection>("dashboard");
  const [checkoutUserId, setCheckoutUserId] = useState("");
  const [checkoutBookId, setCheckoutBookId] = useState("");
  const [checkoutHoldingId, setCheckoutHoldingId] = useState("");
  const [deskBorrowingId, setDeskBorrowingId] = useState("");
  const [deskOverrideDueAt, setDeskOverrideDueAt] = useState("");
  const [deskOverrideReason, setDeskOverrideReason] = useState("");
  const [deskExceptionNote, setDeskExceptionNote] = useState("");
  const errors = useMemo(() => validateBookForm(bookForm), [bookForm]);
  const memberUsers = useMemo(() => users.filter((user) => user.role === "MEMBER"), [users]);
  const checkoutHoldings = useMemo(
    () => holdings.filter((holding) => !checkoutBookId || holding.bookId === Number(checkoutBookId)),
    [checkoutBookId, holdings],
  );
  const activeBorrowings = useMemo(
    () => borrowings.filter((borrowing) => borrowing.status !== "RETURNED"),
    [borrowings],
  );
  const selectedDeskBorrowing = useMemo(
    () => activeBorrowings.find((borrowing) => borrowing.id === Number(deskBorrowingId)) ?? null,
    [activeBorrowings, deskBorrowingId],
  );

  const sections = useMemo<SectionItem[]>(() => {
    const next: SectionItem[] = [{ id: "dashboard", label: "Dashboard" }];

    if (canManageCatalog) {
      next.push({ id: "catalog", label: "Catalog", count: books.length });
      next.push({ id: "upcoming", label: "Upcoming", count: upcomingBooks.length });
    }
    if (canManageInventory) {
      next.push({ id: "inventory", label: "Inventory", count: holdings.length });
      next.push({ id: "locations", label: "Locations", count: locations.length });
    }
    if (canReadNotifications || canSendNotifications) {
      next.push({ id: "notifications", label: "Notifications", count: unreadNotifications });
    }
    if (canSeeBorrowings) {
      next.push({ id: "borrowings", label: "Borrowings", count: borrowings.length });
    }
    if (canReadReservations) {
      next.push({ id: "reservations", label: "Reservations", count: reservations.length });
      next.push({ id: "transfers", label: "Transfers", count: transfers.length });
    }
    if (canReadFines) {
      next.push({ id: "fines", label: "Fines", count: fines.length });
    }
    if (canReadUsers) {
      next.push({ id: "access", label: "Access", count: users.length });
    }
    if (canManageBranches) {
      next.push({ id: "branches", label: "Branches", count: branches.length });
    }
    if (canReadPolicies) {
      next.push({ id: "policies", label: "Policies" });
    }
    return next;
  }, [
    books.length,
    borrowings.length,
    branches.length,
    canManageBranches,
    canManageCatalog,
    canManageInventory,
    canReadFines,
    canReadNotifications,
    canReadPolicies,
    canReadReservations,
    canReadUsers,
    canSeeBorrowings,
    canSendNotifications,
    fines.length,
    holdings.length,
    locations.length,
    reservations.length,
    transfers.length,
    unreadNotifications,
    upcomingBooks.length,
    users.length,
  ]);

  useEffect(() => {
    if (!sections.some((section) => section.id === activeSection)) {
      setActiveSection(sections[0]?.id ?? "dashboard");
    }
  }, [activeSection, sections]);

  useEffect(() => {
    if (deskBorrowingId && !activeBorrowings.some((borrowing) => borrowing.id === Number(deskBorrowingId))) {
      setDeskBorrowingId("");
    }
  }, [activeBorrowings, deskBorrowingId]);

  function touch(field: string) {
    setTouched((current) => ({ ...current, [field]: true }));
  }

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    setTouched({
      title: true,
      author: true,
      category: true,
      isbn: true,
    });

    if (Object.keys(errors).length > 0) {
      event.preventDefault();
      return;
    }

    onSubmit(event);
  }

  function resetDeskWorkflowInputs() {
    setDeskOverrideDueAt("");
    setDeskOverrideReason("");
    setDeskExceptionNote("");
  }

  function renderCatalogSection() {
    return (
      <div className="stack-list">
        <div className="admin-panel">
          <h3>{editingBookId === null ? "Create title" : "Edit title"}</h3>
          <form className="book-form" onSubmit={handleSubmit} noValidate>
            <label className="field">
              <span>Title</span>
              <input
                value={bookForm.title}
                onChange={(event) => onUpdateField("title", event.target.value)}
                onBlur={() => touch("title")}
                required
                maxLength={255}
                aria-invalid={touched.title && errors.title ? "true" : "false"}
              />
              {touched.title && errors.title ? <small className="field-error">{errors.title}</small> : null}
            </label>
            <label className="field">
              <span>Author</span>
              <input
                value={bookForm.author}
                onChange={(event) => onUpdateField("author", event.target.value)}
                onBlur={() => touch("author")}
                required
                maxLength={255}
                aria-invalid={touched.author && errors.author ? "true" : "false"}
              />
              {touched.author && errors.author ? <small className="field-error">{errors.author}</small> : null}
            </label>
            <label className="field">
              <span>Category</span>
              <input
                value={bookForm.category}
                onChange={(event) => onUpdateField("category", event.target.value)}
                onBlur={() => touch("category")}
                maxLength={100}
                aria-invalid={touched.category && errors.category ? "true" : "false"}
              />
              {touched.category && errors.category ? <small className="field-error">{errors.category}</small> : null}
            </label>
            <label className="field">
              <span>Tags</span>
              <input
                value={bookForm.tags}
                onChange={(event) => onUpdateField("tags", event.target.value)}
                placeholder="architecture, distributed-systems, resilience"
              />
            </label>
            <label className="field">
              <span>ISBN</span>
              <input
                value={bookForm.isbn}
                onChange={(event) => onUpdateField("isbn", event.target.value)}
                onBlur={() => touch("isbn")}
                maxLength={50}
                aria-invalid={touched.isbn && errors.isbn ? "true" : "false"}
              />
              {touched.isbn && errors.isbn ? <small className="field-error">{errors.isbn}</small> : null}
            </label>
            <label className="field">
              <span>Cover image</span>
              <input
                type="file"
                accept="image/png,image/jpeg,image/webp,image/gif"
                onChange={(event) => onCoverSelected(event.target.files?.[0] ?? null)}
              />
            </label>
            {coverPreviewUrl ? (
              <BookCoverArt title={bookForm.title || "Untitled"} coverImageUrl={coverPreviewUrl} className="admin-cover-preview" />
            ) : null}
            <div className="form-actions">
              <button type="submit">{editingBookId === null ? "Create book" : "Save changes"}</button>
              {editingBookId !== null ? (
                <button type="button" className="button-secondary" onClick={onReset}>
                  Cancel edit
                </button>
              ) : null}
            </div>
          </form>
        </div>

        <div className="admin-panel admin-panel-wide">
          <h3>Catalog governance</h3>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Cover</th>
                  <th>Title</th>
                  <th>Author</th>
                  <th>Availability</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {books.map((book) => (
                  <tr key={book.id}>
                    <td>
                      <BookCoverArt title={book.title} coverImageUrl={book.coverImageUrl} className="table-cover" />
                    </td>
                    <td>
                      <div className="table-title-cell">
                        <strong>{book.title}</strong>
                        <BookTagChips tags={book.tags} className="tag-strip compact-tags" />
                      </div>
                    </td>
                    <td>{book.author}</td>
                    <td>
                      {book.availableQuantity}/{book.totalQuantity}
                      <small className="table-subcopy">
                        {book.availability.length > 0
                          ? book.availability
                              .map((holding) => holding.location?.name ?? (holding.format === "DIGITAL" ? "Online access" : holding.branch?.name))
                              .filter(Boolean)
                              .slice(0, 2)
                              .join(", ")
                          : "No holdings yet"}
                      </small>
                    </td>
                    <td className="table-actions">
                      <button className="button-secondary" onClick={() => onStartEdit(book)}>
                        Edit
                      </button>
                      {canDeleteCatalog ? (
                        <button className="button-danger" onClick={() => onDelete(book)}>
                          Delete
                        </button>
                      ) : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    );
  }

  function renderBorrowingsSection() {
    return (
      <div className="admin-panel admin-panel-wide">
        <div className="section-heading">
          <div>
            <p className="section-label">Borrowings</p>
            <h3>Operational circulation oversight</h3>
          </div>
          <div className="status-chip">{pluralize(borrowings.length, "transaction")}</div>
        </div>
        {canStaffCheckout ? (
          <div className="command-grid">
            <label className="field">
              <span>Member</span>
              <select value={checkoutUserId} onChange={(event) => setCheckoutUserId(event.target.value)}>
                <option value="">Select member</option>
                {memberUsers.map((user) => (
                  <option key={user.id} value={user.id}>
                    {user.username}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Title</span>
              <select value={checkoutBookId} onChange={(event) => setCheckoutBookId(event.target.value)}>
                <option value="">Select title</option>
                {books.map((book) => (
                  <option key={book.id} value={book.id}>
                    {book.title}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Holding</span>
              <select value={checkoutHoldingId} onChange={(event) => setCheckoutHoldingId(event.target.value)}>
                <option value="">Auto-select when possible</option>
                {checkoutHoldings.map((holding) => (
                  <option key={holding.id} value={holding.id}>
                    {holding.bookTitle} | {holding.branch?.name ?? "Global"} | {holding.location?.name ?? "Online access"}
                  </option>
                ))}
              </select>
            </label>
            <div className="form-actions">
              <button
                type="button"
                onClick={() => {
                  if (!checkoutUserId || !checkoutBookId) {
                    return;
                  }
                  onStaffCheckout(
                    Number(checkoutUserId),
                    Number(checkoutBookId),
                    checkoutHoldingId ? Number(checkoutHoldingId) : null,
                  );
                }}
                disabled={!checkoutUserId || !checkoutBookId}
              >
                Staff checkout
              </button>
            </div>
          </div>
        ) : null}
        {canOverrideBorrowings || canManageBorrowingExceptions ? (
          <div className="command-grid">
            <label className="field">
              <span>Borrowing</span>
              <select value={deskBorrowingId} onChange={(event) => setDeskBorrowingId(event.target.value)}>
                <option value="">Select active borrowing</option>
                {activeBorrowings.map((borrowing) => (
                  <option key={borrowing.id} value={borrowing.id}>
                    {borrowing.username} | {borrowing.bookTitle} | {borrowing.status}
                  </option>
                ))}
              </select>
            </label>
            {canOverrideBorrowings ? (
              <>
                <label className="field">
                  <span>Override due at</span>
                  <input
                    type="datetime-local"
                    value={deskOverrideDueAt}
                    onChange={(event) => setDeskOverrideDueAt(event.target.value)}
                    disabled={!selectedDeskBorrowing || selectedDeskBorrowing.status !== "BORROWED"}
                  />
                </label>
                <label className="field">
                  <span>Override reason</span>
                  <input
                    value={deskOverrideReason}
                    onChange={(event) => setDeskOverrideReason(event.target.value)}
                    placeholder="Reason for manual renewal"
                    disabled={!selectedDeskBorrowing || selectedDeskBorrowing.status !== "BORROWED"}
                  />
                </label>
                <div className="form-actions">
                  <button
                    type="button"
                    onClick={() => {
                      if (!selectedDeskBorrowing || !deskOverrideReason.trim()) {
                        return;
                      }
                      onOverrideBorrowing(
                        selectedDeskBorrowing.id,
                        deskOverrideDueAt.trim() ? deskOverrideDueAt : null,
                        deskOverrideReason.trim(),
                      );
                      setDeskOverrideDueAt("");
                      setDeskOverrideReason("");
                    }}
                    disabled={!selectedDeskBorrowing || selectedDeskBorrowing.status !== "BORROWED" || !deskOverrideReason.trim()}
                  >
                    Override renew
                  </button>
                </div>
              </>
            ) : null}
            {canManageBorrowingExceptions ? (
              <>
                <label className="field">
                  <span>Exception note</span>
                  <input
                    value={deskExceptionNote}
                    onChange={(event) => setDeskExceptionNote(event.target.value)}
                    placeholder="Document the desk action"
                    disabled={!selectedDeskBorrowing || selectedDeskBorrowing.status !== "BORROWED"}
                  />
                </label>
                <div className="form-actions">
                  <button
                    type="button"
                    className="button-secondary"
                    onClick={() => {
                      if (!selectedDeskBorrowing || !deskExceptionNote.trim()) {
                        return;
                      }
                      onRecordBorrowingException(selectedDeskBorrowing.id, "CLAIM_RETURNED", deskExceptionNote.trim());
                      setDeskExceptionNote("");
                    }}
                    disabled={!selectedDeskBorrowing || selectedDeskBorrowing.status !== "BORROWED" || !deskExceptionNote.trim()}
                  >
                    Claim returned
                  </button>
                  <button
                    type="button"
                    className="button-secondary"
                    onClick={() => {
                      if (!selectedDeskBorrowing || !deskExceptionNote.trim()) {
                        return;
                      }
                      onRecordBorrowingException(selectedDeskBorrowing.id, "MARK_LOST", deskExceptionNote.trim());
                      setDeskExceptionNote("");
                    }}
                    disabled={!selectedDeskBorrowing || selectedDeskBorrowing.status !== "BORROWED" || !deskExceptionNote.trim()}
                  >
                    Mark lost
                  </button>
                  <button
                    type="button"
                    className="button-secondary"
                    onClick={() => {
                      if (!selectedDeskBorrowing || !deskExceptionNote.trim()) {
                        return;
                      }
                      onRecordBorrowingException(selectedDeskBorrowing.id, "MARK_DAMAGED", deskExceptionNote.trim());
                      setDeskExceptionNote("");
                    }}
                    disabled={!selectedDeskBorrowing || selectedDeskBorrowing.status !== "BORROWED" || !deskExceptionNote.trim()}
                  >
                    Mark damaged
                  </button>
                </div>
              </>
            ) : null}
            {selectedDeskBorrowing ? (
              <div className="status-chip">
                {selectedDeskBorrowing.username} | {selectedDeskBorrowing.status}
              </div>
            ) : null}
          </div>
        ) : null}
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>User</th>
                <th>Book</th>
                <th>Holding</th>
                <th>Borrowed</th>
                <th>Due</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {borrowings.map((borrowing) => (
                <tr key={borrowing.id}>
                  <td>{borrowing.username}</td>
                  <td>{borrowing.bookTitle}</td>
                  <td>
                    {borrowing.holdingFormat === "DIGITAL"
                      ? borrowing.branchName ?? "Digital collection"
                      : [borrowing.branchName, borrowing.locationName].filter(Boolean).join(" | ") || "Unassigned"}
                  </td>
                  <td>{formatDateTime(borrowing.borrowedAt)}</td>
                  <td>
                    {formatDateTime(borrowing.dueAt)}
                    {borrowing.lastRenewalOverride && borrowing.lastRenewalReason ? (
                      <small className="table-subcopy">Override: {borrowing.lastRenewalReason}</small>
                    ) : null}
                  </td>
                  <td>
                    <span className={`pill pill-${borrowing.status.toLowerCase()}`}>{borrowing.status}</span>
                    {borrowing.exceptionNote ? <small className="table-subcopy">{borrowing.exceptionNote}</small> : null}
                  </td>
                  <td className="table-actions">
                    {canForceReturn ? (
                      <button
                        onClick={() => onReturn(borrowing.id)}
                        disabled={["RETURNED", "LOST", "DAMAGED"].includes(borrowing.status)}
                      >
                        {borrowing.status === "CLAIMED_RETURNED"
                          ? "Confirm returned"
                          : borrowing.status === "RETURNED"
                            ? "Returned"
                            : ["LOST", "DAMAGED"].includes(borrowing.status)
                              ? "Closed"
                              : "Force return"}
                      </button>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    );
  }

  function renderActiveSection() {
    switch (activeSection) {
      case "dashboard":
        return (
          <AdminDashboardPanel
            roleLabel={roleLabel}
            books={books}
            holdings={holdings}
            branches={branches}
            locations={locations}
            borrowings={borrowings}
            reservations={reservations}
            fines={fines}
            notifications={notifications}
            upcomingBooks={upcomingBooks}
          />
        );
      case "catalog":
        return renderCatalogSection();
      case "inventory":
        return (
          <InventoryPanel
            editingHoldingId={editingHoldingId}
            holdingForm={holdingForm}
            books={books}
            branches={branches}
            locations={locations}
            holdings={holdings}
            copies={copies}
            onUpdateField={onUpdateHoldingField}
            onSubmit={onSubmitHolding}
            onReset={onResetHolding}
            onStartEdit={onStartEditHolding}
          />
        );
      case "upcoming":
        return (
          <UpcomingBooksPanel
            editingUpcomingBookId={editingUpcomingBookId}
            upcomingBookForm={upcomingBookForm}
            upcomingBooks={upcomingBooks}
            branches={branches}
            onUpdateField={onUpdateUpcomingField}
            onSubmit={onSubmitUpcoming}
            onReset={onResetUpcoming}
            onStartEdit={onStartEditUpcoming}
            onDelete={onDeleteUpcoming}
          />
        );
      case "notifications":
        return (
          <NotificationsPanel
            notifications={notifications}
            notificationForm={notificationForm}
            disciplineRequestForm={disciplineRequestForm}
            branches={branches}
            canSendNotifications={canSendNotifications}
            canRequestDisciplineReview={canRequestDisciplineReview}
            onUpdateField={onUpdateNotificationField}
            onUpdateDisciplineRequestField={onUpdateDisciplineRequestField}
            onSend={onSendNotification}
            onSubmitDisciplineRequest={onSubmitDisciplineRequest}
            onMarkRead={onMarkNotificationRead}
          />
        );
      case "borrowings":
        return renderBorrowingsSection();
      case "reservations":
        return (
          <ReservationsPanel
            label="Reservations"
            title="Operational holds and no-show handling"
            reservations={reservations}
            canManage={canManageReservations}
            canCheckoutReadyHolds={canStaffCheckout}
            onCheckoutReadyHold={(reservationId, userId, bookId, holdingId) => {
              onStaffCheckout(userId, bookId, holdingId, reservationId);
              resetDeskWorkflowInputs();
            }}
            onPrepare={onPrepareReservation}
            onReady={onReadyReservation}
            onExpire={onExpireReservation}
            onNoShow={onNoShowReservation}
          />
        );
      case "transfers":
        return <TransfersPanel transfers={transfers} />;
      case "fines":
        return (
          <FinesPanel
            label="Fines"
            title="Fine posture and waiver workflow"
            fines={fines}
            canWaive={canWaiveFines}
            onWaive={onWaiveFine}
          />
        );
      case "access":
        return (
          <AccessManagementPanel
            canManageUsers={canManageUsers}
            canRegisterStaff={canRegisterStaff}
            users={users}
            selectedUserId={selectedUserId}
            selectedUser={selectedUser}
            disciplineHistory={disciplineHistory}
            accessOptions={accessOptions}
            accessForm={accessForm}
            staffRegistrationOptions={staffRegistrationOptions}
            staffRegistrationForm={staffRegistrationForm}
            onSelectUser={onSelectUser}
            onUpdateField={onUpdateAccessField}
            onSave={onSaveAccess}
            onUpdateStaffRegistrationField={onUpdateStaffRegistrationField}
            onRegisterStaff={onRegisterStaff}
            onApplyUserDiscipline={onApplyUserDiscipline}
          />
        );
      case "branches":
        return (
          <BranchesPanel
            editingBranchId={editingBranchId}
            branchForm={branchForm}
            branches={branches}
            onUpdateField={onUpdateBranchField}
            onSubmit={onSubmitBranch}
            onReset={onResetBranch}
            onStartEdit={onStartEditBranch}
          />
        );
      case "locations":
        return (
          <LocationsPanel
            editingLocationId={editingLocationId}
            locationForm={locationForm}
            branches={branches}
            locations={locations}
            onUpdateField={onUpdateLocationField}
            onSubmit={onSubmitLocation}
            onReset={onResetLocation}
            onStartEdit={onStartEditLocation}
          />
        );
      case "policies":
        return (
          <PolicyPanel
            canManagePolicies={canManagePolicies}
            policy={policy}
            policyForm={policyForm}
            onUpdateField={onUpdatePolicyField}
            onSave={onSavePolicy}
          />
        );
    }
  }

  return (
    <section className="surface admin-surface">
      <div className="section-heading">
        <div>
          <p className="section-label">Operations Workspace</p>
          <h2>{roleLabel} task workspace</h2>
        </div>
        <div className="status-chip">{pluralize(sections.length, "task section")}</div>
      </div>

      <div className="operations-layout">
        <aside className="operations-sidebar">
          {sections.map((section) => (
            <button
              key={section.id}
              className={activeSection === section.id ? "button-secondary is-active" : "button-secondary"}
              onClick={() => setActiveSection(section.id)}
            >
              {section.label}
              {typeof section.count === "number" ? ` (${section.count})` : ""}
            </button>
          ))}
        </aside>

        <div className="operations-content">{renderActiveSection()}</div>
      </div>
    </section>
  );
}
