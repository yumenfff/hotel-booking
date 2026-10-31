package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.AvailabilityRequest;
import com.hotelbooking.hotel.dto.ReleaseRequest;
import com.hotelbooking.hotel.dto.ReservationResponse;
import com.hotelbooking.hotel.service.RoomReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(
    name = "Room reservations (internal)",
    description = "Шаги саги: блокировка слота и компенсация. Через Gateway не публикуются")
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomReservationController {

  private final RoomReservationService reservationService;

  @Operation(summary = "INTERNAL: temporarily lock the room for the dates (saga step 1)")
  @PostMapping("/{id}/confirm-availability")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public ReservationResponse confirmAvailability(
      @PathVariable Long id, @Valid @RequestBody AvailabilityRequest request) {
    return reservationService.confirmAvailability(id, request);
  }

  @Operation(summary = "INTERNAL: release the lock (saga compensation), idempotent")
  @PostMapping("/{id}/release")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public void release(@PathVariable Long id, @Valid @RequestBody ReleaseRequest request) {
    reservationService.release(id, request);
  }
}
