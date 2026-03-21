package com.example.library.catalog;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private static final Set<String> ALLOWED_COVER_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif");

    private static final long MAX_COVER_BYTES = 5L * 1024 * 1024;

    private final BookRepository bookRepository;
    private final BookCoverRepository bookCoverRepository;

    public CatalogService(BookRepository bookRepository, BookCoverRepository bookCoverRepository) {
        this.bookRepository = bookRepository;
        this.bookCoverRepository = bookCoverRepository;
    }

    public List<BookResponse> search(String query, String category, String tag) {
        String normalizedQuery = normalizeSearch(query);
        String normalizedCategory = normalizeSearch(category);
        String normalizedTag = normalizeSearch(tag);
        return bookRepository.search(normalizedQuery, normalizedCategory, normalizedTag).stream()
                .map(BookResponse::from)
                .toList();
    }

    public BookFilterOptionsResponse filterOptions() {
        return new BookFilterOptionsResponse(
                bookRepository.findDistinctCategories(),
                bookRepository.findDistinctTags());
    }

    public Book findEntity(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book %d was not found".formatted(bookId)));
    }

    public BookResponse getById(Long id) {
        return BookResponse.from(findEntity(id));
    }

    @Transactional
    public BookResponse create(BookRequest request) {
        Book book = new Book(
                request.title().trim(),
                request.author().trim(),
                normalize(request.category()),
                normalize(request.isbn()),
                0,
                normalizeTags(request.tags()));
        return BookResponse.from(bookRepository.save(book));
    }

    @Transactional
    public BookResponse update(Long id, BookRequest request) {
        Book book = findEntity(id);
        book.updateMetadata(
                request.title().trim(),
                request.author().trim(),
                normalize(request.category()),
                normalize(request.isbn()),
                normalizeTags(request.tags()));
        return BookResponse.from(book);
    }

    @Transactional
    public void delete(Long id) {
        bookRepository.delete(findEntity(id));
    }

    @Transactional
    public BookResponse uploadCover(Long id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A non-empty image file is required");
        }
        if (file.getSize() > MAX_COVER_BYTES) {
            throw new IllegalArgumentException("Cover image must be 5 MB or smaller");
        }

        Book book = findEntity(id);
        String fileName = normalizeFileName(file.getOriginalFilename());
        String contentType = normalizeContentType(file.getContentType(), fileName);
        byte[] content = readFileContent(file);

        BookCover bookCover = bookCoverRepository.findById(id)
                .map(existing -> {
                    existing.replace(fileName, contentType, content);
                    return existing;
                })
                .orElseGet(() -> new BookCover(book, fileName, contentType, content));

        book.markCoverImagePresent();
        bookCoverRepository.save(bookCover);
        return BookResponse.from(book);
    }

    public BookCoverContent loadCover(Long id) {
        BookCover cover = bookCoverRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book %d does not have a cover image".formatted(id)));
        return new BookCoverContent(cover.getFileName(), cover.getContentType(), cover.getContent());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeSearch(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }

    private List<String> normalizeTags(Collection<String> tags) {
        if (tags == null) {
            return List.of();
        }

        return tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .sorted()
                .toList();
    }

    private byte[] readFileContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read uploaded cover image", exception);
        }
    }

    private String normalizeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "cover";
        }

        return originalFilename.replace("\\", "_").replace("/", "_").trim();
    }

    private String normalizeContentType(String contentType, String fileName) {
        if (contentType != null && ALLOWED_COVER_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            return contentType.toLowerCase(Locale.ROOT);
        }

        String lowerName = fileName.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".png")) {
            return "image/png";
        }
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lowerName.endsWith(".webp")) {
            return "image/webp";
        }
        if (lowerName.endsWith(".gif")) {
            return "image/gif";
        }

        throw new IllegalArgumentException("Only JPEG, PNG, WEBP, and GIF cover images are supported");
    }
}
