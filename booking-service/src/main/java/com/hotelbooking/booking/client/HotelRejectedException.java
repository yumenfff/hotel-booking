package com.hotelbooking.booking.client;

public class HotelRejectedException extends RuntimeException {

  private final int status;

  public HotelRejectedException(int status, String message) {
    super(message);
    this.status = status;
  }

  public int getStatus() {
    return status;
  }
}
