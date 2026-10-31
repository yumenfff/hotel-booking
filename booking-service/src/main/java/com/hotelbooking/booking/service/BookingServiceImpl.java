package com.hotelbooking.booking.service;

import com.hotelbooking.booking.dto.BookingCreateRequest;
import com.hotelbooking.booking.dto.BookingResponse;
import com.hotelbooking.booking.dto.PageResponse;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.entity.User;
import com.hotelbooking.booking.exception.BadRequestException;
import com.hotelbooking.booking.exception.ConflictException;
import com.hotelbooking.booking.exception.NotFoundException;
import com.hotelbooking.booking.mapper.BookingMapper;
import com.hotelbooking.booking.repository.BookingRepository;
import com.hotelbooking.booking.repository.UserRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

  private static final List<BookingStatus> ACTIVE_STATUSES =
      List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

  private final BookingRepository bookingRepository;
  private final UserRepository userRepository;
  private final BookingStateService bookingStateService;
  private final BookingMapper bookingMapper;

  @Override
  public BookingResponse create(BookingCreateRequest request, Jwt jwt) {
    Long userId = userId(jwt);
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
    String requestId = normalizeRequestId(request.requestId());

    Booking existing = bookingRepository.findByUserIdAndRequestId(userId, requestId).orElse(null);
    if (existing != null) {
      ensureSameRequest(existing, request);
      return bookingMapper.toResponse(existing);
    }

    if (bookingRepository.existsOverlappingBooking(
        request.roomId(), request.startDate(), request.endDate(), ACTIVE_STATUSES)) {
      throw new ConflictException(
          "Room " + request.roomId() + " is already booked for the requested dates");
    }

    Booking pending;
    try {
      pending = bookingStateService.createPending(user, request, requestId);
    } catch (DataIntegrityViolationException ex) {
      Booking concurrent =
          bookingRepository.findByUserIdAndRequestId(userId, requestId).orElseThrow(() -> ex);
      ensureSameRequest(concurrent, request);
      return bookingMapper.toResponse(concurrent);
    }
    return bookingMapper.toResponse(bookingStateService.confirm(pending.getId()));
  }

  @Override
  public PageResponse<BookingResponse> findMine(int page, int size, BookingStatus status, Jwt jwt) {
    if (page < 0 || size < 1 || size > 100) {
      throw new BadRequestException("page must be non-negative and size must be between 1 and 100");
    }
    PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<Booking> bookings =
        status == null
            ? bookingRepository.findByUserId(userId(jwt), pageable)
            : bookingRepository.findByUserIdAndStatus(userId(jwt), status, pageable);
    return new PageResponse<>(
        bookings.getContent().stream().map(bookingMapper::toResponse).toList(),
        bookings.getNumber(),
        bookings.getSize(),
        bookings.getTotalElements(),
        bookings.getTotalPages());
  }

  @Override
  public BookingResponse getById(Long id, Jwt jwt) {
    return bookingMapper.toResponse(loadAccessible(id, jwt));
  }

  @Override
  public void cancel(Long id, Jwt jwt) {
    Booking booking = loadAccessible(id, jwt);
    if (booking.getStatus() == BookingStatus.CANCELLED) {
      return;
    }
    if (booking.getStatus() == BookingStatus.PENDING) {
      throw new ConflictException("Pending booking cannot be cancelled");
    }
    bookingStateService.cancel(
        booking.getId(), isAdmin(jwt) ? "Cancelled by administrator" : "Cancelled by user");
  }

  private Booking loadAccessible(Long id, Jwt jwt) {
    Booking booking =
        bookingRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Booking " + id + " not found"));
    if (!isAdmin(jwt) && !booking.getUser().getId().equals(userId(jwt))) {
      throw new AccessDeniedException("Access denied");
    }
    return booking;
  }

  private void ensureSameRequest(Booking booking, BookingCreateRequest request) {
    if (!Objects.equals(booking.getRoomId(), request.roomId())
        || !Objects.equals(booking.getStartDate(), request.startDate())
        || !Objects.equals(booking.getEndDate(), request.endDate())) {
      throw new ConflictException("RequestId was already used with different booking parameters");
    }
  }

  private String normalizeRequestId(String requestId) {
    return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
  }

  private Long userId(Jwt jwt) {
    return ((Number) jwt.getClaim("uid")).longValue();
  }

  private boolean isAdmin(Jwt jwt) {
    return "ADMIN".equals(jwt.getClaimAsString("role"));
  }
}
