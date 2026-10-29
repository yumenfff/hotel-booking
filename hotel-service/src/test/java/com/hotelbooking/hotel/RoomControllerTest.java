package com.hotelbooking.hotel;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotelbooking.hotel.entity.Hotel;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class RoomControllerTest extends AbstractIntegrationTest {

  @Test
  void recommend_sortsByTimesBookedThenById() throws Exception {
    // given
    Hotel hotel = createHotel("Grand");
    createRoom(hotel, "101", 5);
    createRoom(hotel, "102", 0);
    createRoom(hotel, "103", 3);
    createRoom(hotel, "104", 3);

    // when / then
    mockMvc
        .perform(get("/api/rooms/recommend").with(user()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].number").value(contains("102", "103", "104", "101")));
  }

  @Test
  void findAvailable_hasNoSpecialSorting_returnsById() throws Exception {
    // given
    Hotel hotel = createHotel("Grand");
    createRoom(hotel, "101", 5);
    createRoom(hotel, "102", 0);

    // when / then
    mockMvc
        .perform(get("/api/rooms").with(user()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].number").value(contains("101", "102")));
  }

  @Test
  void recommend_skipsRoomsOutOfService() throws Exception {
    // given
    Hotel hotel = createHotel("Grand");
    createRoom(hotel, "101", 0, false);
    createRoom(hotel, "102", 1, true);

    // when / then
    mockMvc
        .perform(get("/api/rooms/recommend").with(user()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].number").value("102"));
  }

  @Test
  void createRoom_asAdmin_returns201() throws Exception {
    // given
    Hotel hotel = createHotel("Grand");
    String body =
        """
        {"hotelId":%d,"number":"101","capacity":2,"pricePerNight":100.00}"""
            .formatted(hotel.getId());

    // when / then
    mockMvc
        .perform(
            post("/api/rooms").with(admin()).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.timesBooked").value(0));
  }

  @Test
  void createRoom_forUnknownHotel_returns404() throws Exception {
    String body =
        """
        {"hotelId":999999,"number":"101","capacity":2,"pricePerNight":100.00}""";

    mockMvc
        .perform(
            post("/api/rooms").with(admin()).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isNotFound());
  }

  @Test
  void createRoom_asUser_returns403() throws Exception {
    String body =
        """
        {"hotelId":1,"number":"101","capacity":2,"pricePerNight":100.00}""";

    mockMvc
        .perform(
            post("/api/rooms").with(user()).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isForbidden());
  }
}
