package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.dto.RoomRequest;
import com.hotelbooking.hotel.dto.RoomResponse;
import com.hotelbooking.hotel.dto.RoomSearchCriteria;
import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.exception.NotFoundException;
import com.hotelbooking.hotel.mapper.RoomMapper;
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
public class RoomServiceImpl implements RoomService {

  private static final Sort BY_ID = Sort.by(Sort.Direction.ASC, "id");
  private static final Sort BY_TIMES_BOOKED_THEN_ID =
      Sort.by(Sort.Direction.ASC, "timesBooked").and(BY_ID);

  private final RoomRepository roomRepository;
  private final HotelRepository hotelRepository;
  private final RoomMapper roomMapper;

  @Override
  @Transactional
  public RoomResponse create(RoomRequest request) {
    Room room = roomMapper.toEntity(request);
    room.setHotel(findHotel(request.hotelId()));
    return roomMapper.toResponse(roomRepository.save(room));
  }

  @Override
  public RoomResponse getById(Long id) {
    return roomMapper.toResponse(findOrThrow(id));
  }

  @Override
  public List<RoomResponse> findAvailable(RoomSearchCriteria criteria) {
    return search(criteria, BY_ID);
  }

  @Override
  public List<RoomResponse> findRecommended(RoomSearchCriteria criteria) {
    return search(criteria, BY_TIMES_BOOKED_THEN_ID);
  }

  @Override
  @Transactional
  public RoomResponse update(Long id, RoomRequest request) {
    Room room = findOrThrow(id);
    roomMapper.update(request, room);
    room.setHotel(findHotel(request.hotelId()));
    return roomMapper.toResponse(room);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    roomRepository.delete(findOrThrow(id));
  }

  private List<RoomResponse> search(RoomSearchCriteria criteria, Sort sort) {
    return roomRepository
        .search(criteria.hotelId(), criteria.minCapacity(), criteria.maxPrice(), sort)
        .stream()
        .map(roomMapper::toResponse)
        .toList();
  }

  private Room findOrThrow(Long id) {
    return roomRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Room " + id + " not found"));
  }

  private Hotel findHotel(Long hotelId) {
    return hotelRepository
        .findById(hotelId)
        .orElseThrow(() -> new NotFoundException("Hotel " + hotelId + " not found"));
  }
}
