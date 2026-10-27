package com.hotelbooking.hotel.mapper;

import com.hotelbooking.hotel.dto.HotelRequest;
import com.hotelbooking.hotel.dto.HotelResponse;
import com.hotelbooking.hotel.entity.Hotel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface HotelMapper {

  HotelResponse toResponse(Hotel hotel);

  @Mapping(target = "id", ignore = true)
  Hotel toEntity(HotelRequest request);

  @Mapping(target = "id", ignore = true)
  void update(HotelRequest request, @MappingTarget Hotel hotel);
}
