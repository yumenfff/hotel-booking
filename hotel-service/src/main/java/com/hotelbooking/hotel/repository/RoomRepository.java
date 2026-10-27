package com.hotelbooking.hotel.repository;

import com.hotelbooking.hotel.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {}
