package com.hotelbooking.booking.dto;

import com.hotelbooking.booking.entity.BookingStatus;
import java.time.Instant;
import java.time.LocalDate;

public record BookingResponse(
    Long id,
    Long userId,
    Long roomId,
    LocalDate startDate,
    LocalDate endDate,
    BookingStatus status,
    String requestId,
    String failureReason,
    Instant createdAt,
    Instant updatedAt) {}
