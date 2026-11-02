package com.hotelbooking.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotelbooking.booking.client.HotelClient;
import com.hotelbooking.booking.client.HotelConflictException;
import com.hotelbooking.booking.client.HotelRejectedException;
import com.hotelbooking.booking.client.HotelUnavailableException;
import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.entity.Role;
import com.hotelbooking.booking.entity.User;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class BookingSagaTest extends AbstractIntegrationTest {

  @MockitoBean private HotelClient hotelClient;

  @Test
  void create_success_confirmsBookingAfterHotelLock() throws Exception {
    // given
    String token = userToken();
    when(hotelClient.findRecommendedRoomIds(any(), any())).thenReturn(List.of(7L));

    // when / then
    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request(true, null, "saga-1")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.roomId").value(7));
    verify(hotelClient).confirmAvailability(any(), any(), any(), any(), any());
  }

  @Test
  void create_hotelConflict_cancelsBookingAndReturns409() throws Exception {
    // given
    String token = userToken();
    doThrow(new HotelConflictException("room is occupied"))
        .when(hotelClient)
        .confirmAvailability(any(), any(), any(), any(), any());

    // when / then
    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request(false, 7L, "saga-2")))
        .andExpect(status().isConflict());
    assertThat(bookingRepository.findAll())
        .singleElement()
        .extracting(Booking::getStatus)
        .isEqualTo(BookingStatus.CANCELLED);
    verify(hotelClient, never()).release(any(), any());
  }

  @Test
  void create_hotelUnavailable_compensatesAndReturns503() throws Exception {
    // given
    String token = userToken();
    doThrow(new HotelUnavailableException("timeout", new RuntimeException()))
        .when(hotelClient)
        .confirmAvailability(any(), any(), any(), any(), any());

    // when / then
    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request(false, 7L, "saga-3")))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value(503));
    assertThat(bookingRepository.findAll())
        .singleElement()
        .extracting(Booking::getStatus)
        .isEqualTo(BookingStatus.CANCELLED);
    verify(hotelClient).release(any(), any());
  }

  @Test
  void create_roomNotFoundInHotel_cancelsBookingAndReturns404() throws Exception {
    // given
    String token = userToken();
    doThrow(new HotelRejectedException(404, "room not found"))
        .when(hotelClient)
        .confirmAvailability(any(), any(), any(), any(), any());

    // when / then
    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request(false, 999L, "saga-4")))
        .andExpect(status().isNotFound());
    assertThat(bookingRepository.findAll())
        .singleElement()
        .extracting(Booking::getStatus)
        .isEqualTo(BookingStatus.CANCELLED);
  }

  @Test
  void create_sameRequestIdDelivered_callsHotelOnlyOnce() throws Exception {
    // given
    String token = userToken();
    when(hotelClient.findRecommendedRoomIds(any(), any())).thenReturn(List.of(7L));
    String body = request(true, null, "saga-5");

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
        .andExpect(status().isOk());

    // then
    verify(hotelClient).confirmAvailability(any(), any(), any(), any(), any());
    assertThat(bookingRepository.count()).isEqualTo(1);
  }

  @Test
  void create_autoSelect_triesNextCandidateAfterConflict() throws Exception {
    // given
    String token = userToken();
    when(hotelClient.findRecommendedRoomIds(any(), any())).thenReturn(List.of(7L, 8L));
    doThrow(new HotelConflictException("room is occupied"))
        .doNothing()
        .when(hotelClient)
        .confirmAvailability(any(), any(), any(), any(), any());

    // when / then
    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request(true, null, "saga-6")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.roomId").value(8));
  }

  @Test
  void create_autoSelectWithoutCandidates_returns409AndCreatesNothing() throws Exception {
    // given
    String token = userToken();
    when(hotelClient.findRecommendedRoomIds(any(), any())).thenReturn(List.of());

    // when / then
    mockMvc
        .perform(
            post("/api/bookings")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request(true, null, "saga-7")))
        .andExpect(status().isConflict());
    assertThat(bookingRepository.count()).isZero();
  }

  @Test
  void cancel_hotelUnavailable_keepsConfirmedAndReturns503() throws Exception {
    // given
    User owner = createUser("cancel-owner", Role.USER);
    Booking booking = saveConfirmedBooking(owner, 1L, "cancel-unavailable");
    String token = loginAndGetToken(owner.getUsername());
    doThrow(new HotelUnavailableException("hotel timeout", new RuntimeException()))
        .when(hotelClient)
        .release(booking.getRoomId(), "booking-" + booking.getId());

    // when / then
    mockMvc
        .perform(
            delete("/api/bookings/{id}", booking.getId()).header("Authorization", bearer(token)))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value(503));
    assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
        .isEqualTo(BookingStatus.CONFIRMED);
  }

  @Test
  void cancel_ownBooking_releasesRoomInHotel() throws Exception {
    // given
    User owner = createUser("release-owner", Role.USER);
    Booking booking = saveConfirmedBooking(owner, 1L, "cancel-release");
    String token = loginAndGetToken(owner.getUsername());

    // when
    mockMvc
        .perform(
            delete("/api/bookings/{id}", booking.getId()).header("Authorization", bearer(token)))
        .andExpect(status().isNoContent());

    // then
    verify(hotelClient).release(1L, "booking-" + booking.getId());
  }

  @Test
  void cancel_foreignBooking_doesNotTouchHotel() throws Exception {
    // given
    User owner = createUser("cancel-victim", Role.USER);
    User intruder = createUser("cancel-intruder", Role.USER);
    Booking booking = saveConfirmedBooking(owner, 1L, "foreign-cancel");
    String token = loginAndGetToken(intruder.getUsername());

    // when / then
    mockMvc
        .perform(
            delete("/api/bookings/{id}", booking.getId()).header("Authorization", bearer(token)))
        .andExpect(status().isForbidden());
    verify(hotelClient, never()).release(any(), any());
  }

  private Booking saveConfirmedBooking(User user, Long roomId, String requestId) {
    Booking booking = new Booking();
    booking.setUser(user);
    booking.setRoomId(roomId);
    booking.setAutoSelect(false);
    booking.setStartDate(LocalDate.now().plusDays(1));
    booking.setEndDate(LocalDate.now().plusDays(3));
    booking.setStatus(BookingStatus.CONFIRMED);
    booking.setRequestId(requestId);
    return bookingRepository.save(booking);
  }

  private static String request(boolean autoSelect, Long roomId, String requestId) {
    return """
      {"requestId":"%s","roomId":%s,"autoSelect":%s,"startDate":"%s","endDate":"%s"}
      """
        .formatted(
            requestId,
            roomId,
            autoSelect,
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(3));
  }
}
