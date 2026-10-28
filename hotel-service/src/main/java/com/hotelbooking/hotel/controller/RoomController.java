package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.RoomRequest;
import com.hotelbooking.hotel.dto.RoomResponse;
import com.hotelbooking.hotel.dto.RoomSearchCriteria;
import com.hotelbooking.hotel.service.RoomService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

  private final RoomService roomService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public RoomResponse create(@Valid @RequestBody RoomRequest request) {
    return roomService.create(request);
  }

  @GetMapping
  public List<RoomResponse> findAvailable(RoomSearchCriteria criteria) {
    return roomService.findAvailable(criteria);
  }

  @GetMapping("/{id}")
  public RoomResponse getById(@PathVariable Long id) {
    return roomService.getById(id);
  }

  @PutMapping("/{id}")
  public RoomResponse update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
    return roomService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    roomService.delete(id);
  }
}
