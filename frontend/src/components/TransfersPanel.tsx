import { formatDateTime } from "../lib/format";
import type { BookTransfer } from "../types";

type TransfersPanelProps = {
  transfers: BookTransfer[];
};

export function TransfersPanel({ transfers }: TransfersPanelProps) {
  return (
    <div className="admin-panel admin-panel-wide">
      <div className="section-heading">
        <div>
          <p className="section-label">Transfers</p>
          <h3>Inter-branch movement and ready-hold routing</h3>
        </div>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Copy</th>
              <th>Book</th>
              <th>From</th>
              <th>To</th>
              <th>Status</th>
              <th>Requested</th>
              <th>Ready</th>
            </tr>
          </thead>
          <tbody>
            {transfers.map((transfer) => (
              <tr key={transfer.id}>
                <td>{transfer.copyBarcode}</td>
                <td>{transfer.bookTitle}</td>
                <td>
                  {[transfer.sourceBranch?.name, transfer.sourceLocation?.name].filter(Boolean).join(" | ") || "Unknown"}
                </td>
                <td>{transfer.destinationBranch?.name ?? "Unknown"}</td>
                <td>
                  <span className={`pill pill-${transfer.status.toLowerCase()}`}>{transfer.status}</span>
                </td>
                <td>{formatDateTime(transfer.requestedAt)}</td>
                <td>{transfer.readyAt ? formatDateTime(transfer.readyAt) : "Pending"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {transfers.length === 0 ? <p className="empty-state">No transfer records are visible in this scope.</p> : null}
    </div>
  );
}
