package com.example.library.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.List;
import java.util.Optional;

import com.example.library.identity.AccountStatus;
import com.example.library.identity.AppRole;
import com.example.library.identity.CurrentUser;
import com.example.library.identity.CurrentUserService;
import com.example.library.identity.MembershipStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTests {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookCoverRepository bookCoverRepository;

    @Mock
    private BookViewStatsRepository bookViewStatsRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

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
        when(currentUserService.getCurrentUser()).thenReturn(defaultCurrentUser());

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
    void searchIncludesViewCounts() {
        Book book = new Book("Release It!", "Michael Nygard", "Operations", "9781680504552", 3);
        org.springframework.test.util.ReflectionTestUtils.setField(book, "id", 5L);

        when(bookRepository.search("", "", "")).thenReturn(List.of(book));
        when(bookViewStatsRepository.findViewCountsByBookIds(List.of(5L))).thenReturn(Map.of(5L, 12L));

        List<BookResponse> responses = catalogService.search("", "", "");

        assertThat(responses).singleElement().satisfies(response -> assertThat(response.viewCount()).isEqualTo(12L));
    }

    @Test
    void deleteLoadsEntityBeforeDeletion() {
        Book book = new Book("Team Topologies", "Matthew Skelton", "Leadership", "9781942788812", 3);
        when(bookRepository.findById(42L)).thenReturn(Optional.of(book));
        when(currentUserService.getCurrentUser()).thenReturn(defaultCurrentUser());

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
        when(currentUserService.getCurrentUser()).thenReturn(defaultCurrentUser());

        org.springframework.test.util.ReflectionTestUtils.setField(book, "id", 7L);

        BookResponse response = catalogService.uploadCover(7L, file);

        assertThat(response.coverImageUrl()).isEqualTo("/api/books/7/cover");
        assertThat(response.viewCount()).isZero();
        assertThat(book.hasCoverImage()).isTrue();
        verify(bookCoverRepository).save(org.mockito.ArgumentMatchers.any(BookCover.class));
    }

    private CurrentUser defaultCurrentUser() {
        return new CurrentUser(
                1L,
                "admin-1",
                "admin",
                "admin@library.local",
                AppRole.ADMIN,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                null,
                null);
    }
}
