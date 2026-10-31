package com.hotelbooking.hotel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record RoomSearchCriteria(
    Long hotelId,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
    Integer minCapacity,
    BigDecimal maxPrice) {}
