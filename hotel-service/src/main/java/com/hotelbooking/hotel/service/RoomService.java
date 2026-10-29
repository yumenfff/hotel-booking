package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.dto.RoomRequest;
import com.hotelbooking.hotel.dto.RoomResponse;
import com.hotelbooking.hotel.dto.RoomSearchCriteria;
import java.util.List;

public interface RoomService {

  RoomResponse create(RoomRequest request);

  RoomResponse getById(Long id);

  List<RoomResponse> findAvailable(RoomSearchCriteria criteria);

  List<RoomResponse> findRecommended(RoomSearchCriteria criteria);

  RoomResponse update(Long id, RoomRequest request);

  void delete(Long id);
}
