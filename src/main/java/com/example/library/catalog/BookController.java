package com.example.library.catalog;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/books")
class BookController {

    private final CatalogService catalogService;

    BookController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    List<BookResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag) {
        return catalogService.search(query, category, tag);
    }

    @GetMapping("/filters")
    BookFilterOptionsResponse filters() {
        return catalogService.filterOptions();
    }

    @GetMapping("/{id}")
    BookResponse getById(@PathVariable Long id) {
        return catalogService.getById(id);
    }

    @GetMapping("/{id}/cover")
    ResponseEntity<ByteArrayResource> getCover(@PathVariable Long id) {
        BookCoverContent cover = catalogService.loadCover(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cover.contentType()))
                .cacheControl(CacheControl.noCache())
                .body(new ByteArrayResource(cover.content()));
    }

    @PostMapping
    @PreAuthorize("@authorizationService.canManageCatalog()")
    BookResponse create(@Valid @RequestBody BookRequest request) {
        return catalogService.create(request);
    }

    @PostMapping(path = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authorizationService.canManageCatalog()")
    BookResponse uploadCover(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return catalogService.uploadCover(id, file);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authorizationService.canManageCatalog()")
    BookResponse update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return catalogService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationService.canDeleteCatalog()")
    void delete(@PathVariable Long id) {
        catalogService.delete(id);
    }
}
