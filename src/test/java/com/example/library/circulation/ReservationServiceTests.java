package com.example.library.circulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import com.example.library.branch.BranchService;
import com.example.library.branch.LibraryBranch;
import com.example.library.catalog.Book;
import com.example.library.catalog.CatalogService;
import com.example.library.identity.AccountStatus;
import com.example.library.identity.AppRole;
import com.example.library.identity.AppUser;
import com.example.library.identity.AuthorizationService;
import com.example.library.identity.CurrentUser;
import com.example.library.identity.CurrentUserService;
import com.example.library.identity.MembershipStatus;
import com.example.library.inventory.BookCopy;
import com.example.library.inventory.BookCopyStatus;
import com.example.library.inventory.BookHolding;
import com.example.library.inventory.HoldingFormat;
import com.example.library.inventory.LibraryLocation;
import com.example.library.inventory.InventoryService;
import com.example.library.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTests {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BorrowTransactionRepository borrowTransactionRepository;

    @Mock
    private CatalogService catalogService;

    @Mock
    private BranchService branchService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private PolicyService policyService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private TransferService transferService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void memberCanCreateReservationWhenNoActiveReservationExists() {
        CurrentUser currentUser = new CurrentUser(
                8L,
                "member-8",
                "reader",
                "reader@library.local",
                AppRole.MEMBER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                1L,
                1L);
        AppUser appUser = new AppUser("member-8", "reader", "reader@library.local", AppRole.MEMBER);
        Book book = new Book("DDD", "Eric Evans", "Architecture", "9780321125217", 2);
        LibraryBranch branch = new LibraryBranch("CENTRAL", "Central Library", null, null, true);
        ReflectionTestUtils.setField(book, "id", 4L);
        ReflectionTestUtils.setField(appUser, "id", 8L);
        ReflectionTestUtils.setField(branch, "id", 1L);

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(currentUserService.getCurrentUserEntity()).thenReturn(appUser);
        when(authorizationService.canCreateReservationForSelf()).thenReturn(true);
        when(catalogService.findEntity(4L)).thenReturn(book);
        when(branchService.resolveBranch(1L)).thenReturn(branch);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = reservationService.createForCurrentUser(new CreateReservationRequest(4L, 1L));

        assertThat(response.bookId()).isEqualTo(4L);
        assertThat(response.status()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(response.pickupBranch()).isNotNull();
        assertThat(response.pickupBranch().id()).isEqualTo(1L);
    }

    @Test
    void cancelRejectsAlreadyClosedReservation() {
        AppUser user = new AppUser("member-9", "reader", "reader@library.local", AppRole.MEMBER);
        Book book = new Book("Clean Architecture", "Robert C. Martin", "Architecture", "9780134494166", 2);
        Reservation reservation = new Reservation(user, book, Instant.now());
        ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CANCELLED);

        when(reservationRepository.findById(9L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancel(9L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only open reservations can be cancelled");
    }

    @Test
    void prepareForPickupStartsTransferForCrossBranchPhysicalReservation() {
        AppUser member = new AppUser(
                "member-10",
                "reader",
                "reader@library.local",
                AppRole.MEMBER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                1L,
                2L);
        AppUser adminActor = new AppUser("admin-1", "admin", "admin@library.local", AppRole.ADMIN);
        CurrentUser admin = new CurrentUser(1L, "admin-1", "admin", "admin@library.local", AppRole.ADMIN);
        Book book = new Book("Designing Data-Intensive Applications", "Martin Kleppmann", "Architecture", "9781449373320", 2);
        LibraryBranch sourceBranch = new LibraryBranch("CENTRAL", "Central Library", null, null, true);
        LibraryBranch pickupBranch = new LibraryBranch("WEST", "West Branch", null, null, true);
        ReflectionTestUtils.setField(sourceBranch, "id", 1L);
        ReflectionTestUtils.setField(pickupBranch, "id", 2L);
        LibraryLocation location = new LibraryLocation(sourceBranch, "STACK-A", "Stacks A", "1", "A", true);
        ReflectionTestUtils.setField(location, "id", 20L);
        BookHolding holding = new BookHolding(book, sourceBranch, location, HoldingFormat.PHYSICAL, 2, 2, null, true);
        ReflectionTestUtils.setField(book, "id", 15L);
        ReflectionTestUtils.setField(member, "id", 10L);
        ReflectionTestUtils.setField(adminActor, "id", 1L);
        ReflectionTestUtils.setField(holding, "id", 100L);

        Reservation reservation = new Reservation(member, book, pickupBranch, Instant.now().minusSeconds(600));
        ReflectionTestUtils.setField(reservation, "id", 400L);

        BookCopy copy = new BookCopy(holding, "copy-100", BookCopyStatus.AVAILABLE);
        ReflectionTestUtils.setField(copy, "id", 500L);
        BookTransfer transfer = new BookTransfer(copy, holding, pickupBranch, Instant.now());
        ReflectionTestUtils.setField(transfer, "id", 600L);

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(currentUserService.getCurrentUserEntity()).thenReturn(adminActor);
        when(authorizationService.canManageReservations()).thenReturn(true);
        when(reservationRepository.findById(400L)).thenReturn(Optional.of(reservation));
        when(inventoryService.resolveBorrowableHolding(book, 100L)).thenReturn(holding);
        when(inventoryService.startTransfer(holding, pickupBranch)).thenReturn(copy);
        when(transferService.createInTransitTransfer(eq(holding), eq(copy), eq(pickupBranch), any(Instant.class)))
                .thenReturn(transfer);

        ReservationResponse response = reservationService.prepareForPickup(400L, new PrepareReservationRequest(100L));

        assertThat(response.status()).isEqualTo(ReservationStatus.IN_TRANSIT);
        assertThat(response.reservedHoldingId()).isEqualTo(100L);
        assertThat(response.reservedCopyId()).isEqualTo(500L);
        assertThat(response.transferId()).isEqualTo(600L);
        assertThat(response.transferStatus()).isEqualTo(BookTransferStatus.IN_TRANSIT);
        assertThat(response.transferDestinationBranchName()).isEqualTo("West Branch");
        verify(inventoryService).startTransfer(holding, pickupBranch);
        verify(transferService).createInTransitTransfer(eq(holding), eq(copy), eq(pickupBranch), any(Instant.class));
    }
}
