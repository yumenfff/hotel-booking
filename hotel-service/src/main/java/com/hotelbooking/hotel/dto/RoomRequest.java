package com.hotelbooking.hotel.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RoomRequest(
    @NotNull Long hotelId,
    @NotBlank String number,
    Boolean available,
    @NotNull @Min(1) Integer capacity,
    @NotNull @DecimalMin("0.0") BigDecimal pricePerNight) {}
