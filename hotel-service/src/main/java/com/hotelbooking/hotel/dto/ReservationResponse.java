package com.hotelbooking.hotel.dto;

import com.hotelbooking.hotel.entity.ReservationStatus;
import java.time.LocalDate;

public record ReservationResponse(
    Long id,
    Long roomId,
    String requestId,
    Long bookingId,
    LocalDate startDate,
    LocalDate endDate,
    ReservationStatus status) {}
