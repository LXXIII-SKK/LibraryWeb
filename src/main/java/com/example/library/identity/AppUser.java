package com.example.library.identity;

import java.time.Instant;

import com.example.library.branch.LibraryBranch;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_user_id", nullable = false, unique = true, length = 100)
    private String keycloakUserId;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AppRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 30)
    private AccountStatus accountStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_status", nullable = false, length = 30)
    private MembershipStatus membershipStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private LibraryBranch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_branch_id")
    private LibraryBranch homeBranch;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppUser() {
    }

    public AppUser(String keycloakUserId, String username, String email, AppRole role) {
        this(
                keycloakUserId,
                username,
                email,
                role,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                (Long) null,
                (Long) null);
    }

    public AppUser(
            String keycloakUserId,
            String username,
            String email,
            AppRole role,
            AccountStatus accountStatus,
            MembershipStatus membershipStatus,
            Long branchId,
            Long homeBranchId) {
        this(
                keycloakUserId,
                username,
                email,
                role,
                accountStatus,
                membershipStatus,
                LibraryBranch.reference(branchId),
                LibraryBranch.reference(homeBranchId));
    }

    public AppUser(
            String keycloakUserId,
            String username,
            String email,
            AppRole role,
            AccountStatus accountStatus,
            MembershipStatus membershipStatus,
            LibraryBranch branch,
            LibraryBranch homeBranch) {
        this.keycloakUserId = keycloakUserId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.accountStatus = accountStatus;
        this.membershipStatus = membershipStatus;
        this.branch = branch;
        this.homeBranch = homeBranch;
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

    public String getKeycloakUserId() {
        return keycloakUserId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public AppRole getRole() {
        return role;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public MembershipStatus getMembershipStatus() {
        return membershipStatus;
    }

    public Long getBranchId() {
        return branch != null ? branch.getId() : null;
    }

    public Long getHomeBranchId() {
        return homeBranch != null ? homeBranch.getId() : null;
    }

    public LibraryBranch getBranch() {
        return branch;
    }

    public LibraryBranch getHomeBranch() {
        return homeBranch;
    }

    public void relinkToKeycloakUser(String keycloakUserId) {
        this.keycloakUserId = keycloakUserId;
    }

    public void synchronizeIdentity(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public void updateAccess(
            AppRole role,
            AccountStatus accountStatus,
            MembershipStatus membershipStatus,
            Long branchId,
            Long homeBranchId) {
        updateAccess(
                role,
                accountStatus,
                membershipStatus,
                LibraryBranch.reference(branchId),
                LibraryBranch.reference(homeBranchId));
    }

    public void updateAccess(
            AppRole role,
            AccountStatus accountStatus,
            MembershipStatus membershipStatus,
            LibraryBranch branch,
            LibraryBranch homeBranch) {
        this.role = role;
        this.accountStatus = accountStatus;
        this.membershipStatus = membershipStatus;
        this.branch = branch;
        this.homeBranch = homeBranch;
    }
}
