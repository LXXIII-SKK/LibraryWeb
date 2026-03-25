package com.example.library.inventory;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.example.library.branch.BranchService;
import com.example.library.branch.LibraryBranch;
import com.example.library.catalog.Book;
import com.example.library.catalog.CatalogService;
import com.example.library.circulation.BorrowTransactionRepository;
import com.example.library.identity.AuthorizationService;
import com.example.library.identity.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTests {

    @Mock
    private BookHoldingRepository bookHoldingRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private CatalogService catalogService;

    @Mock
    private BranchService branchService;

    @Mock
    private LocationService locationService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private BorrowTransactionRepository borrowTransactionRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void updateRejectsMovingPhysicalHoldingWhenTrackedCopiesAreActive() {
        Book book = new Book("Release It!", "Michael Nygard", "Operations", "9781680504552", 2);
        ReflectionTestUtils.setField(book, "id", 50L);
        LibraryBranch sourceBranch = new LibraryBranch("CENTRAL", "Central Library", null, null, true);
        LibraryBranch targetBranch = new LibraryBranch("WEST", "West Branch", null, null, true);
        ReflectionTestUtils.setField(sourceBranch, "id", 1L);
        ReflectionTestUtils.setField(targetBranch, "id", 2L);
        LibraryLocation sourceLocation = new LibraryLocation(sourceBranch, "STACK-A", "Stacks A", "1", "A", true);
        LibraryLocation targetLocation = new LibraryLocation(targetBranch, "STACK-B", "Stacks B", "1", "B", true);
        ReflectionTestUtils.setField(sourceLocation, "id", 10L);
        ReflectionTestUtils.setField(targetLocation, "id", 20L);

        BookHolding holding = new BookHolding(book, sourceBranch, sourceLocation, HoldingFormat.PHYSICAL, 1, 0, null, true);
        ReflectionTestUtils.setField(holding, "id", 100L);
        BookCopy borrowedCopy = new BookCopy(holding, "copy-1", BookCopyStatus.BORROWED);

        when(bookHoldingRepository.findById(100L)).thenReturn(Optional.of(holding));
        when(branchService.resolveBranch(2L)).thenReturn(targetBranch);
        when(locationService.findEntity(20L)).thenReturn(targetLocation);
        when(bookCopyRepository.findAllByHolding_IdOrderByBarcodeAsc(100L)).thenReturn(List.of(borrowedCopy));

        assertThatThrownBy(() -> inventoryService.update(
                100L,
                new BookHoldingUpsertRequest(50L, 2L, 20L, HoldingFormat.PHYSICAL, 1, 0, null, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot move a physical holding while tracked copies are borrowed, reserved, or in transit");
    }
}
