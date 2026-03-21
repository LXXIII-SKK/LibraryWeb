package com.example.library.branch;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryBranchRepository extends JpaRepository<LibraryBranch, Long> {

    List<LibraryBranch> findAllByOrderByNameAsc();

    List<LibraryBranch> findAllByActiveTrueOrderByNameAsc();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
