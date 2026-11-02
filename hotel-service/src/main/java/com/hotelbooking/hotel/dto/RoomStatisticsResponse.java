package com.hotelbooking.hotel.dto;

import java.time.LocalDate;

public record RoomStatisticsResponse(
    Long roomId,
    Long hotelId,
    String number,
    int timesBooked,
    int activeReservations,
    long bookedNights,
    double occupancyPercent,
    LocalDate periodFrom,
    LocalDate periodTo) {}
