package com.hotelbooking.gateway.filter;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class GatewayGlobalFilter implements WebFilter, Ordered {

  private static final Logger log = LoggerFactory.getLogger(GatewayGlobalFilter.class);
  private static final String TRACE_ID = "X-Trace-Id";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID);
    if (traceId == null || traceId.isBlank()) {
      traceId = UUID.randomUUID().toString();
    }
    final String requestTraceId = traceId;

    ServerHttpRequest request =
        exchange.getRequest().mutate().header(TRACE_ID, requestTraceId).build();
    ServerWebExchange tracedExchange = exchange.mutate().request(request).build();
    tracedExchange.getResponse().getHeaders().set(TRACE_ID, requestTraceId);

    if (isInternalHotelPath(request.getURI().getPath())) {
      log.warn(
          "Blocked internal Hotel endpoint: traceId={}, path={}",
          requestTraceId,
          request.getPath().value());
      tracedExchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
      return tracedExchange.getResponse().setComplete();
    }

    log.info(
        "Gateway request: traceId={}, method={}, path={}",
        requestTraceId,
        request.getMethod(),
        request.getPath().value());
    return chain
        .filter(tracedExchange)
        .doOnSuccess(
            unused ->
                log.info(
                    "Gateway response: traceId={}, status={}",
                    requestTraceId,
                    tracedExchange.getResponse().getStatusCode()));
  }

  private boolean isInternalHotelPath(String path) {
    return path.matches("/api/rooms/[^/]+/(confirm-availability|release)/?");
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
