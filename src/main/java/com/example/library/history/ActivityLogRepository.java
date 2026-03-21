package com.example.library.history;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findAllByUserIdOrderByOccurredAtDesc(Long userId);

    List<ActivityLog> findAllByUser_Branch_IdOrderByOccurredAtDesc(Long branchId);

    List<ActivityLog> findAllByOrderByOccurredAtDesc();
}
