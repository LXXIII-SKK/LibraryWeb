package com.example.library.catalog;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.example.library.inventory.BookHolding;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;

@Entity
@Table(name = "book")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column
    private String category;

    @Column
    private String isbn;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "has_cover_image", nullable = false)
    private boolean hasCoverImage;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final Set<BookTag> tags = new LinkedHashSet<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final Set<BookHolding> holdings = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Book() {
    }

    public Book(String title, String author, String category, String isbn, int totalQuantity) {
        this(title, author, category, isbn, totalQuantity, List.of());
    }

    public Book(String title, String author, String category, String isbn, int totalQuantity, Collection<String> tags) {
        this.title = title;
        this.author = author;
        this.category = category;
        this.isbn = isbn;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = totalQuantity;
        replaceTags(tags);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getCategory() {
        return category;
    }

    public String getIsbn() {
        return isbn;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public boolean hasCoverImage() {
        return hasCoverImage;
    }

    public List<BookHolding> getHoldings() {
        return holdings.stream()
                .sorted(Comparator.comparing(BookHolding::getFormat)
                        .thenComparing(holding -> holding.getBranch() != null ? holding.getBranch().getName() : "")
                        .thenComparing(holding -> holding.getLocation() != null ? holding.getLocation().getName() : "")
                        .thenComparing(BookHolding::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    public void update(String title, String author, String category, String isbn, int totalQuantity) {
        update(title, author, category, isbn, totalQuantity, List.of());
    }

    public void update(String title, String author, String category, String isbn, int totalQuantity, Collection<String> tags) {
        int borrowedCopies = this.totalQuantity - this.availableQuantity;
        if (totalQuantity < borrowedCopies) {
            throw new IllegalArgumentException("Total quantity cannot be less than currently borrowed copies");
        }

        this.title = title;
        this.author = author;
        this.category = category;
        this.isbn = isbn;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = totalQuantity - borrowedCopies;
        replaceTags(tags);
    }

    public void updateMetadata(String title, String author, String category, String isbn, Collection<String> tags) {
        this.title = title;
        this.author = author;
        this.category = category;
        this.isbn = isbn;
        replaceTags(tags);
    }

    public void synchronizeInventory(int totalQuantity, int availableQuantity) {
        if (totalQuantity < 0 || availableQuantity < 0 || availableQuantity > totalQuantity) {
            throw new IllegalArgumentException("Inventory quantities are invalid");
        }

        this.totalQuantity = totalQuantity;
        this.availableQuantity = availableQuantity;
    }

    public void borrowOne() {
        if (availableQuantity <= 0) {
            throw new IllegalArgumentException("Book is out of stock");
        }
        this.availableQuantity -= 1;
    }

    public void returnOne() {
        if (availableQuantity >= totalQuantity) {
            throw new IllegalArgumentException("Book inventory is already full");
        }
        this.availableQuantity += 1;
    }

    public void markCoverImagePresent() {
        this.hasCoverImage = true;
    }

    public List<String> getTags() {
        return tags.stream()
                .map(BookTag::getName)
                .sorted()
                .toList();
    }

    private void replaceTags(Collection<String> tagValues) {
        tags.clear();
        if (tagValues == null) {
            return;
        }

        tagValues.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .sorted()
                .forEach(value -> tags.add(new BookTag(this, value)));
    }
}
