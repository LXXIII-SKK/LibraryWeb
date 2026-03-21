package com.example.library.circulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
}
