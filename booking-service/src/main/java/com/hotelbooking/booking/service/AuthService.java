package com.hotelbooking.booking.service;

import com.hotelbooking.booking.dto.AuthRequest;
import com.hotelbooking.booking.dto.AuthResponse;
import com.hotelbooking.booking.dto.RegisterRequest;

public interface AuthService {

  AuthResponse register(RegisterRequest request);

  AuthResponse authenticate(AuthRequest request);
}
