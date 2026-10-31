package com.hotelbooking.hotel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotelbooking.hotel.entity.ReservationStatus;
import com.hotelbooking.hotel.entity.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class RoomReservationFlowTest extends AbstractIntegrationTest {

  private Room room;

  @BeforeEach
  void createRoomFixture() {
    room = createRoom(createHotel("Grand"), "101", 0);
  }

  @Test
  void confirm_freeRoom_holdsSlotAndIncrementsCounter() throws Exception {
    // when
    ResultActions result = confirm(room.getId(), "req-1", 1, "2030-01-10", "2030-01-12");

    // then
    result
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("HELD"))
        .andExpect(jsonPath("$.requestId").value("req-1"));
    assertThat(timesBooked(room)).isEqualTo(1);
  }

  @Test
  void confirm_sameRequestTwice_isIdempotent() throws Exception {
    // given
    confirm(room.getId(), "req-1", 1, "2030-01-10", "2030-01-12").andExpect(status().isOk());

    // when
    confirm(room.getId(), "req-1", 1, "2030-01-10", "2030-01-12").andExpect(status().isOk());

    // then
    assertThat(reservationRepository.count()).isEqualTo(1);
    assertThat(timesBooked(room)).isEqualTo(1);
  }

  @Test
  void confirm_overlappingDates_returns409AndChangesNothing() throws Exception {
    // given
    confirm(room.getId(), "req-1", 1, "2030-01-10", "2030-01-12").andExpect(status().isOk());

    // when / then
    confirm(room.getId(), "req-2", 2, "2030-01-11", "2030-01-13")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409));
    assertThat(reservationRepository.count()).isEqualTo(1);
    assertThat(timesBooked(room)).isEqualTo(1);
  }

  @Test
  void confirm_checkInOnPreviousCheckOutDay_isAllowed() throws Exception {
    // given
    confirm(room.getId(), "req-1", 1, "2030-01-10", "2030-01-12").andExpect(status().isOk());

    // when / then
    confirm(room.getId(), "req-2", 2, "2030-01-12", "2030-01-14").andExpect(status().isOk());
    assertThat(reservationRepository.count()).isEqualTo(2);
  }

  @Test
  void confirm_requestIdReusedWithOtherDates_returns409() throws Exception {
    // given
    confirm(room.getId(), "req-1", 1, "2030-01-10", "2030-01-12").andExpect(status().isOk());

    // when / then
    confirm(room.getId(), "req-1", 1, "2030-02-10", "2030-02-12").andExpect(status().isConflict());
  }

  @Test
  void confirm_roomOutOfService_returns409() throws Exception {
    // given
    Room broken = createRoom(hotelRepository.findAll().get(0), "102", 0, false);

    // when / then
    confirm(broken.getId(), "req-1", 1, "2030-01-10", "2030-01-12")
        .andExpect(status().isConflict());
  }

  @Test
  void confirm_startNotBeforeEnd_returns400() throws Exception {
    confirm(room.getId(), "req-1", 1, "2030-01-10", "2030-01-10")
        .andExpect(status().isBadRequest());
  }

  @Test
  void confirm_unknownRoom_returns404() throws Exception {
    confirm(999_999L, "req-1", 1, "2030-01-10", "2030-01-12").andExpect(status().isNotFound());
  }

  @Test
  void confirm_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/rooms/{id}/confirm-availability", room.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(confirmBody("req-1", 1, "2030-01-10", "2030-01-12")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void release_afterConfirm_freesSlotAndRestoresCounter() throws Exception {
    // given
    confirm(room.getId(), "req-1", 1, "2030-01-10", "2030-01-12").andExpect(status().isOk());

    // when
    release(room.getId(), "req-1").andExpect(status().isNoContent());

    // then
    assertThat(reservationRepository.findByRequestId("req-1").orElseThrow().getStatus())
        .isEqualTo(ReservationStatus.RELEASED);
    assertThat(timesBooked(room)).isZero();
    confirm(room.getId(), "req-2", 2, "2030-01-10", "2030-01-12").andExpect(status().isOk());
  }

  @Test
  void release_calledTwice_isIdempotent() throws Exception {
    // given
    confirm(room.getId(), "req-1", 1, "2030-01-10", "2030-01-12").andExpect(status().isOk());
    release(room.getId(), "req-1").andExpect(status().isNoContent());

    // when
    release(room.getId(), "req-1").andExpect(status().isNoContent());

    // then
    assertThat(timesBooked(room)).isZero();
  }

  @Test
  void release_unknownRequestId_returns204() throws Exception {
    release(room.getId(), "never-seen").andExpect(status().isNoContent());
  }

  private ResultActions confirm(
      Long roomId, String requestId, long bookingId, String start, String end) throws Exception {
    return mockMvc.perform(
        post("/api/rooms/{id}/confirm-availability", roomId)
            .with(user())
            .contentType(MediaType.APPLICATION_JSON)
            .content(confirmBody(requestId, bookingId, start, end)));
  }

  private ResultActions release(Long roomId, String requestId) throws Exception {
    return mockMvc.perform(
        post("/api/rooms/{id}/release", roomId)
            .with(user())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"requestId\":\"%s\"}".formatted(requestId)));
  }

  private static String confirmBody(String requestId, long bookingId, String start, String end) {
    return """
      {"requestId":"%s","bookingId":%d,"startDate":"%s","endDate":"%s"}"""
        .formatted(requestId, bookingId, start, end);
  }
}
