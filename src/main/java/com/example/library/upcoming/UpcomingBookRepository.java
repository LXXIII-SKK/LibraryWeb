package com.example.library.upcoming;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UpcomingBookRepository extends JpaRepository<UpcomingBook, Long> {

    List<UpcomingBook> findAllByOrderByExpectedAtAscTitleAsc();

    List<UpcomingBook> findAllByBranch_IdOrderByExpectedAtAscTitleAsc(Long branchId);
}
