package com.example.library.circulation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowTransactionRepository extends JpaRepository<BorrowTransaction, Long> {

    List<BorrowTransaction> findAllByUserIdOrderByBorrowedAtDesc(Long userId);

    List<BorrowTransaction> findAllByUser_Branch_IdOrderByBorrowedAtDesc(Long branchId);

    List<BorrowTransaction> findAllByOrderByBorrowedAtDesc();

    boolean existsByUserIdAndBookIdAndStatus(Long userId, Long bookId, BorrowStatus status);
}
