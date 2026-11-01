package com.hotelbooking.booking.client;

public class HotelUnavailableException extends RuntimeException {

  public HotelUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
