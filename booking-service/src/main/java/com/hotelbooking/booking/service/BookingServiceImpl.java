package com.hotelbooking.booking.service;

import com.hotelbooking.booking.client.HotelClient;
import com.hotelbooking.booking.client.HotelConflictException;
import com.hotelbooking.booking.client.HotelRejectedException;
import com.hotelbooking.booking.client.HotelUnavailableException;
import com.hotelbooking.booking.dto.BookingCreateRequest;
import com.hotelbooking.booking.dto.BookingResponse;
import com.hotelbooking.booking.dto.PageResponse;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.entity.User;
import com.hotelbooking.booking.exception.BadRequestException;
import com.hotelbooking.booking.exception.ConflictException;
import com.hotelbooking.booking.exception.NotFoundException;
import com.hotelbooking.booking.exception.ServiceUnavailableException;
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

  private static final String NO_ROOMS_MESSAGE = "No rooms are available for the requested dates";
  private static final List<BookingStatus> ACTIVE_STATUSES =
      List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

  private final BookingRepository bookingRepository;
  private final UserRepository userRepository;
  private final BookingStateService bookingStateService;
  private final HotelClient hotelClient;
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

    List<Long> candidates = candidates(request);
    if (candidates.isEmpty()) {
      throw new ConflictException(NO_ROOMS_MESSAGE);
    }
    if (!request.autoSelect()
        && bookingRepository.existsOverlappingBooking(
            request.roomId(), request.startDate(), request.endDate(), ACTIVE_STATUSES)) {
      throw new ConflictException(
          "Room " + request.roomId() + " is already booked for the requested dates");
    }

    Booking pending;
    try {
      pending = bookingStateService.createPending(user, request, requestId, candidates.get(0));
    } catch (DataIntegrityViolationException ex) {
      Booking concurrent =
          bookingRepository.findByUserIdAndRequestId(userId, requestId).orElseThrow(() -> ex);
      ensureSameRequest(concurrent, request);
      return bookingMapper.toResponse(concurrent);
    }

    for (Long roomId : candidates) {
      if (!Objects.equals(pending.getRoomId(), roomId)) {
        pending = bookingStateService.assignRoom(pending.getId(), roomId);
      }
      if (lockRoom(pending, roomId, request)) {
        return bookingMapper.toResponse(confirmOrCompensate(pending, roomId));
      }
    }

    bookingStateService.cancel(pending.getId(), "No candidate room is available");
    throw new ConflictException(NO_ROOMS_MESSAGE);
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
    try {
      hotelClient.release(booking.getRoomId(), hotelRequestId(booking.getId()));
    } catch (HotelConflictException ex) {
      throw new ConflictException(ex.getMessage());
    } catch (HotelRejectedException | HotelUnavailableException ex) {
      throw unavailable(ex);
    }
    bookingStateService.cancel(
        booking.getId(), isAdmin(jwt) ? "Cancelled by administrator" : "Cancelled by user");
  }

  private List<Long> candidates(BookingCreateRequest request) {
    return request.autoSelect()
        ? hotelClient.findRecommendedRoomIds(request.startDate(), request.endDate())
        : List.of(request.roomId());
  }

  private boolean lockRoom(Booking pending, Long roomId, BookingCreateRequest request) {
    try {
      hotelClient.confirmAvailability(
          roomId,
          hotelRequestId(pending.getId()),
          pending.getId(),
          request.startDate(),
          request.endDate());
      return true;
    } catch (HotelConflictException ex) {
      log.info(
          "Booking candidate rejected: bookingId={}, requestId={}, roomId={}",
          pending.getId(),
          pending.getRequestId(),
          roomId);
      if (request.autoSelect()) {
        return false;
      }
      bookingStateService.cancel(pending.getId(), ex.getMessage());
      throw new ConflictException(ex.getMessage());
    } catch (HotelRejectedException ex) {
      bookingStateService.cancel(pending.getId(), ex.getMessage());
      if (ex.getStatus() == 404) {
        throw new NotFoundException(ex.getMessage());
      }
      throw unavailable(ex);
    } catch (HotelUnavailableException ex) {
      compensate(pending, roomId);
      bookingStateService.cancel(pending.getId(), ex.getMessage());
      throw unavailable(ex);
    }
  }

  private Booking confirmOrCompensate(Booking pending, Long roomId) {
    try {
      return bookingStateService.confirm(pending.getId());
    } catch (RuntimeException ex) {
      log.error(
          "Local confirmation failed after room lock: bookingId={}, requestId={}, roomId={}",
          pending.getId(),
          pending.getRequestId(),
          roomId,
          ex);
      compensate(pending, roomId);
      bookingStateService.cancel(pending.getId(), "Failed to confirm booking locally");
      throw ex;
    }
  }

  private void compensate(Booking booking, Long roomId) {
    try {
      hotelClient.release(roomId, hotelRequestId(booking.getId()));
    } catch (RuntimeException ex) {
      log.warn(
          "Booking compensation failed: bookingId={}, requestId={}, roomId={}",
          booking.getId(),
          booking.getRequestId(),
          roomId,
          ex);
    }
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
    if (booking.isAutoSelect() != request.autoSelect()
        || (!request.autoSelect() && !Objects.equals(booking.getRoomId(), request.roomId()))
        || !Objects.equals(booking.getStartDate(), request.startDate())
        || !Objects.equals(booking.getEndDate(), request.endDate())) {
      throw new ConflictException("RequestId was already used with different booking parameters");
    }
  }

  private String hotelRequestId(Long bookingId) {
    return "booking-" + bookingId;
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

  private ServiceUnavailableException unavailable(Throwable cause) {
    return new ServiceUnavailableException("Hotel Service is unavailable", cause);
  }
}
