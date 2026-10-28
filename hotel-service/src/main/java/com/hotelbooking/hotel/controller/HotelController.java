package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.HotelRequest;
import com.hotelbooking.hotel.dto.HotelResponse;
import com.hotelbooking.hotel.service.HotelService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
public class HotelController {

  private final HotelService hotelService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public HotelResponse create(@Valid @RequestBody HotelRequest request) {
    return hotelService.create(request);
  }

  @GetMapping
  public List<HotelResponse> findAll(@RequestParam(required = false) String city) {
    return hotelService.findAll(city);
  }

  @GetMapping("/{id}")
  public HotelResponse getById(@PathVariable Long id) {
    return hotelService.getById(id);
  }

  @PutMapping("/{id}")
  public HotelResponse update(@PathVariable Long id, @Valid @RequestBody HotelRequest request) {
    return hotelService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    hotelService.delete(id);
  }
}
