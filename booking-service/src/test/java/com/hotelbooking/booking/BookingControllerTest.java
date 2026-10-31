package com.hotelbooking.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.entity.Role;
import com.hotelbooking.booking.entity.User;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class BookingControllerTest extends AbstractIntegrationTest {

  @Test
  void create_validRequest_returnsConfirmedBooking() throws Exception {
    // given
    String token = userToken();

    // when / then
    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request(7L, "booking-1", 1, 3)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.roomId").value(7))
        .andExpect(jsonPath("$.requestId").value("booking-1"));
  }

  @Test
  void create_sameRequestId_returnsSameBookingWithoutDuplicate() throws Exception {
    // given
    String token = userToken();
    String body = request(7L, "idempotent", 1, 3);

    // when
    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());

    // then
    assertThat(bookingRepository.count()).isEqualTo(1);
  }

  @Test
  void create_requestIdReusedWithOtherDates_returns409() throws Exception {
    // given
    String token = userToken();
    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request(7L, "reused", 1, 3)))
        .andExpect(status().isCreated());

    // when / then
    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request(7L, "reused", 10, 12)))
        .andExpect(status().isConflict());
    assertThat(bookingRepository.count()).isEqualTo(1);
  }

  @Test
  void create_overlappingDatesForSameRoom_returns409() throws Exception {
    // given
    User owner = createUser("owner", Role.USER);
    saveBooking(owner, 7L, "existing", 1, 4);
    createUser("guest", Role.USER);
    String token = loginAndGetToken("guest");

    // when / then
    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request(7L, "overlap", 3, 6)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409));
    assertThat(bookingRepository.count()).isEqualTo(1);
  }

  @Test
  void create_invalidDates_returns400() throws Exception {
    // given
    String token = userToken();

    // when / then
    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request(7L, "invalid", 3, 1)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void findMine_returnsOnlyCurrentUsersBookings() throws Exception {
    // given
    User first = createUser("first", Role.USER);
    User second = createUser("second", Role.USER);
    saveBooking(first, 1L, "first-booking", 1, 3);
    saveBooking(second, 2L, "second-booking", 1, 3);
    String token = loginAndGetToken("first");

    // when / then
    mockMvc
        .perform(get("/api/bookings").header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].requestId").value("first-booking"));
  }

  @Test
  void findMine_invalidPagination_returns400WithErrorDto() throws Exception {
    // given
    String token = userToken();

    // when / then
    mockMvc
        .perform(get("/api/bookings?page=-1&size=0").header("Authorization", bearer(token)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.path").value("/api/bookings"));
  }

  @Test
  void getById_foreignBooking_returns403WithErrorDto() throws Exception {
    // given
    User owner = createUser("owner", Role.USER);
    User anotherUser = createUser("another", Role.USER);
    Booking booking = saveBooking(owner, 1L, "owner-booking", 1, 3);
    String token = loginAndGetToken(anotherUser.getUsername());

    // when / then
    mockMvc
        .perform(get("/api/bookings/{id}", booking.getId()).header("Authorization", bearer(token)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.path").value("/api/bookings/" + booking.getId()));
  }

  @Test
  void cancel_ownBooking_returns204AndStatusCancelled() throws Exception {
    // given
    User owner = createUser("cancel-owner", Role.USER);
    Booking booking = saveBooking(owner, 1L, "own-cancel", 1, 3);
    String token = loginAndGetToken(owner.getUsername());

    // when
    mockMvc
        .perform(
            delete("/api/bookings/{id}", booking.getId()).header("Authorization", bearer(token)))
        .andExpect(status().isNoContent());

    // then
    assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
        .isEqualTo(BookingStatus.CANCELLED);
  }

  @Test
  void cancel_foreignBooking_returns403AndKeepsBookingConfirmed() throws Exception {
    // given
    User owner = createUser("cancel-victim", Role.USER);
    User intruder = createUser("cancel-intruder", Role.USER);
    Booking booking = saveBooking(owner, 1L, "foreign-cancel", 1, 3);
    String token = loginAndGetToken(intruder.getUsername());

    // when / then
    mockMvc
        .perform(
            delete("/api/bookings/{id}", booking.getId()).header("Authorization", bearer(token)))
        .andExpect(status().isForbidden());
    assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
        .isEqualTo(BookingStatus.CONFIRMED);
  }

  private Booking saveBooking(
      User user, Long roomId, String requestId, int startOffset, int endOffset) {
    Booking booking = new Booking();
    booking.setUser(user);
    booking.setRoomId(roomId);
    booking.setStartDate(LocalDate.now().plusDays(startOffset));
    booking.setEndDate(LocalDate.now().plusDays(endOffset));
    booking.setStatus(BookingStatus.CONFIRMED);
    booking.setRequestId(requestId);
    return bookingRepository.save(booking);
  }

  private static String request(Long roomId, String requestId, int startOffset, int endOffset) {
    return """
      {"requestId":"%s","roomId":%s,"startDate":"%s","endDate":"%s"}
      """
        .formatted(
            requestId,
            roomId,
            LocalDate.now().plusDays(startOffset),
            LocalDate.now().plusDays(endOffset));
  }
}
