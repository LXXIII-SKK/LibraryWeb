package com.example.library.circulation;

import java.time.Instant;

import com.example.library.branch.LibraryBranch;
import com.example.library.catalog.Book;
import com.example.library.identity.AppUser;
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
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_branch_id")
    private LibraryBranch pickupBranch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserved_holding_id")
    private BookHolding reservedHolding;

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;

    @Column(name = "transfer_requested_at")
    private Instant transferRequestedAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatus status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Reservation() {
    }

    public Reservation(AppUser user, Book book, Instant reservedAt) {
        this(user, book, null, reservedAt);
    }

    public Reservation(AppUser user, Book book, LibraryBranch pickupBranch, Instant reservedAt) {
        this.user = user;
        this.book = book;
        this.pickupBranch = pickupBranch;
        this.reservedAt = reservedAt;
        this.updatedAt = reservedAt;
        this.status = ReservationStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public Book getBook() {
        return book;
    }

    public Instant getReservedAt() {
        return reservedAt;
    }

    public LibraryBranch getPickupBranch() {
        return pickupBranch;
    }

    public BookHolding getReservedHolding() {
        return reservedHolding;
    }

    public Instant getTransferRequestedAt() {
        return transferRequestedAt;
    }

    public Instant getReadyAt() {
        return readyAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void cancel(Instant updatedAt) {
        clearFulfillmentTimestamps();
        this.status = ReservationStatus.CANCELLED;
        this.updatedAt = updatedAt;
    }

    public void markNoShow(Instant updatedAt) {
        clearFulfillmentTimestamps();
        this.status = ReservationStatus.NO_SHOW;
        this.updatedAt = updatedAt;
    }

    public void fulfill(Instant updatedAt) {
        clearFulfillmentTimestamps();
        this.status = ReservationStatus.FULFILLED;
        this.updatedAt = updatedAt;
    }

    public void beginTransfer(BookHolding reservedHolding, Instant updatedAt) {
        this.reservedHolding = requireHolding(reservedHolding);
        this.transferRequestedAt = updatedAt;
        this.readyAt = null;
        this.expiresAt = null;
        this.status = ReservationStatus.IN_TRANSIT;
        this.updatedAt = updatedAt;
    }

    public void markReadyForPickup(BookHolding reservedHolding, Instant readyAt, Instant expiresAt) {
        this.reservedHolding = requireHolding(reservedHolding);
        this.transferRequestedAt = this.transferRequestedAt == null ? readyAt : this.transferRequestedAt;
        this.readyAt = readyAt;
        this.expiresAt = expiresAt;
        this.status = ReservationStatus.READY_FOR_PICKUP;
        this.updatedAt = readyAt;
    }

    public void expire(Instant updatedAt) {
        clearFulfillmentTimestamps();
        this.status = ReservationStatus.EXPIRED;
        this.updatedAt = updatedAt;
    }

    private BookHolding requireHolding(BookHolding holding) {
        if (holding == null) {
            throw new IllegalArgumentException("A fulfillment holding is required");
        }
        return holding;
    }

    private void clearFulfillmentTimestamps() {
        this.transferRequestedAt = null;
        this.readyAt = null;
        this.expiresAt = null;
    }
}
