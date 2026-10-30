package com.hotelbooking.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayRoutingTest {

  private static final AtomicReference<String> receivedPath = new AtomicReference<>();
  private static HttpServer backend;

  @Autowired private WebTestClient webTestClient;

  @BeforeAll
  static void startBackend() throws IOException {
    backend = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    backend.createContext("/", GatewayRoutingTest::respond);
    backend.start();
  }

  @AfterAll
  static void stopBackend() {
    backend.stop(0);
  }

  @DynamicPropertySource
  static void gatewayProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.cloud.gateway.server.webflux.routes[0].uri",
        () -> "http://localhost:" + backend.getAddress().getPort());
    registry.add(
        "spring.cloud.gateway.server.webflux.routes[0].predicates[0]",
        () -> "Path=/api/bookings/**,/api/user/**");
  }

  @Test
  void publicBookingRoute_forwardsRequestToBookingBackend() {
    // when
    webTestClient
        .post()
        .uri("/api/user/auth")
        .bodyValue("{\"username\":\"ivan\",\"password\":\"user123\"}")
        .exchange()
        // then
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.routed")
        .isEqualTo(true);

    assertThat(receivedPath).hasValue("/api/user/auth");
  }

  private static void respond(HttpExchange exchange) throws IOException {
    receivedPath.set(exchange.getRequestURI().getPath());
    byte[] response = "{\"routed\":true}".getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, response.length);
    try (var output = exchange.getResponseBody()) {
      output.write(response);
    }
  }
}
