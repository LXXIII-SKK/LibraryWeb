package com.example.library.notification;

import java.time.Instant;

import com.example.library.identity.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "staff_notification_receipt")
public class StaffNotificationReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id")
    private StaffNotification notification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    protected StaffNotificationReceipt() {
    }

    public StaffNotificationReceipt(StaffNotification notification, AppUser user, Instant readAt) {
        this.notification = notification;
        this.user = user;
        this.readAt = readAt;
    }

    public Long getId() {
        return id;
    }

    public StaffNotification getNotification() {
        return notification;
    }

    public AppUser getUser() {
        return user;
    }

    public Instant getReadAt() {
        return readAt;
    }
}
