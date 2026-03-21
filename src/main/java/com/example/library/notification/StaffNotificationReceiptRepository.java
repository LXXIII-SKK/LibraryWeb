package com.example.library.notification;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffNotificationReceiptRepository extends JpaRepository<StaffNotificationReceipt, Long> {

    List<StaffNotificationReceipt> findAllByUser_Id(Long userId);

    Optional<StaffNotificationReceipt> findByNotification_IdAndUser_Id(Long notificationId, Long userId);
}
