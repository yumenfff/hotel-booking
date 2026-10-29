package com.hotelbooking.booking.dto;

import com.hotelbooking.booking.entity.Role;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
    @Size(min = 3, max = 50) String username,
    @Size(min = 6, max = 100) String password,
    Role role) {}
