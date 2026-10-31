package com.hotelbooking.booking.repository;

import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

  Optional<Booking> findByUserIdAndRequestId(Long userId, String requestId);

  Page<Booking> findByUserId(Long userId, Pageable pageable);

  Page<Booking> findByUserIdAndStatus(Long userId, BookingStatus status, Pageable pageable);

  @Query(
      """

      select count(b) > 0
          from Booking b
          where b.roomId = :roomId
            and b.status in :statuses
            and b.startDate < :endDate
            and b.endDate > :startDate
          """)
  boolean existsOverlappingBooking(
      @Param("roomId") Long roomId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      @Param("statuses") Iterable<BookingStatus> statuses);
}
