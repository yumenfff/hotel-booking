package com.hotelbooking.hotel.controller;

import com.hotelbooking.hotel.dto.RoomStatisticsResponse;
import com.hotelbooking.hotel.dto.StatisticsCriteria;
import com.hotelbooking.hotel.service.RoomStatisticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Statistics", description = "Статистика загруженности номеров (ADMIN)")
@RestController
@RequestMapping("/api/rooms/statistics")
@RequiredArgsConstructor
public class RoomStatisticsController {

  private final RoomStatisticsService statisticsService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public List<RoomStatisticsResponse> getStatistics(@ParameterObject StatisticsCriteria criteria) {
    return statisticsService.getRoomStatistics(criteria);
  }
}
