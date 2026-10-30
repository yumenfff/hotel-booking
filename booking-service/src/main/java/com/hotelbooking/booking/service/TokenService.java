package com.hotelbooking.booking.service;

import com.hotelbooking.booking.dto.AuthResponse;
import com.hotelbooking.booking.entity.User;

public interface TokenService {

  AuthResponse issueToken(User user);
}
