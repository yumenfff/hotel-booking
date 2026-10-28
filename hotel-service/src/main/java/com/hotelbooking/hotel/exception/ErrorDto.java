package com.hotelbooking.hotel.exception;

import java.time.Instant;
import org.springframework.http.HttpStatus;

public record ErrorDto(Instant timestamp, int status, String error, String message, String path) {

  public static ErrorDto of(HttpStatus status, String message, String path) {
    return new ErrorDto(Instant.now(), status.value(), status.getReasonPhrase(), message, path);
  }
}
