package com.hotelbooking.hotel.repository;

import com.hotelbooking.hotel.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {}
