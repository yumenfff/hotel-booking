package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.dto.AvailabilityRequest;
import com.hotelbooking.hotel.dto.ReleaseRequest;
import com.hotelbooking.hotel.dto.ReservationResponse;
import com.hotelbooking.hotel.entity.ReservationStatus;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.entity.RoomReservation;
import com.hotelbooking.hotel.exception.BadRequestException;
import com.hotelbooking.hotel.exception.ConflictException;
import com.hotelbooking.hotel.exception.NotFoundException;
import com.hotelbooking.hotel.mapper.ReservationMapper;
import com.hotelbooking.hotel.repository.RoomRepository;
import com.hotelbooking.hotel.repository.RoomReservationRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomReservationServiceImpl implements RoomReservationService {

  private final RoomRepository roomRepository;
  private final RoomReservationRepository reservationRepository;
  private final ReservationMapper reservationMapper;

  @Override
  @Transactional
  public ReservationResponse confirmAvailability(Long roomId, AvailabilityRequest request) {
    if (!request.startDate().isBefore(request.endDate())) {
      throw new BadRequestException("startDate must be before endDate");
    }
    Room room =
        roomRepository
            .findByIdForUpdate(roomId)
            .orElseThrow(() -> new NotFoundException("Room " + roomId + " not found"));

    Optional<RoomReservation> existing = reservationRepository.findByRequestId(request.requestId());
    if (existing.isPresent()) {
      return replay(existing.get(), roomId, request);
    }
    if (!room.isAvailable()) {
      log.warn(
          "[bookingId={}, requestId={}] confirm rejected: room {} is out of service",
          request.bookingId(),
          request.requestId(),
          roomId);
      throw new ConflictException("Room " + roomId + " is not available");
    }
    if (reservationRepository.existsOverlap(roomId, request.startDate(), request.endDate())) {
      log.warn(
          "[bookingId={}, requestId={}] confirm rejected: room {} is busy for {}..{}",
          request.bookingId(),
          request.requestId(),
          roomId,
          request.startDate(),
          request.endDate());
      throw new ConflictException("Room " + roomId + " is already booked for the requested dates");
    }

    RoomReservation reservation = new RoomReservation();
    reservation.setRoom(room);
    reservation.setRequestId(request.requestId());
    reservation.setBookingId(request.bookingId());
    reservation.setStartDate(request.startDate());
    reservation.setEndDate(request.endDate());
    reservation.setStatus(ReservationStatus.HELD);
    reservationRepository.save(reservation);
    room.setTimesBooked(room.getTimesBooked() + 1);

    log.info(
        "[bookingId={}, requestId={}] slot HELD: room {} {}..{}, timesBooked={}",
        request.bookingId(),
        request.requestId(),
        roomId,
        request.startDate(),
        request.endDate(),
        room.getTimesBooked());
    return reservationMapper.toResponse(reservation);
  }

  @Override
  @Transactional
  public void release(Long roomId, ReleaseRequest request) {
    Room room =
        roomRepository
            .findByIdForUpdate(roomId)
            .orElseThrow(() -> new NotFoundException("Room " + roomId + " not found"));

    Optional<RoomReservation> existing = reservationRepository.findByRequestId(request.requestId());
    if (existing.isEmpty()) {
      log.info(
          "[requestId={}] release: nothing to release for room {}", request.requestId(), roomId);
      return;
    }
    RoomReservation reservation = existing.get();
    if (!reservation.getRoom().getId().equals(roomId)) {
      throw new ConflictException("requestId " + request.requestId() + " belongs to another room");
    }
    if (reservation.getStatus() == ReservationStatus.RELEASED) {
      log.info(
          "[bookingId={}, requestId={}] release: already RELEASED",
          reservation.getBookingId(),
          request.requestId());
      return;
    }
    reservation.setStatus(ReservationStatus.RELEASED);
    room.setTimesBooked(Math.max(0, room.getTimesBooked() - 1));
    log.info(
        "[bookingId={}, requestId={}] slot RELEASED: room {}, timesBooked={}",
        reservation.getBookingId(),
        request.requestId(),
        roomId,
        room.getTimesBooked());
  }

  private ReservationResponse replay(
      RoomReservation existing, Long roomId, AvailabilityRequest request) {
    boolean sameParams =
        existing.getRoom().getId().equals(roomId)
            && existing.getStartDate().equals(request.startDate())
            && existing.getEndDate().equals(request.endDate());
    if (!sameParams) {
      throw new ConflictException(
          "requestId " + request.requestId() + " was already used with other parameters");
    }
    if (existing.getStatus() == ReservationStatus.RELEASED) {
      throw new ConflictException("requestId " + request.requestId() + " was already released");
    }
    log.info(
        "[bookingId={}, requestId={}] confirm replayed (idempotent), no changes",
        request.bookingId(),
        request.requestId());
    return reservationMapper.toResponse(existing);
  }
}
