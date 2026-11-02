package com.hotelbooking.hotel.repository;

import com.hotelbooking.hotel.entity.RoomReservation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomReservationRepository extends JpaRepository<RoomReservation, Long> {

  Optional<RoomReservation> findByRequestId(String requestId);

  @Query(
      """

      select distinct rr.room.id from RoomReservation rr
          where rr.status = com.hotelbooking.hotel.entity.ReservationStatus.HELD
            and rr.startDate < :endDate
            and rr.endDate > :startDate
          """)
  List<Long> findBusyRoomIds(
      @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

  @Query(
      """
          select count(rr) > 0 from RoomReservation rr
          where rr.room.id = :roomId
            and rr.status = com.hotelbooking.hotel.entity.ReservationStatus.HELD
            and rr.startDate < :endDate
            and rr.endDate > :startDate
          """)
  boolean existsOverlap(
      @Param("roomId") Long roomId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query(
      """
          select rr from RoomReservation rr
          join fetch rr.room r
          where rr.status = com.hotelbooking.hotel.entity.ReservationStatus.HELD
            and rr.startDate < :to
            and rr.endDate > :from
            and (:hotelId is null or r.hotel.id = :hotelId)
          """)
  List<RoomReservation> findHeldOverlapping(
      @Param("from") LocalDate from, @Param("to") LocalDate to, @Param("hotelId") Long hotelId);
}
