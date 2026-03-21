package com.example.library.circulation;

import java.time.Instant;

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
@Table(name = "borrow_transaction")
public class BorrowTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holding_id")
    private BookHolding holding;

    @Column(name = "borrowed_at", nullable = false)
    private Instant borrowedAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Column(name = "last_renewed_at")
    private Instant lastRenewedAt;

    @Column(name = "renewal_count", nullable = false)
    private int renewalCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BorrowStatus status;

    protected BorrowTransaction() {
    }

    public BorrowTransaction(AppUser user, Book book, Instant borrowedAt, Instant dueAt) {
        this(user, book, null, borrowedAt, dueAt);
    }

    public BorrowTransaction(AppUser user, Book book, BookHolding holding, Instant borrowedAt, Instant dueAt) {
        this.user = user;
        this.book = book;
        this.holding = holding;
        this.borrowedAt = borrowedAt;
        this.dueAt = dueAt;
        this.status = BorrowStatus.BORROWED;
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

    public BookHolding getHolding() {
        return holding;
    }

    public Instant getBorrowedAt() {
        return borrowedAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }

    public Instant getLastRenewedAt() {
        return lastRenewedAt;
    }

    public int getRenewalCount() {
        return renewalCount;
    }

    public BorrowStatus getStatus() {
        return status;
    }

    public void markReturned(Instant returnedAt) {
        this.returnedAt = returnedAt;
        this.status = BorrowStatus.RETURNED;
    }

    public void renewTo(Instant dueAt, Instant renewedAt) {
        if (status == BorrowStatus.RETURNED) {
            throw new IllegalArgumentException("Returned borrowings cannot be renewed");
        }
        this.dueAt = dueAt;
        this.lastRenewedAt = renewedAt;
        this.renewalCount += 1;
    }
}
