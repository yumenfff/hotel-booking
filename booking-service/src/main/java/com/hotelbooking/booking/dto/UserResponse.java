package com.hotelbooking.booking.dto;

import com.hotelbooking.booking.entity.Role;
import java.time.Instant;

public record UserResponse(Long id, String username, Role role, Instant createdAt) {}
