package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.dto.HotelRequest;
import com.hotelbooking.hotel.dto.HotelResponse;
import java.util.List;

public interface HotelService {

  HotelResponse create(HotelRequest request);

  HotelResponse getById(Long id);

  List<HotelResponse> findAll(String city);

  HotelResponse update(Long id, HotelRequest request);

  void delete(Long id);
}
