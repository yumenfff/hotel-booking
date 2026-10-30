package com.hotelbooking.booking.exception;

public class AuthenticationFailedException extends RuntimeException {

  public AuthenticationFailedException(String message) {
    super(message);
  }
}
