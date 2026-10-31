package com.hotelbooking.booking.mapper;

import com.hotelbooking.booking.dto.BookingResponse;
import com.hotelbooking.booking.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface BookingMapper {

  @Mapping(target = "userId", source = "user.id")
  BookingResponse toResponse(Booking booking);
}
