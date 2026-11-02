package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.dto.RoomStatisticsResponse;
import com.hotelbooking.hotel.dto.StatisticsCriteria;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.entity.RoomReservation;
import com.hotelbooking.hotel.exception.BadRequestException;
import com.hotelbooking.hotel.repository.RoomRepository;
import com.hotelbooking.hotel.repository.RoomReservationRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomStatisticsServiceImpl implements RoomStatisticsService {

  private static final int DEFAULT_PERIOD_DAYS = 30;

  private final RoomRepository roomRepository;
  private final RoomReservationRepository reservationRepository;

  @Override
  public List<RoomStatisticsResponse> getRoomStatistics(StatisticsCriteria criteria) {
    LocalDate from = criteria.from() != null ? criteria.from() : LocalDate.now();
    LocalDate to = criteria.to() != null ? criteria.to() : from.plusDays(DEFAULT_PERIOD_DAYS);
    if (!from.isBefore(to)) {
      throw new BadRequestException("from must be before to");
    }
    long periodNights = ChronoUnit.DAYS.between(from, to);

    Sort byId = Sort.by("id");
    List<Room> rooms =
        criteria.hotelId() == null
            ? roomRepository.findAll(byId)
            : roomRepository.findByHotelId(criteria.hotelId(), byId);

    Map<Long, List<RoomReservation>> reservationsByRoom =
        reservationRepository.findHeldOverlapping(from, to, criteria.hotelId()).stream()
            .collect(Collectors.groupingBy(rr -> rr.getRoom().getId()));

    return rooms.stream()
        .map(
            room ->
                toResponse(
                    room,
                    reservationsByRoom.getOrDefault(room.getId(), List.of()),
                    from,
                    to,
                    periodNights))
        .sorted(
            Comparator.comparingInt(RoomStatisticsResponse::timesBooked)
                .reversed()
                .thenComparing(RoomStatisticsResponse::roomId))
        .toList();
  }

  private RoomStatisticsResponse toResponse(
      Room room,
      List<RoomReservation> reservations,
      LocalDate from,
      LocalDate to,
      long periodNights) {
    long bookedNights = reservations.stream().mapToLong(rr -> overlapNights(rr, from, to)).sum();
    double occupancy = Math.round(bookedNights * 1000.0 / periodNights) / 10.0;
    return new RoomStatisticsResponse(
        room.getId(),
        room.getHotel().getId(),
        room.getNumber(),
        room.getTimesBooked(),
        reservations.size(),
        bookedNights,
        occupancy,
        from,
        to);
  }

  private long overlapNights(RoomReservation rr, LocalDate from, LocalDate to) {
    LocalDate start = rr.getStartDate().isAfter(from) ? rr.getStartDate() : from;
    LocalDate end = rr.getEndDate().isBefore(to) ? rr.getEndDate() : to;
    return ChronoUnit.DAYS.between(start, end);
  }
}
