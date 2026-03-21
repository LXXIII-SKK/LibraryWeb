package com.example.library.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTests {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookCoverRepository bookCoverRepository;

    @InjectMocks
    private CatalogService catalogService;

    @Test
    void searchNormalizesBlankFiltersToEmptyStrings() {
        when(bookRepository.search("", "", "")).thenReturn(List.of());

        catalogService.search("   ", " ", " ");

        verify(bookRepository).search("", "", "");
    }

    @Test
    void createTrimsValuesBeforeSaving() {
        BookRequest request = new BookRequest(
                "  Clean Code  ",
                "  Robert C. Martin ",
                "  Engineering  ",
                " 12345 ",
                List.of(" Clean ", "Refactoring", "clean"));
        Book persisted = new Book("Clean Code", "Robert C. Martin", "Engineering", "12345", 0, List.of("clean", "refactoring"));

        when(bookRepository.save(org.mockito.ArgumentMatchers.any(Book.class))).thenReturn(persisted);

        BookResponse response = catalogService.create(request);

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        Book saved = captor.getValue();

        assertThat(saved.getTitle()).isEqualTo("Clean Code");
        assertThat(saved.getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(saved.getCategory()).isEqualTo("Engineering");
        assertThat(saved.getIsbn()).isEqualTo("12345");
        assertThat(saved.getTags()).containsExactly("clean", "refactoring");
        assertThat(response.title()).isEqualTo("Clean Code");
    }

    @Test
    void deleteLoadsEntityBeforeDeletion() {
        Book book = new Book("Team Topologies", "Matthew Skelton", "Leadership", "9781942788812", 3);
        when(bookRepository.findById(42L)).thenReturn(Optional.of(book));

        catalogService.delete(42L);

        verify(bookRepository).findById(eq(42L));
        verify(bookRepository).delete(book);
    }

    @Test
    void uploadCoverStoresBinaryContentAndExposesCoverUrl() {
        Book book = new Book("DDD", "Eric Evans", "Architecture", "9780321125217", 4);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "ddd.png",
                "image/png",
                "fake-png-binary".getBytes());

        when(bookRepository.findById(7L)).thenReturn(Optional.of(book));
        when(bookCoverRepository.findById(7L)).thenReturn(Optional.empty());
        when(bookCoverRepository.save(org.mockito.ArgumentMatchers.any(BookCover.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        org.springframework.test.util.ReflectionTestUtils.setField(book, "id", 7L);

        BookResponse response = catalogService.uploadCover(7L, file);

        assertThat(response.coverImageUrl()).isEqualTo("/api/books/7/cover");
        assertThat(book.hasCoverImage()).isTrue();
        verify(bookCoverRepository).save(org.mockito.ArgumentMatchers.any(BookCover.class));
    }
}
