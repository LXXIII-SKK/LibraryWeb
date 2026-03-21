import { formatDateTime } from "../lib/format";
import type { Reservation } from "../types";

type ReservationsPanelProps = {
  title: string;
  label: string;
  reservations: Reservation[];
  canManage: boolean;
  onPrepare?: (reservationId: number) => void;
  onReady?: (reservationId: number) => void;
  onExpire?: (reservationId: number) => void;
  onNoShow?: (reservationId: number) => void;
};

export function ReservationsPanel({
  title,
  label,
  reservations,
  canManage,
  onPrepare,
  onReady,
  onExpire,
  onNoShow,
}: ReservationsPanelProps) {
  return (
    <div className="admin-panel admin-panel-wide">
      <div className="section-heading">
        <div>
          <p className="section-label">{label}</p>
          <h3>{title}</h3>
        </div>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>User</th>
              <th>Book</th>
              <th>Pickup</th>
              <th>Reserved</th>
              <th>Status</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {reservations.map((reservation) => (
              <tr key={reservation.id}>
                <td>{reservation.username}</td>
                <td>{reservation.bookTitle}</td>
                <td>{reservation.pickupBranch?.name ?? "Unassigned"}</td>
                <td>{formatDateTime(reservation.reservedAt)}</td>
                <td>
                  <span className={`pill pill-${reservation.status.toLowerCase()}`}>{reservation.status}</span>
                </td>
                <td className="table-actions">
                  {canManage && reservation.status === "ACTIVE" && onPrepare ? (
                    <button className="button-secondary" onClick={() => onPrepare(reservation.id)}>
                      Prepare
                    </button>
                  ) : null}
                  {canManage && reservation.status === "IN_TRANSIT" && onReady ? (
                    <button className="button-secondary" onClick={() => onReady(reservation.id)}>
                      Mark ready
                    </button>
                  ) : null}
                  {canManage && ["ACTIVE", "IN_TRANSIT", "READY_FOR_PICKUP"].includes(reservation.status) && onExpire ? (
                    <button className="button-secondary" onClick={() => onExpire(reservation.id)}>
                      Expire
                    </button>
                  ) : null}
                  {canManage && onNoShow ? (
                    <button
                      className="button-secondary"
                      onClick={() => onNoShow(reservation.id)}
                      disabled={!["ACTIVE", "READY_FOR_PICKUP"].includes(reservation.status)}
                    >
                      {["ACTIVE", "READY_FOR_PICKUP"].includes(reservation.status) ? "Mark no-show" : "Closed"}
                    </button>
                  ) : null}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {reservations.length === 0 ? <p className="empty-state">No reservations match this scope.</p> : null}
    </div>
  );
}
