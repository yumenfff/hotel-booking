package com.hotelbooking.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.hotelbooking.booking.client.HotelClient;
import com.hotelbooking.booking.client.HotelConflictException;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.entity.Role;
import com.hotelbooking.booking.entity.User;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;

@Import(BookingConcurrencyTest.FakeHotelConfiguration.class)
class BookingConcurrencyTest extends AbstractIntegrationTest {

  private static final LocalDate START = LocalDate.of(2030, 1, 10);
  private static final LocalDate END = LocalDate.of(2030, 1, 12);

  @Autowired private FakeHotelClient fakeHotel;

  @AfterEach
  void resetFakeHotel() {
    fakeHotel.reset();
  }

  @Test
  void concurrentBookingsForOneRoom_confirmExactlyOne() throws Exception {
    // given
    fakeHotel.setRecommendedRooms(List.of(1L));
    List<String> tokens = createTokens(8, "single");
    ExecutorService executor = Executors.newFixedThreadPool(8);

    // when
    List<Future<Integer>> responses = submitBookings(executor, tokens, false, 1L, "single");

    // then
    executor.shutdown();
    assertThat(responses.stream().map(this::getStatus).toList())
        .containsExactlyInAnyOrder(201, 409, 409, 409, 409, 409, 409, 409);
    assertThat(
            bookingRepository.findAll().stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED))
        .hasSize(1);
  }

  @Test
  void concurrentAutoSelectUsesDifferentRooms() throws Exception {
    // given
    fakeHotel.setRecommendedRooms(List.of(1L, 2L, 3L));
    List<String> tokens = createTokens(8, "auto");
    ExecutorService executor = Executors.newFixedThreadPool(8);

    // when
    List<Future<Integer>> responses = submitBookings(executor, tokens, true, null, "auto");

    // then
    executor.shutdown();
    assertThat(responses.stream().map(this::getStatus).toList())
        .containsExactlyInAnyOrder(201, 201, 201, 409, 409, 409, 409, 409);
    assertThat(
            bookingRepository.findAll().stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                .map(booking -> booking.getRoomId())
                .distinct())
        .hasSize(3);
  }

  private List<String> createTokens(int count, String prefix) throws Exception {
    List<String> tokens = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      User user = createUser(prefix + i, Role.USER);
      tokens.add(loginAndGetToken(user.getUsername()));
    }
    return tokens;
  }

  private List<Future<Integer>> submitBookings(
      ExecutorService executor,
      List<String> tokens,
      boolean autoSelect,
      Long roomId,
      String requestPrefix) {
    return tokens.stream()
        .map(
            token ->
                executor.submit(
                    () ->
                        mockMvc
                            .perform(
                                post("/api/bookings")
                                    .header("Authorization", bearer(token))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(request(autoSelect, roomId, requestPrefix)))
                            .andReturn()
                            .getResponse()
                            .getStatus()))
        .toList();
  }

  private int getStatus(Future<Integer> response) {
    try {
      return response.get();
    } catch (Exception ex) {
      throw new AssertionError("Concurrent request failed", ex);
    }
  }

  private String request(boolean autoSelect, Long roomId, String requestPrefix) {
    String requestId =
        requestPrefix + "-" + Thread.currentThread().getId() + "-" + System.nanoTime();
    return """
      {"requestId":"%s","roomId":%s,"autoSelect":%s,"startDate":"%s","endDate":"%s"}
      """
        .formatted(requestId, roomId, autoSelect, START, END);
  }

  @TestConfiguration
  static class FakeHotelConfiguration {

    @Bean
    @Primary
    FakeHotelClient fakeHotelClient() {
      return new FakeHotelClient();
    }
  }

  static class FakeHotelClient implements HotelClient {

    private final Map<String, Reservation> reservations = new ConcurrentHashMap<>();
    private volatile List<Long> recommendedRooms = List.of();

    @Override
    public synchronized List<Long> findRecommendedRoomIds(LocalDate startDate, LocalDate endDate) {
      Set<Long> occupied =
          reservations.values().stream().map(Reservation::roomId).collect(Collectors.toSet());
      return recommendedRooms.stream().filter(roomId -> !occupied.contains(roomId)).toList();
    }

    @Override
    public synchronized void confirmAvailability(
        Long roomId, String requestId, Long bookingId, LocalDate startDate, LocalDate endDate) {
      Reservation existing = reservations.get(requestId);
      if (existing != null) {
        if (!existing.roomId().equals(roomId)
            || !existing.startDate().equals(startDate)
            || !existing.endDate().equals(endDate)) {
          throw new HotelConflictException("requestId parameters differ");
        }
        return;
      }
      boolean overlap =
          reservations.values().stream()
              .anyMatch(
                  reservation ->
                      reservation.roomId().equals(roomId)
                          && reservation.startDate().isBefore(endDate)
                          && reservation.endDate().isAfter(startDate));
      if (overlap) {
        throw new HotelConflictException("room is occupied");
      }
      reservations.put(requestId, new Reservation(roomId, startDate, endDate));
    }

    @Override
    public synchronized void release(Long roomId, String requestId) {
      reservations.remove(requestId);
    }

    void setRecommendedRooms(List<Long> rooms) {
      recommendedRooms = List.copyOf(rooms);
    }

    void reset() {
      reservations.clear();
      recommendedRooms = List.of();
    }

    private record Reservation(Long roomId, LocalDate startDate, LocalDate endDate) {}
  }
}
