package com.example.library.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BookTests {

    @Test
    void borrowOneDecrementsAvailableQuantity() {
        Book book = new Book("Refactoring", "Martin Fowler", "Engineering", "9780134757599", 3);

        book.borrowOne();

        assertThat(book.getAvailableQuantity()).isEqualTo(2);
    }

    @Test
    void borrowOneRejectsOutOfStockBooks() {
        Book book = new Book("Release It!", "Michael Nygard", "Operations", "9781680504552", 1);
        book.borrowOne();

        assertThatThrownBy(book::borrowOne)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Book is out of stock");
    }

    @Test
    void updateRejectsTotalQuantityBelowBorrowedCopies() {
        Book book = new Book("DDD", "Eric Evans", "Architecture", "9780321125217", 4);
        book.borrowOne();
        book.borrowOne();

        assertThatThrownBy(() -> book.update("DDD", "Eric Evans", "Architecture", "9780321125217", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Total quantity cannot be less than currently borrowed copies");
    }

    @Test
    void updateReplacesTagsWithNormalizedDistinctValues() {
        Book book = new Book("DDD", "Eric Evans", "Architecture", "9780321125217", 4, java.util.List.of("DDD", "Domain"));

        book.update("DDD", "Eric Evans", "Architecture", "9780321125217", 4, java.util.List.of(" Domain ", "Microservices", "domain"));

        assertThat(book.getTags()).containsExactly("domain", "microservices");
    }

    @Test
    void returnOneRestoresInventory() {
        Book book = new Book("Accelerate", "Nicole Forsgren", "DevOps", "9781942788331", 2);
        book.borrowOne();

        book.returnOne();

        assertThat(book.getAvailableQuantity()).isEqualTo(2);
    }
}
