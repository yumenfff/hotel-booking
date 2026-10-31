package com.hotelbooking.hotel;

import static org.assertj.core.api.Assertions.assertThat;

import com.hotelbooking.hotel.dto.AvailabilityRequest;
import com.hotelbooking.hotel.entity.Room;
import com.hotelbooking.hotel.exception.ConflictException;
import com.hotelbooking.hotel.service.RoomReservationService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ConcurrentBookingTest extends AbstractIntegrationTest {

  private static final int THREADS = 8;
  private static final LocalDate START = LocalDate.parse("2030-01-10");
  private static final LocalDate END = LocalDate.parse("2030-01-12");

  @Autowired private RoomReservationService reservationService;

  @Test
  void sameRoomSameDates_differentRequests_onlyOneSucceeds() throws Exception {
    // given
    Room room = createRoom(createHotel("Grand"), "101", 0);
    List<Callable<Boolean>> tasks =
        IntStream.range(0, THREADS)
            .<Callable<Boolean>>mapToObj(i -> () -> tryConfirm(room.getId(), "req-" + i, i))
            .toList();

    // when
    List<Boolean> results = runConcurrently(tasks);

    // then
    assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
    assertThat(reservationRepository.count()).isEqualTo(1);
    assertThat(timesBooked(room)).isEqualTo(1);
  }

  @Test
  void sameRequestDeliveredConcurrently_isProcessedOnce() throws Exception {
    // given
    Room room = createRoom(createHotel("Grand"), "101", 0);
    List<Callable<Boolean>> tasks =
        IntStream.range(0, THREADS)
            .<Callable<Boolean>>mapToObj(i -> () -> tryConfirm(room.getId(), "same-request", 1))
            .toList();

    // when
    List<Boolean> results = runConcurrently(tasks);

    // then
    assertThat(results).allMatch(Boolean::booleanValue);
    assertThat(reservationRepository.count()).isEqualTo(1);
    assertThat(timesBooked(room)).isEqualTo(1);
  }

  private boolean tryConfirm(Long roomId, String requestId, long bookingId) {
    try {
      reservationService.confirmAvailability(
          roomId, new AvailabilityRequest(requestId, bookingId, START, END));
      return true;
    } catch (ConflictException e) {
      return false;
    }
  }

  private List<Boolean> runConcurrently(List<Callable<Boolean>> tasks) throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
    CyclicBarrier barrier = new CyclicBarrier(tasks.size());
    try {
      List<Future<Boolean>> futures =
          tasks.stream()
              .map(
                  task ->
                      pool.submit(
                          () -> {
                            barrier.await();
                            return task.call();
                          }))
              .toList();
      List<Boolean> results = new ArrayList<>();
      for (Future<Boolean> future : futures) {
        results.add(future.get(30, TimeUnit.SECONDS));
      }
      return results;
    } finally {
      pool.shutdownNow();
    }
  }
}
