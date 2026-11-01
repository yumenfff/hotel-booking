package com.hotelbooking.booking.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
class HotelRestClientResilienceTest {

  private static final String CONFIRM_URL = "/api/rooms/1/confirm-availability";
  private static final String RELEASE_URL = "/api/rooms/1/release";
  private static final LocalDate START = LocalDate.parse("2030-01-10");
  private static final LocalDate END = LocalDate.parse("2030-01-12");

  @RegisterExtension
  static WireMockExtension hotel =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @DynamicPropertySource
  static void hotelProperties(DynamicPropertyRegistry registry) {
    registry.add("hotel.base-url", hotel::baseUrl);
    registry.add("hotel.read-timeout", () -> "300ms");
  }

  @Autowired private HotelClient hotelClient;
  @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;

  private CircuitBreaker circuitBreaker;

  @BeforeEach
  void setUp() {
    circuitBreaker = circuitBreakerRegistry.circuitBreaker("hotel");
    circuitBreaker.reset();
    Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "HS256").claim("sub", "ivan").build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
    MDC.clear();
  }

  @Test
  void confirmAvailability_success_sendsPayloadAndPropagatesToken() {
    // given
    MDC.put("traceId", "trace-test-1");
    hotel.stubFor(
        post(urlEqualTo(CONFIRM_URL))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"status\":\"HELD\"}")));

    // when
    hotelClient.confirmAvailability(1L, "booking-1", 1L, START, END);

    // then
    hotel.verify(
        1,
        postRequestedFor(urlEqualTo(CONFIRM_URL))
            .withHeader("Authorization", equalTo("Bearer test-token"))
            .withHeader("X-Trace-Id", equalTo("trace-test-1"))
            .withRequestBody(matchingJsonPath("$.requestId", equalTo("booking-1")))
            .withRequestBody(matchingJsonPath("$.bookingId", equalTo("1")))
            .withRequestBody(matchingJsonPath("$.startDate", equalTo("2030-01-10")))
            .withRequestBody(matchingJsonPath("$.endDate", equalTo("2030-01-12"))));
  }

  @Test
  void confirmAvailability_hotelReturns409_throwsConflictWithoutRetry() {
    // given
    hotel.stubFor(post(urlEqualTo(CONFIRM_URL)).willReturn(aResponse().withStatus(409)));

    // when / then
    assertThatThrownBy(() -> hotelClient.confirmAvailability(1L, "booking-1", 1L, START, END))
        .isInstanceOf(HotelConflictException.class);
    hotel.verify(1, postRequestedFor(urlEqualTo(CONFIRM_URL)));
  }

  @Test
  void confirmAvailability_hotelReturns404_throwsRejectedWithoutRetry() {
    // given
    hotel.stubFor(post(urlEqualTo(CONFIRM_URL)).willReturn(aResponse().withStatus(404)));

    // when / then
    assertThatThrownBy(() -> hotelClient.confirmAvailability(1L, "booking-1", 1L, START, END))
        .isInstanceOfSatisfying(
            HotelRejectedException.class, e -> assertThat(e.getStatus()).isEqualTo(404));
    hotel.verify(1, postRequestedFor(urlEqualTo(CONFIRM_URL)));
  }

  @Test
  void confirmAvailability_serverError_retriedThreeTimesThenUnavailable() {
    // given
    hotel.stubFor(post(urlEqualTo(CONFIRM_URL)).willReturn(aResponse().withStatus(503)));

    // when / then
    assertThatThrownBy(() -> hotelClient.confirmAvailability(1L, "booking-1", 1L, START, END))
        .isInstanceOf(HotelUnavailableException.class);
    hotel.verify(3, postRequestedFor(urlEqualTo(CONFIRM_URL)));
  }

  @Test
  void confirmAvailability_failsOnceThenSucceeds_isRetried() {
    // given
    hotel.stubFor(
        post(urlEqualTo(CONFIRM_URL))
            .inScenario("flaky")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("recovered"));
    hotel.stubFor(
        post(urlEqualTo(CONFIRM_URL))
            .inScenario("flaky")
            .whenScenarioStateIs("recovered")
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"status\":\"HELD\"}")));

    // when / then
    assertThatCode(() -> hotelClient.confirmAvailability(1L, "booking-1", 1L, START, END))
        .doesNotThrowAnyException();
    hotel.verify(2, postRequestedFor(urlEqualTo(CONFIRM_URL)));
  }

  @Test
  void confirmAvailability_slowResponse_timesOutAndRetries() {
    // given
    hotel.stubFor(
        post(urlEqualTo(CONFIRM_URL)).willReturn(aResponse().withStatus(200).withFixedDelay(1000)));

    // when / then
    assertThatThrownBy(() -> hotelClient.confirmAvailability(1L, "booking-1", 1L, START, END))
        .isInstanceOf(HotelUnavailableException.class);
    hotel.verify(3, postRequestedFor(urlEqualTo(CONFIRM_URL)));
  }

  @Test
  void circuitBreaker_opensAfterRepeatedFailures_failsFastWithoutCallingHotel() {
    // given
    hotel.stubFor(post(urlEqualTo(CONFIRM_URL)).willReturn(aResponse().withStatus(503)));

    // when
    for (int i = 0; i < 5 && circuitBreaker.getState() != CircuitBreaker.State.OPEN; i++) {
      assertThatThrownBy(() -> hotelClient.confirmAvailability(1L, "booking-1", 1L, START, END))
          .isInstanceOfAny(HotelUnavailableException.class, CallNotPermittedException.class);
    }
    int requestsBeforeShortCircuit = hotel.findAll(postRequestedFor(anyUrl())).size();

    // then
    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    assertThatThrownBy(() -> hotelClient.confirmAvailability(1L, "booking-1", 1L, START, END))
        .isInstanceOf(CallNotPermittedException.class);
    assertThat(hotel.findAll(postRequestedFor(anyUrl()))).hasSize(requestsBeforeShortCircuit);
  }

  @Test
  void release_isNotBlockedByOpenCircuitBreaker() {
    // given
    circuitBreaker.transitionToOpenState();
    hotel.stubFor(post(urlEqualTo(RELEASE_URL)).willReturn(aResponse().withStatus(204)));

    // when / then
    assertThatCode(() -> hotelClient.release(1L, "booking-1")).doesNotThrowAnyException();
    hotel.verify(
        1,
        postRequestedFor(urlEqualTo(RELEASE_URL))
            .withRequestBody(matchingJsonPath("$.requestId", equalTo("booking-1"))));
  }

  @Test
  void release_serverError_retriedFourTimesThenUnavailable() {
    // given
    hotel.stubFor(post(urlEqualTo(RELEASE_URL)).willReturn(aResponse().withStatus(500)));

    // when / then
    assertThatThrownBy(() -> hotelClient.release(1L, "booking-1"))
        .isInstanceOf(HotelUnavailableException.class);
    hotel.verify(4, postRequestedFor(urlEqualTo(RELEASE_URL)));
  }

  @Test
  void findRecommendedRoomIds_returnsIdsInHotelOrder() {
    // given
    hotel.stubFor(
        get(urlPathEqualTo("/api/rooms/recommend"))
            .withQueryParam("startDate", equalTo("2030-01-10"))
            .withQueryParam("endDate", equalTo("2030-01-12"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "[{\"id\":2,\"number\":\"102\",\"timesBooked\":0},"
                            + "{\"id\":5,\"number\":\"102\",\"timesBooked\":1}]")));

    // when
    List<Long> ids = hotelClient.findRecommendedRoomIds(START, END);

    // then
    assertThat(ids).containsExactly(2L, 5L);
    hotel.verify(
        getRequestedFor(urlPathEqualTo("/api/rooms/recommend"))
            .withHeader("Authorization", equalTo("Bearer test-token")));
  }
}
