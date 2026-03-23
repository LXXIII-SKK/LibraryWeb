package com.example.library.circulation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookTransferRepository extends JpaRepository<BookTransfer, Long> {

    List<BookTransfer> findAllByOrderByRequestedAtDesc();

    List<BookTransfer> findAllBySourceHolding_Branch_IdOrDestinationBranch_IdOrderByRequestedAtDesc(Long sourceBranchId, Long destinationBranchId);
}
