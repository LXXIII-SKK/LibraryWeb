package com.example.library.circulation;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BorrowTransactionRepository extends JpaRepository<BorrowTransaction, Long> {

    List<BorrowTransaction> findAllByUserIdOrderByBorrowedAtDesc(Long userId);

    List<BorrowTransaction> findAllByUser_Branch_IdOrderByBorrowedAtDesc(Long branchId);

    List<BorrowTransaction> findAllByOrderByBorrowedAtDesc();

    boolean existsByUserIdAndBookIdAndStatus(Long userId, Long bookId, BorrowStatus status);

    @Query("""
            select transaction.book.id as bookId, count(distinct transaction.user.id) as weeklyCount
            from BorrowTransaction transaction
            where transaction.borrowedAt > :threshold
            group by transaction.book.id
            """)
    List<BookBorrowCount> countUniqueBorrowersByBookSince(@Param("threshold") Instant threshold);
}
