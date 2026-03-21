package com.example.library.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

interface BookCoverRepository extends JpaRepository<BookCover, Long> {
}
