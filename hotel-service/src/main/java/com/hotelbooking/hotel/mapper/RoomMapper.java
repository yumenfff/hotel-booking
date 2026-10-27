package com.hotelbooking.hotel.mapper;

import com.hotelbooking.hotel.dto.RoomRequest;
import com.hotelbooking.hotel.dto.RoomResponse;
import com.hotelbooking.hotel.entity.Room;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper
public interface RoomMapper {

  @Mapping(target = "hotelId", source = "hotel.id")
  RoomResponse toResponse(Room room);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "hotel", ignore = true)
  @Mapping(target = "timesBooked", ignore = true)
  Room toEntity(RoomRequest request);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "hotel", ignore = true)
  @Mapping(target = "timesBooked", ignore = true)
  void update(RoomRequest request, @MappingTarget Room room);
}
