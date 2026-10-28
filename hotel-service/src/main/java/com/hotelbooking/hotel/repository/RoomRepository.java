package com.hotelbooking.hotel.repository;

import com.hotelbooking.hotel.entity.Room;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long> {

  boolean existsByHotelId(Long hotelId);

  @Query(
      """

      select r from Room r
          where r.available = true
            and (:hotelId is null or r.hotel.id = :hotelId)
            and (:minCapacity is null or r.capacity >= :minCapacity)
            and (:maxPrice is null or r.pricePerNight <= :maxPrice)
          """)
  List<Room> search(
      @Param("hotelId") Long hotelId,
      @Param("minCapacity") Integer minCapacity,
      @Param("maxPrice") BigDecimal maxPrice,
      Sort sort);
}
