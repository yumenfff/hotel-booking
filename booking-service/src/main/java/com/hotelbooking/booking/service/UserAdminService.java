package com.hotelbooking.booking.service;

import com.hotelbooking.booking.dto.UserCreateRequest;
import com.hotelbooking.booking.dto.UserResponse;
import com.hotelbooking.booking.dto.UserUpdateRequest;
import java.util.List;

public interface UserAdminService {

  UserResponse create(UserCreateRequest request);

  List<UserResponse> findAll();

  UserResponse getById(Long id);

  UserResponse update(Long id, UserUpdateRequest request);

  void delete(Long id);
}
