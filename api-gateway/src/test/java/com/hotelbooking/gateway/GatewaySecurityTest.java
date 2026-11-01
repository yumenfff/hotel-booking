package com.hotelbooking.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewaySecurityTest {

  @Autowired private WebTestClient webTestClient;

  @Test
  void protectedEndpointWithoutToken_returns401() {
    webTestClient
        .get()
        .uri("/api/hotels")
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .exists("X-Trace-Id")
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(401);
  }

  @Test
  void internalHotelEndpoint_isBlocked() {
    webTestClient
        .post()
        .uri("/api/rooms/1/confirm-availability")
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectHeader()
        .exists("X-Trace-Id");
  }

  @Test
  void suppliedTraceId_isReturned() {
    webTestClient
        .get()
        .uri("/api/hotels")
        .header("X-Trace-Id", "test-trace-id")
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .valueEquals("X-Trace-Id", "test-trace-id");
  }
}
