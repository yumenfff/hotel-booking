package com.hotelbooking.hotel.dto;

import java.math.BigDecimal;

public record RoomResponse(
    Long id,
    Long hotelId,
    String number,
    boolean available,
    int timesBooked,
    int capacity,
    BigDecimal pricePerNight) {}
