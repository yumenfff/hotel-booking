package com.hotelbooking.hotel.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record StatisticsCriteria(
    Long hotelId,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {}
