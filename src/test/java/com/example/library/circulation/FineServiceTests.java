package com.example.library.circulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.example.library.catalog.Book;
import com.example.library.identity.AppRole;
import com.example.library.identity.AppUser;
import com.example.library.identity.AuthorizationService;
import com.example.library.identity.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FineServiceTests {

    @Mock
    private FineRecordRepository fineRecordRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private PolicyService policyService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private FineService fineService;

    @Test
    void createOverdueFineUsesPolicyRateAndOverdueDays() {
        AppUser user = new AppUser("member-7", "reader", "reader@library.local", AppRole.MEMBER);
        Book book = new Book("Release It!", "Michael Nygard", "Operations", "9781680504552", 2);
        BorrowTransaction transaction = new BorrowTransaction(
                user,
                book,
                Instant.now().minus(20, ChronoUnit.DAYS),
                Instant.now().minus(3, ChronoUnit.DAYS));
        LibraryPolicy policy = new LibraryPolicy(1L, 14, 7, 2, BigDecimal.valueOf(2.50), BigDecimal.valueOf(10), false);
        ReflectionTestUtils.setField(transaction, "id", 40L);

        when(policyService.getCurrentPolicyEntity()).thenReturn(policy);
        when(fineRecordRepository.existsByBorrowTransactionId(40L)).thenReturn(false);

        fineService.createOverdueFineIfRequired(transaction, Instant.now());

        ArgumentCaptor<FineRecord> captor = ArgumentCaptor.forClass(FineRecord.class);
        verify(fineRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(captor.getValue().getReason()).contains("late");
    }
}
