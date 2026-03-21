package com.example.library.circulation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FineRecordRepository extends JpaRepository<FineRecord, Long> {

    List<FineRecord> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<FineRecord> findAllByUser_Branch_IdOrderByCreatedAtDesc(Long branchId);

    List<FineRecord> findAllByOrderByCreatedAtDesc();

    boolean existsByBorrowTransactionId(Long borrowTransactionId);
}
