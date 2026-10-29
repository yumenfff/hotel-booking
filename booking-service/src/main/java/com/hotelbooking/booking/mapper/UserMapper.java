package com.hotelbooking.booking.mapper;

import com.hotelbooking.booking.dto.UserResponse;
import com.hotelbooking.booking.entity.User;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {

  UserResponse toResponse(User user);
}
