package com.hotelbooking.booking.controller;

import com.hotelbooking.booking.dto.BookingCreateRequest;
import com.hotelbooking.booking.dto.BookingResponse;
import com.hotelbooking.booking.dto.PageResponse;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.service.BookingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Bookings", description = "Booking creation and management")
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class BookingController {

  private final BookingService bookingService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BookingResponse create(
      @Valid @RequestBody BookingCreateRequest request, @AuthenticationPrincipal Jwt jwt) {
    return bookingService.create(request, jwt);
  }

  @GetMapping
  public PageResponse<BookingResponse> findMine(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) BookingStatus status,
      @AuthenticationPrincipal Jwt jwt) {
    return bookingService.findMine(page, size, status, jwt);
  }

  @GetMapping("/{id}")
  public BookingResponse getById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
    return bookingService.getById(id, jwt);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancel(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
    bookingService.cancel(id, jwt);
  }
}
