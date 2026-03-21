package com.example.library.circulation;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findAllByUserIdOrderByReservedAtDesc(Long userId);

    List<Reservation> findAllByUser_Branch_IdOrderByReservedAtDesc(Long branchId);

    List<Reservation> findAllByOrderByReservedAtDesc();

    boolean existsByUserIdAndBookIdAndStatus(Long userId, Long bookId, ReservationStatus status);

    boolean existsByBookIdAndStatusAndUserIdNot(Long bookId, ReservationStatus status, Long userId);

    Optional<Reservation> findFirstByBookIdAndStatusOrderByReservedAtAsc(Long bookId, ReservationStatus status);

    Optional<Reservation> findFirstByBookIdAndStatusInOrderByReservedAtAsc(Long bookId, Collection<ReservationStatus> statuses);

    List<Reservation> findAllByUserIdAndBookIdAndStatusInOrderByReservedAtAsc(
            Long userId,
            Long bookId,
            Collection<ReservationStatus> statuses);
}
