package com.example.library.circulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import com.example.library.catalog.Book;
import com.example.library.catalog.CatalogService;
import com.example.library.identity.AccountStatus;
import com.example.library.identity.AuthorizationService;
import com.example.library.identity.AppRole;
import com.example.library.identity.AppUser;
import com.example.library.identity.AppUserRepository;
import com.example.library.identity.CurrentUser;
import com.example.library.identity.CurrentUserService;
import com.example.library.identity.MembershipStatus;
import com.example.library.inventory.BookHolding;
import com.example.library.inventory.HoldingFormat;
import com.example.library.inventory.InventoryService;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CirculationServiceTests {

    @Mock
    private BorrowTransactionRepository borrowTransactionRepository;

    @Mock
    private CatalogService catalogService;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private PolicyService policyService;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationService reservationService;

    @Mock
    private FineService fineService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private final ObservationRegistry observationRegistry = ObservationRegistry.create();

    @InjectMocks
    private CirculationService circulationService;

    @Test
    void borrowCreatesTransactionAndPublishesEvent() {
        Book book = new Book("DDD", "Eric Evans", "Architecture", "9780321125217", 5);
        BookHolding holding = new BookHolding(book, new com.example.library.branch.LibraryBranch("CENTRAL", "Central", null, null, true), null, HoldingFormat.DIGITAL, 5, 5, "https://example.com", true);
        AppUser appUser = new AppUser("keycloak-member", "reader", "reader@library.local", AppRole.MEMBER);
        CurrentUser currentUser = new CurrentUser(7L, "keycloak-member", "reader", "reader@library.local", AppRole.MEMBER);
        BorrowBookRequest request = new BorrowBookRequest(10L, 100L);
        LibraryPolicy policy = new LibraryPolicy(1L, 14, 7, 2, java.math.BigDecimal.valueOf(1.50), java.math.BigDecimal.TEN, false);
        ReflectionTestUtils.setField(book, "id", 10L);
        ReflectionTestUtils.setField(holding, "id", 100L);
        ReflectionTestUtils.setField(appUser, "id", 7L);

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(currentUserService.getCurrentUserEntity()).thenReturn(appUser);
        when(catalogService.findEntity(10L)).thenReturn(book);
        when(inventoryService.resolveBorrowableHolding(book, 100L)).thenReturn(holding);
        org.mockito.Mockito.doAnswer(invocation -> {
            book.synchronizeInventory(holding.getTotalQuantity(), holding.getAvailableQuantity());
            return null;
        }).when(inventoryService).synchronizeBookInventory(book);
        when(policyService.getCurrentPolicyEntity()).thenReturn(policy);
        when(reservationRepository.findFirstByBookIdAndStatusOrderByReservedAtAsc(10L, ReservationStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(borrowTransactionRepository.save(any(BorrowTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BorrowTransactionResponse response = circulationService.borrow(request);

        assertThat(response.bookId()).isEqualTo(10L);
        assertThat(book.getAvailableQuantity()).isEqualTo(4);
        assertThat(response.dueAt()).isAfter(response.borrowedAt().plusSeconds(13L * 24L * 60L * 60L));

        ArgumentCaptor<BookBorrowedEvent> eventCaptor = ArgumentCaptor.forClass(BookBorrowedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().bookTitle()).isEqualTo("DDD");
    }

    @Test
    void borrowRejectsWhenAnotherUserHoldsEarliestReservation() {
        Book book = new Book("Release It!", "Michael Nygard", "Operations", "9781680504552", 2);
        AppUser currentUserEntity = new AppUser("member-21", "reader", "reader@library.local", AppRole.MEMBER);
        AppUser reservedUser = new AppUser("member-22", "other", "other@library.local", AppRole.MEMBER);
        Reservation reservation = new Reservation(reservedUser, book, Instant.now().minusSeconds(600));
        CurrentUser currentUser = new CurrentUser(21L, "member-21", "reader", "reader@library.local", AppRole.MEMBER);
        ReflectionTestUtils.setField(book, "id", 10L);
        ReflectionTestUtils.setField(reservedUser, "id", 22L);

        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(catalogService.findEntity(10L)).thenReturn(book);
        when(reservationRepository.findFirstByBookIdAndStatusOrderByReservedAtAsc(10L, ReservationStatus.ACTIVE))
                .thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> circulationService.borrow(new BorrowBookRequest(10L, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This title is currently reserved for another member");
    }

    @Test
    void returnRejectsWhenNonAdminReturnsSomeoneElsesBook() {
        AppUser borrower = new AppUser("seed-user", "reader", "reader@library.local", AppRole.MEMBER);
        Book book = new Book("Release It!", "Michael Nygard", "Operations", "9781680504552", 3);
        BorrowTransaction transaction = new BorrowTransaction(borrower, book, Instant.now().minusSeconds(7200), Instant.now().plusSeconds(7200));
        CurrentUser outsider = new CurrentUser(99L, "outsider", "outsider", "outsider@library.local", AppRole.MEMBER);
        ReflectionTestUtils.setField(borrower, "id", 15L);
        ReflectionTestUtils.setField(book, "id", 20L);

        when(currentUserService.getCurrentUser()).thenReturn(outsider);
        when(borrowTransactionRepository.findById(12L)).thenReturn(Optional.of(transaction));
        org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("denied"))
                .when(authorizationService)
                .assertCanReturnBorrowingForUser(borrower);

        assertThatThrownBy(() -> circulationService.returnBook(12L))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessage("denied");
    }

    @Test
    void adminCanReturnBorrowedBook() {
        AppUser borrower = new AppUser("seed-user", "reader", "reader@library.local", AppRole.MEMBER);
        Book book = new Book("Accelerate", "Nicole Forsgren", "DevOps", "9781942788331", 2);
        book.borrowOne();
        BorrowTransaction transaction = new BorrowTransaction(borrower, book, Instant.now().minusSeconds(7200), Instant.now().plusSeconds(7200));
        CurrentUser admin = new CurrentUser(1L, "admin", "admin", "admin@library.local", AppRole.ADMIN);
        ReflectionTestUtils.setField(borrower, "id", 22L);
        ReflectionTestUtils.setField(book, "id", 5L);

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(borrowTransactionRepository.findById(77L)).thenReturn(Optional.of(transaction));

        BorrowTransactionResponse response = circulationService.returnBook(77L);

        assertThat(response.status()).isEqualTo(BorrowStatus.RETURNED);
        assertThat(book.getAvailableQuantity()).isEqualTo(2);
    }

    @Test
    void staffCheckoutReadyReservationPublishesReadyHoldBorrowEvent() {
        AppUser member = new AppUser(
                "member-8",
                "reader",
                "reader@library.local",
                AppRole.MEMBER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        Book book = new Book("Domain-Driven Design", "Eric Evans", "Architecture", "9780321125217", 2);
        com.example.library.branch.LibraryBranch branch = new com.example.library.branch.LibraryBranch(
                "EAST",
                "East Branch",
                null,
                null,
                true);
        ReflectionTestUtils.setField(branch, "id", 3L);
        com.example.library.inventory.LibraryLocation location = new com.example.library.inventory.LibraryLocation(
                branch,
                "STACK-A",
                "Stacks A",
                "1",
                "A",
                true);
        BookHolding holding = new BookHolding(book, branch, location, HoldingFormat.PHYSICAL, 2, 1, null, true);
        Reservation reservation = new Reservation(member, book, branch, Instant.now().minusSeconds(3600));
        AppUser actor = new AppUser(
                "manager-1",
                "branch.manager",
                "manager@library.local",
                AppRole.BRANCH_MANAGER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        CurrentUser branchManager = new CurrentUser(
                91L,
                "manager-1",
                "branch.manager",
                "manager@library.local",
                AppRole.BRANCH_MANAGER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        LibraryPolicy policy = new LibraryPolicy(1L, 14, 7, 2, java.math.BigDecimal.ONE, java.math.BigDecimal.TEN, false);

        ReflectionTestUtils.setField(member, "id", 8L);
        ReflectionTestUtils.setField(actor, "id", 91L);
        ReflectionTestUtils.setField(book, "id", 10L);
        ReflectionTestUtils.setField(location, "id", 30L);
        ReflectionTestUtils.setField(holding, "id", 100L);
        ReflectionTestUtils.setField(reservation, "id", 400L);
        reservation.markReadyForPickup(holding, null, Instant.now().minusSeconds(300), Instant.now().plusSeconds(3600));

        when(currentUserService.getCurrentUser()).thenReturn(branchManager);
        when(currentUserService.getCurrentUserEntity()).thenReturn(actor);
        when(authorizationService.canCheckoutForMember()).thenReturn(true);
        when(appUserRepository.findById(8L)).thenReturn(Optional.of(member));
        when(catalogService.findEntity(10L)).thenReturn(book);
        when(reservationRepository.findById(400L)).thenReturn(Optional.of(reservation));
        when(policyService.getCurrentPolicyEntity()).thenReturn(policy);
        when(borrowTransactionRepository.save(any(BorrowTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BorrowTransactionResponse response = circulationService.staffCheckout(new StaffCheckoutRequest(8L, 10L, null, 400L));

        assertThat(response.status()).isEqualTo(BorrowStatus.BORROWED);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.FULFILLED);

        ArgumentCaptor<BookBorrowedEvent> eventCaptor = ArgumentCaptor.forClass(BookBorrowedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().fromReadyReservation()).isTrue();
    }

    @Test
    void adminOverrideRenewalRequiresReason() {
        AppUser borrower = new AppUser(
                "member-10",
                "reader",
                "reader@library.local",
                AppRole.MEMBER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        Book book = new Book("Release It!", "Michael Nygard", "Operations", "9781680502398", 2);
        BorrowTransaction transaction = new BorrowTransaction(
                borrower,
                book,
                Instant.now().minusSeconds(7200),
                Instant.now().plusSeconds(7200));
        CurrentUser admin = new CurrentUser(1L, "admin-1", "admin", "admin@library.local", AppRole.ADMIN);
        LibraryPolicy policy = new LibraryPolicy(1L, 14, 7, 2, java.math.BigDecimal.ONE, java.math.BigDecimal.TEN, false);
        ReflectionTestUtils.setField(book, "id", 45L);
        ReflectionTestUtils.setField(borrower, "id", 10L);

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(borrowTransactionRepository.findById(55L)).thenReturn(Optional.of(transaction));
        when(policyService.getCurrentPolicyEntity()).thenReturn(policy);
        when(reservationRepository.existsByBookIdAndStatusAndUserIdNot(45L, ReservationStatus.ACTIVE, 10L))
                .thenReturn(false);

        assertThatThrownBy(() -> circulationService.renewBorrowing(55L, new RenewBorrowingRequest(null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Override renewals require a reason");
    }

    @Test
    void adminOverrideRenewalStoresReasonAndPublishesEvent() {
        AppUser borrower = new AppUser(
                "member-11",
                "reader",
                "reader@library.local",
                AppRole.MEMBER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        Book book = new Book("Clean Architecture", "Robert C. Martin", "Architecture", "9780134494166", 2);
        BorrowTransaction transaction = new BorrowTransaction(
                borrower,
                book,
                Instant.now().minusSeconds(7200),
                Instant.now().plusSeconds(7200));
        CurrentUser admin = new CurrentUser(1L, "admin-1", "admin", "admin@library.local", AppRole.ADMIN);
        LibraryPolicy policy = new LibraryPolicy(1L, 14, 7, 2, java.math.BigDecimal.ONE, java.math.BigDecimal.TEN, false);
        Instant overrideDueAt = Instant.now().plusSeconds(10L * 24L * 60L * 60L);
        ReflectionTestUtils.setField(book, "id", 44L);
        ReflectionTestUtils.setField(borrower, "id", 11L);

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(borrowTransactionRepository.findById(56L)).thenReturn(Optional.of(transaction));
        when(policyService.getCurrentPolicyEntity()).thenReturn(policy);
        when(reservationRepository.existsByBookIdAndStatusAndUserIdNot(44L, ReservationStatus.ACTIVE, 11L))
                .thenReturn(false);

        BorrowTransactionResponse response = circulationService.renewBorrowing(
                56L,
                new RenewBorrowingRequest(overrideDueAt, "Member traveling for research"));

        assertThat(response.lastRenewalOverride()).isTrue();
        assertThat(response.lastRenewalReason()).isEqualTo("Member traveling for research");
        assertThat(response.dueAt()).isEqualTo(overrideDueAt);

        ArgumentCaptor<BorrowingRenewedEvent> eventCaptor = ArgumentCaptor.forClass(BorrowingRenewedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().overrideApplied()).isTrue();
        assertThat(eventCaptor.getValue().reason()).isEqualTo("Member traveling for research");
    }

    @Test
    void branchManagerCanRecordBorrowingException() {
        AppUser borrower = new AppUser(
                "member-12",
                "reader",
                "reader@library.local",
                AppRole.MEMBER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        Book book = new Book("The Phoenix Project", "Gene Kim", "DevOps", "9781942788294", 2);
        BorrowTransaction transaction = new BorrowTransaction(
                borrower,
                book,
                Instant.now().minusSeconds(3600),
                Instant.now().plusSeconds(3600));
        CurrentUser branchManager = new CurrentUser(
                90L,
                "manager-2",
                "branch.manager",
                "manager@library.local",
                AppRole.BRANCH_MANAGER,
                AccountStatus.ACTIVE,
                MembershipStatus.GOOD_STANDING,
                3L,
                3L);
        ReflectionTestUtils.setField(book, "id", 66L);

        when(currentUserService.getCurrentUser()).thenReturn(branchManager);
        when(borrowTransactionRepository.findById(88L)).thenReturn(Optional.of(transaction));

        BorrowTransactionResponse response = circulationService.recordBorrowingException(
                88L,
                new BorrowingExceptionRequest(BorrowingExceptionAction.CLAIM_RETURNED, "Desk search started"));

        assertThat(response.status()).isEqualTo(BorrowStatus.CLAIMED_RETURNED);
        assertThat(response.exceptionNote()).isEqualTo("Desk search started");

        ArgumentCaptor<BorrowingExceptionRecordedEvent> eventCaptor =
                ArgumentCaptor.forClass(BorrowingExceptionRecordedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().status()).isEqualTo(BorrowStatus.CLAIMED_RETURNED);
    }

    @Test
    void lostBorrowingsCannotBeReturnedThroughReturnEndpoint() {
        AppUser borrower = new AppUser("member-13", "reader", "reader@library.local", AppRole.MEMBER);
        Book book = new Book("Accelerate", "Nicole Forsgren", "DevOps", "9781942788331", 2);
        BorrowTransaction transaction = new BorrowTransaction(
                borrower,
                book,
                Instant.now().minusSeconds(7200),
                Instant.now().plusSeconds(7200));
        CurrentUser admin = new CurrentUser(1L, "admin", "admin", "admin@library.local", AppRole.ADMIN);
        ReflectionTestUtils.setField(transaction, "status", BorrowStatus.LOST);

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(borrowTransactionRepository.findById(77L)).thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> circulationService.returnBook(77L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Lost or damaged borrowings must be resolved through an item exception workflow");
    }
}
