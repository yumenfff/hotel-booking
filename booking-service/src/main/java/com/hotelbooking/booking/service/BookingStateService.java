package com.hotelbooking.booking.service;

import com.hotelbooking.booking.dto.BookingCreateRequest;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.entity.User;
import com.hotelbooking.booking.exception.ConflictException;
import com.hotelbooking.booking.exception.NotFoundException;
import com.hotelbooking.booking.repository.BookingRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingStateService {

  private final BookingRepository bookingRepository;

  @Transactional
  public Booking createPending(User user, BookingCreateRequest request, String requestId) {
    Booking booking = new Booking();
    booking.setUser(user);
    booking.setRoomId(request.roomId());
    booking.setStartDate(request.startDate());
    booking.setEndDate(request.endDate());
    booking.setStatus(BookingStatus.PENDING);
    booking.setRequestId(requestId);
    touch(booking);

    Booking saved = bookingRepository.saveAndFlush(booking);
    log.info(
        "Booking state changed: bookingId={}, requestId={}, status={}",
        saved.getId(),
        saved.getRequestId(),
        saved.getStatus());
    return saved;
  }

  @Transactional
  public Booking confirm(Long bookingId) {
    Booking booking = findOrThrow(bookingId);
    requirePending(booking, "confirmed");
    booking.setStatus(BookingStatus.CONFIRMED);
    booking.setFailureReason(null);
    touch(booking);
    Booking saved = bookingRepository.saveAndFlush(booking);
    log.info(
        "Booking state changed: bookingId={}, requestId={}, status={}",
        saved.getId(),
        saved.getRequestId(),
        saved.getStatus());
    return saved;
  }

  @Transactional
  public Booking cancel(Long bookingId, String failureReason) {
    Booking booking = findOrThrow(bookingId);
    if (booking.getStatus() == BookingStatus.CANCELLED) {
      return booking;
    }
    booking.setStatus(BookingStatus.CANCELLED);
    booking.setFailureReason(failureReason);
    touch(booking);
    Booking saved = bookingRepository.saveAndFlush(booking);
    log.info(
        "Booking state changed: bookingId={}, requestId={}, status={}, reason={}",
        saved.getId(),
        saved.getRequestId(),
        saved.getStatus(),
        saved.getFailureReason());
    return saved;
  }

  private void requirePending(Booking booking, String action) {
    if (booking.getStatus() != BookingStatus.PENDING) {
      throw new ConflictException(
          "Booking %d cannot be %s from status %s"
              .formatted(booking.getId(), action, booking.getStatus()));
    }
  }

  private Booking findOrThrow(Long bookingId) {
    return bookingRepository
        .findById(bookingId)
        .orElseThrow(() -> new NotFoundException("Booking " + bookingId + " not found"));
  }

  private void touch(Booking booking) {
    booking.setUpdatedAt(Instant.now());
  }
}
