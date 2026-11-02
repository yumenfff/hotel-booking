package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.dto.RoomStatisticsResponse;
import com.hotelbooking.hotel.dto.StatisticsCriteria;
import java.util.List;

public interface RoomStatisticsService {

  List<RoomStatisticsResponse> getRoomStatistics(StatisticsCriteria criteria);
}
