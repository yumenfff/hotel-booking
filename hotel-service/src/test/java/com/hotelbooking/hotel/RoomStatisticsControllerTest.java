package com.hotelbooking.hotel;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotelbooking.hotel.entity.Hotel;
import com.hotelbooking.hotel.entity.Room;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RoomStatisticsControllerTest extends AbstractIntegrationTest {

  @Test
  void statistics_asAdmin_returnsOccupancyAndSortsByTimesBooked() throws Exception {
    // given
    Hotel hotel = createHotel("Grand");
    Room busy = createRoom(hotel, "101", 1);
    createRoom(hotel, "102", 5);
    holdRoom(busy, LocalDate.parse("2030-01-02"), LocalDate.parse("2030-01-05"), "req-1");

    // when / then
    mockMvc
        .perform(
            get("/api/rooms/statistics")
                .with(admin())
                .param("from", "2030-01-01")
                .param("to", "2030-01-11"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].number").value("102"))
        .andExpect(jsonPath("$[0].bookedNights").value(0))
        .andExpect(jsonPath("$[1].number").value("101"))
        .andExpect(jsonPath("$[1].bookedNights").value(3))
        .andExpect(jsonPath("$[1].occupancyPercent").value(30.0));
  }

  @Test
  void statistics_asUser_returns403() throws Exception {
    mockMvc.perform(get("/api/rooms/statistics").with(user())).andExpect(status().isForbidden());
  }

  @Test
  void statistics_invalidPeriod_returns400() throws Exception {
    mockMvc
        .perform(
            get("/api/rooms/statistics")
                .with(admin())
                .param("from", "2030-01-10")
                .param("to", "2030-01-10"))
        .andExpect(status().isBadRequest());
  }
}
