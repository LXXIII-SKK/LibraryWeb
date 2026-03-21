package com.example.library.upcoming;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "upcoming_book_tag")
public class UpcomingBookTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "upcoming_book_id")
    private UpcomingBook upcomingBook;

    @Column(nullable = false, length = 50)
    private String name;

    protected UpcomingBookTag() {
    }

    UpcomingBookTag(UpcomingBook upcomingBook, String name) {
        this.upcomingBook = upcomingBook;
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
