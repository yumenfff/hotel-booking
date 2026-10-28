package com.hotelbooking.hotel.dto;

import java.math.BigDecimal;

public record RoomSearchCriteria(Long hotelId, Integer minCapacity, BigDecimal maxPrice) {}
