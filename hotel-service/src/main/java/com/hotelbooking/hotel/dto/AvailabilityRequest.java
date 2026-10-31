package com.hotelbooking.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AvailabilityRequest(
    @NotBlank @Size(max = 64) String requestId,
    @NotNull Long bookingId,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate) {}
