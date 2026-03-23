package com.example.library.circulation;

import java.time.Instant;

import com.example.library.branch.LibraryBranch;
import com.example.library.inventory.BookCopy;
import com.example.library.inventory.BookHolding;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "book_transfer")
public class BookTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "copy_id", nullable = false)
    private BookCopy copy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_holding_id", nullable = false)
    private BookHolding sourceHolding;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_branch_id", nullable = false)
    private LibraryBranch destinationBranch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookTransferStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "dispatched_at", nullable = false)
    private Instant dispatchedAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected BookTransfer() {
    }

    public BookTransfer(BookCopy copy, BookHolding sourceHolding, LibraryBranch destinationBranch, Instant dispatchedAt) {
        this.copy = requireCopy(copy);
        this.sourceHolding = requireHolding(sourceHolding);
        this.destinationBranch = requireBranch(destinationBranch);
        this.requestedAt = requireInstant(dispatchedAt);
        this.dispatchedAt = dispatchedAt;
        this.status = BookTransferStatus.IN_TRANSIT;
    }

    public Long getId() {
        return id;
    }

    public BookCopy getCopy() {
        return copy;
    }

    public BookHolding getSourceHolding() {
        return sourceHolding;
    }

    public LibraryBranch getDestinationBranch() {
        return destinationBranch;
    }

    public BookTransferStatus getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getDispatchedAt() {
        return dispatchedAt;
    }

    public Instant getReadyAt() {
        return readyAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void markReadyForPickup(Instant readyAt) {
        if (status != BookTransferStatus.IN_TRANSIT) {
            throw new IllegalArgumentException("Only in-transit transfers can be marked ready");
        }
        this.readyAt = requireInstant(readyAt);
        this.status = BookTransferStatus.READY_FOR_PICKUP;
    }

    public void complete(Instant completedAt) {
        if (status != BookTransferStatus.READY_FOR_PICKUP) {
            throw new IllegalArgumentException("Only ready transfers can be completed");
        }
        this.completedAt = requireInstant(completedAt);
        this.closedAt = completedAt;
        this.status = BookTransferStatus.COMPLETED;
    }

    public void cancel(Instant closedAt) {
        if (status == BookTransferStatus.COMPLETED || status == BookTransferStatus.CANCELLED || status == BookTransferStatus.EXPIRED) {
            throw new IllegalArgumentException("This transfer is already closed");
        }
        this.closedAt = requireInstant(closedAt);
        this.status = BookTransferStatus.CANCELLED;
    }

    public void expire(Instant closedAt) {
        if (status == BookTransferStatus.COMPLETED || status == BookTransferStatus.CANCELLED || status == BookTransferStatus.EXPIRED) {
            throw new IllegalArgumentException("This transfer is already closed");
        }
        this.closedAt = requireInstant(closedAt);
        this.status = BookTransferStatus.EXPIRED;
    }

    private BookCopy requireCopy(BookCopy value) {
        if (value == null) {
            throw new IllegalArgumentException("Transfer copy is required");
        }
        return value;
    }

    private BookHolding requireHolding(BookHolding value) {
        if (value == null) {
            throw new IllegalArgumentException("Transfer source holding is required");
        }
        return value;
    }

    private LibraryBranch requireBranch(LibraryBranch value) {
        if (value == null) {
            throw new IllegalArgumentException("Transfer destination branch is required");
        }
        return value;
    }

    private Instant requireInstant(Instant value) {
        if (value == null) {
            throw new IllegalArgumentException("Transfer timestamp is required");
        }
        return value;
    }
}
