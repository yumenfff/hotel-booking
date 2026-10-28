package com.hotelbooking.hotel.repository;

import com.hotelbooking.hotel.entity.Hotel;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {

  List<Hotel> findByCityIgnoreCase(String city, Sort sort);
}
