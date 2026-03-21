package com.example.library.branch;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "library_branch")
public class LibraryBranch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 255)
    private String address;

    @Column(length = 50)
    private String phone;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LibraryBranch() {
    }

    private LibraryBranch(Long id) {
        this.id = id;
    }

    public LibraryBranch(String code, String name, String address, String phone, boolean active) {
        this.code = normalizeCode(code);
        this.name = normalizeRequired(name);
        this.address = normalizeNullable(address);
        this.phone = normalizeNullable(phone);
        this.active = active;
    }

    public static LibraryBranch reference(Long id) {
        return id == null ? null : new LibraryBranch(id);
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

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isActive() {
        return active;
    }

    public void updateDetails(String code, String name, String address, String phone, boolean active) {
        this.code = normalizeCode(code);
        this.name = normalizeRequired(name);
        this.address = normalizeNullable(address);
        this.phone = normalizeNullable(phone);
        this.active = active;
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Branch code is required");
        }
        return value.trim().toUpperCase();
    }

    private String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Branch name is required");
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
