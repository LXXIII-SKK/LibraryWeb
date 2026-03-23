package com.example.library.inventory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    List<BookCopy> findAllByOrderByHolding_Book_TitleAscBarcodeAsc();

    List<BookCopy> findAllByCurrentBranch_IdOrderByHolding_Book_TitleAscBarcodeAsc(Long branchId);

    List<BookCopy> findAllByHolding_IdOrderByBarcodeAsc(Long holdingId);

    List<BookCopy> findAllByHolding_IdAndStatusInOrderByBarcodeAsc(Long holdingId, Collection<BookCopyStatus> statuses);

    Optional<BookCopy> findFirstByHolding_IdAndStatusOrderByBarcodeAsc(Long holdingId, BookCopyStatus status);

    long countByHolding_Id(Long holdingId);
}
