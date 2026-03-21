package com.example.library.inventory;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryLocationRepository extends JpaRepository<LibraryLocation, Long> {

    List<LibraryLocation> findAllByOrderByBranch_NameAscNameAsc();

    List<LibraryLocation> findAllByBranch_IdOrderByNameAsc(Long branchId);

    boolean existsByBranch_IdAndCodeIgnoreCase(Long branchId, String code);

    boolean existsByBranch_IdAndCodeIgnoreCaseAndIdNot(Long branchId, String code, Long id);
}
