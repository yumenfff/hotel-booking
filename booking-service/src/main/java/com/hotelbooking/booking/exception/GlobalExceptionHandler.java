package com.hotelbooking.booking.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorDto> handleNotFound(NotFoundException ex, HttpServletRequest req) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorDto> handleConflict(ConflictException ex, HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), req);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorDto> handleIntegrity(
      DataIntegrityViolationException ex, HttpServletRequest req) {
    log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
    return build(
        HttpStatus.CONFLICT,
        "Operation violates data integrity (duplicate or referenced data)",
        req);
  }

  @ExceptionHandler({
    BadRequestException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ErrorDto> handleBadRequest(Exception ex, HttpServletRequest req) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorDto> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest req) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return build(HttpStatus.BAD_REQUEST, message, req);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorDto> handleOther(Exception ex, HttpServletRequest req) {
    if (ex instanceof ErrorResponse errorResponse) {
      return build(HttpStatus.valueOf(errorResponse.getStatusCode().value()), ex.getMessage(), req);
    }
    log.error("Unexpected error on {}", req.getRequestURI(), ex);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", req);
  }

  private ResponseEntity<ErrorDto> build(
      HttpStatus status, String message, HttpServletRequest req) {
    return ResponseEntity.status(status).body(ErrorDto.of(status, message, req.getRequestURI()));
  }
}
