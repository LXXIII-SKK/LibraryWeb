package com.example.library.notification;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffNotificationRepository extends JpaRepository<StaffNotification, Long> {

    List<StaffNotification> findAllByOrderByCreatedAtDesc();
}
