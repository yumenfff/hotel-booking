package com.hotelbooking.booking.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotelRestClient implements HotelClient {

  private static final ParameterizedTypeReference<List<RoomPayload>> ROOM_LIST =
      new ParameterizedTypeReference<>() {};

  private final RestClient restClient;

  @Override
  public List<Long> findRecommendedRoomIds(LocalDate startDate, LocalDate endDate) {
    List<RoomPayload> rooms =
        execute(
            "find recommended rooms " + startDate + ".." + endDate,
            () ->
                restClient
                    .get()
                    .uri(
                        uri ->
                            uri.path("/api/rooms/recommend")
                                .queryParam("startDate", startDate)
                                .queryParam("endDate", endDate)
                                .build())
                    .retrieve()
                    .body(ROOM_LIST));
    return rooms == null ? List.of() : rooms.stream().map(RoomPayload::id).toList();
  }

  @Override
  public void confirmAvailability(
      Long roomId, String requestId, Long bookingId, LocalDate startDate, LocalDate endDate) {
    execute(
        "confirm availability [roomId=%d, requestId=%s]".formatted(roomId, requestId),
        () ->
            restClient
                .post()
                .uri("/api/rooms/{id}/confirm-availability", roomId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AvailabilityPayload(requestId, bookingId, startDate, endDate))
                .retrieve()
                .toBodilessEntity());
  }

  @Override
  public void release(Long roomId, String requestId) {
    execute(
        "release [roomId=%d, requestId=%s]".formatted(roomId, requestId),
        () ->
            restClient
                .post()
                .uri("/api/rooms/{id}/release", roomId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ReleasePayload(requestId))
                .retrieve()
                .toBodilessEntity());
  }

  private <T> T execute(String operation, Supplier<T> call) {
    try {
      return call.get();
    } catch (HttpClientErrorException e) {
      int status = e.getStatusCode().value();
      log.warn("Hotel Service rejected: {} -> HTTP {}", operation, status);
      if (status == HttpStatus.CONFLICT.value()) {
        throw new HotelConflictException(
            "Hotel Service: room is not available for the requested dates");
      }
      throw new HotelRejectedException(
          status, "Hotel Service rejected the request with HTTP " + status);
    } catch (RestClientException | IllegalStateException e) {
      log.warn("Hotel Service call failed: {} -> {}", operation, e.toString());
      throw new HotelUnavailableException("Hotel Service is unavailable: " + operation, e);
    }
  }

  record AvailabilityPayload(
      String requestId, Long bookingId, LocalDate startDate, LocalDate endDate) {}

  record ReleasePayload(String requestId) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record RoomPayload(Long id) {}
}
