package com.hotelbooking.booking.service;

import com.hotelbooking.booking.dto.BookingCreateRequest;
import com.hotelbooking.booking.dto.BookingResponse;
import com.hotelbooking.booking.dto.PageResponse;
import com.hotelbooking.booking.entity.BookingStatus;
import org.springframework.security.oauth2.jwt.Jwt;

public interface BookingService {

  BookingResponse create(BookingCreateRequest request, Jwt jwt);

  PageResponse<BookingResponse> findMine(int page, int size, BookingStatus status, Jwt jwt);

  BookingResponse getById(Long id, Jwt jwt);

  void cancel(Long id, Jwt jwt);
}
