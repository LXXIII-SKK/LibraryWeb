package com.example.library.notification;

import java.util.List;

import com.example.library.identity.AppRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffNotificationRepository extends JpaRepository<StaffNotification, Long> {

    @Query("""
            select distinct notification
            from StaffNotification notification
            left join notification.targetRoles role
            where notification.targetUser.id = :userId
               or (
                    notification.targetUser is null
                and role = :role
                and (notification.branch is null or notification.branch.id = :branchId)
               )
            order by notification.createdAt desc
            """)
    List<StaffNotification> findVisibleToUserOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("role") AppRole role,
            @Param("branchId") Long branchId);
}
