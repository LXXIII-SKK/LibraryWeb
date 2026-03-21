package com.example.library.inventory;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookHoldingRepository extends JpaRepository<BookHolding, Long> {

    List<BookHolding> findAllByOrderByBook_TitleAscBranch_NameAscIdAsc();

    List<BookHolding> findAllByBranch_IdOrderByBook_TitleAscIdAsc(Long branchId);

    List<BookHolding> findAllByBook_IdOrderByFormatAscBranch_NameAscIdAsc(Long bookId);
}
