package com.hotelbooking.booking.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record BookingCreateRequest(
    @Size(max = 100) String requestId,
    @NotNull @Positive Long roomId,
    @NotNull @FutureOrPresent LocalDate startDate,
    @NotNull LocalDate endDate) {

  @AssertTrue(message = "endDate must be after startDate")
  public boolean isDateRangeValid() {
    return startDate == null || endDate == null || startDate.isBefore(endDate);
  }
}
