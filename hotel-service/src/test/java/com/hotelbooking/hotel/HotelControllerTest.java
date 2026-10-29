package com.hotelbooking.hotel;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotelbooking.hotel.entity.Hotel;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class HotelControllerTest extends AbstractIntegrationTest {

  private static final String VALID_BODY =
      """
      {"name":"Grand Hotel","address":"Tverskaya 1","city":"Moscow"}""";

  @Test
  void createHotel_asAdmin_returns201() throws Exception {
    // when / then
    mockMvc
        .perform(
            post("/api/hotels")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.name").value("Grand Hotel"));
  }

  @Test
  void createHotel_asUser_returns403() throws Exception {
    mockMvc
        .perform(
            post("/api/hotels")
                .with(user())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  void createHotel_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(post("/api/hotels").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void createHotel_withBlankName_returns400WithErrorDto() throws Exception {
    mockMvc
        .perform(
            post("/api/hotels")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.path").value("/api/hotels"));
  }

  @Test
  void getHotels_asUser_returnsAllHotels() throws Exception {
    // given
    createHotel("Hotel A");
    createHotel("Hotel B");

    // when / then
    mockMvc
        .perform(get("/api/hotels").with(user()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));
  }

  @Test
  void deleteHotel_withRooms_returns409() throws Exception {
    // given
    Hotel hotel = createHotel("Hotel A");
    createRoom(hotel, "101", 0);

    // when / then
    mockMvc
        .perform(delete("/api/hotels/{id}", hotel.getId()).with(admin()))
        .andExpect(status().isConflict());
  }

  @Test
  void getHotel_unknownId_returns404() throws Exception {
    mockMvc
        .perform(get("/api/hotels/{id}", 999_999).with(user()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }
}
