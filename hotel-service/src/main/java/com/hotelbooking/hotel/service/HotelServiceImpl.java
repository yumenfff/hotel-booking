package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.dto.HotelRequest;
import com.hotelbooking.hotel.dto.HotelResponse;
import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.exception.ConflictException;
import com.hotelbooking.hotel.exception.NotFoundException;
import com.hotelbooking.hotel.mapper.HotelMapper;
import com.hotelbooking.hotel.repository.HotelRepository;
import com.hotelbooking.hotel.repository.RoomRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotelServiceImpl implements HotelService {

  private final HotelRepository hotelRepository;
  private final RoomRepository roomRepository;
  private final HotelMapper hotelMapper;

  @Override
  @Transactional
  public HotelResponse create(HotelRequest request) {
    return hotelMapper.toResponse(hotelRepository.save(hotelMapper.toEntity(request)));
  }

  @Override
  public HotelResponse getById(Long id) {
    return hotelMapper.toResponse(findOrThrow(id));
  }

  @Override
  public List<HotelResponse> findAll(String city) {
    Sort sort = Sort.by("name");
    List<Hotel> hotels =
        (city == null || city.isBlank())
            ? hotelRepository.findAll(sort)
            : hotelRepository.findByCityIgnoreCase(city, sort);
    return hotels.stream().map(hotelMapper::toResponse).toList();
  }

  @Override
  @Transactional
  public HotelResponse update(Long id, HotelRequest request) {
    Hotel hotel = findOrThrow(id);
    hotelMapper.update(request, hotel);
    return hotelMapper.toResponse(hotel);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    Hotel hotel = findOrThrow(id);
    if (roomRepository.existsByHotelId(id)) {
      throw new ConflictException("Hotel " + id + " has rooms and cannot be deleted");
    }
    hotelRepository.delete(hotel);
  }

  private Hotel findOrThrow(Long id) {
    return hotelRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Hotel " + id + " not found"));
  }
}
