package com.example.library.catalog;

import java.time.Instant;
import java.util.Arrays;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "book_cover")
class BookCover {

    @Id
    @Column(name = "book_id")
    private Long bookId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "content", nullable = false)
    private byte[] content;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BookCover() {
    }

    BookCover(Book book, String fileName, String contentType, byte[] content) {
        this.book = book;
        this.fileName = fileName;
        this.contentType = contentType;
        this.content = Arrays.copyOf(content, content.length);
    }

    @PrePersist
    @PreUpdate
    void onChange() {
        this.updatedAt = Instant.now();
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContent() {
        return Arrays.copyOf(content, content.length);
    }

    public void replace(String fileName, String contentType, byte[] content) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.content = Arrays.copyOf(content, content.length);
    }
}
