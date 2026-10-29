package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.RoomRequest;
import com.hotelbooking.hotel.dto.RoomResponse;
import com.hotelbooking.hotel.dto.RoomSearchCriteria;
import com.hotelbooking.hotel.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Rooms", description = "CRUD номеров, список свободных и рекомендованных номеров")
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

  private final RoomService roomService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public RoomResponse create(@Valid @RequestBody RoomRequest request) {
    return roomService.create(request);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public List<RoomResponse> findAvailable(@ParameterObject RoomSearchCriteria criteria) {
    return roomService.findAvailable(criteria);
  }

  @Operation(summary = "Free rooms sorted by times_booked ascending, then by id")
  @GetMapping("/recommend")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public List<RoomResponse> findRecommended(@ParameterObject RoomSearchCriteria criteria) {
    return roomService.findRecommended(criteria);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('USER','ADMIN')")
  public RoomResponse getById(@PathVariable Long id) {
    return roomService.getById(id);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public RoomResponse update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
    return roomService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long id) {
    roomService.delete(id);
  }
}
