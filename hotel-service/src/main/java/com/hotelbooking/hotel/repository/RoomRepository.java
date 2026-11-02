package com.hotelbooking.hotel.repository;

import com.hotelbooking.hotel.entity.Room;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long> {

  boolean existsByHotelId(Long hotelId);

  List<Room> findByHotelId(Long hotelId, Sort sort);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Room r where r.id = :id")
  Optional<Room> findByIdForUpdate(@Param("id") Long id);

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
