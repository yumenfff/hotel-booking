package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.dto.AvailabilityRequest;
import com.hotelbooking.hotel.dto.ReleaseRequest;
import com.hotelbooking.hotel.dto.ReservationResponse;

public interface RoomReservationService {

  ReservationResponse confirmAvailability(Long roomId, AvailabilityRequest request);

  void release(Long roomId, ReleaseRequest request);
}
