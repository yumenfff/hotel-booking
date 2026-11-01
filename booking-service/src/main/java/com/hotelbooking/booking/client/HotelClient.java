package com.hotelbooking.booking.client;

import java.time.LocalDate;
import java.util.List;

public interface HotelClient {

  List<Long> findRecommendedRoomIds(LocalDate startDate, LocalDate endDate);

  void confirmAvailability(
      Long roomId, String requestId, Long bookingId, LocalDate startDate, LocalDate endDate);

  void release(Long roomId, String requestId);
}
