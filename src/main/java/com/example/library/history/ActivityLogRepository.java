package com.example.library.history;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findAllByUserIdOrderByOccurredAtDesc(Long userId);

    List<ActivityLog> findAllByUser_Branch_IdOrderByOccurredAtDesc(Long branchId);

    List<ActivityLog> findAllByOrderByOccurredAtDesc();

    boolean existsByUserIdAndBookIdAndActivityType(Long userId, Long bookId, ActivityType activityType);

    long countByBookIdAndActivityType(Long bookId, ActivityType activityType);

    @Query("""
            select log.book.id as bookId, count(log) as weeklyCount
            from ActivityLog log
            where log.activityType = :activityType
              and log.book is not null
              and log.occurredAt > :threshold
            group by log.book.id
            """)
    List<BookViewCount> countByBookSinceAndActivityType(
            @Param("threshold") Instant threshold,
            @Param("activityType") ActivityType activityType);
}
