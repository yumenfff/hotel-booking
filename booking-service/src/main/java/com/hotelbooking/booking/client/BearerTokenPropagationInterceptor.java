package com.hotelbooking.booking.client;

import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class BearerTokenPropagationInterceptor implements ClientHttpRequestInterceptor {

  private static final String TRACE_ID = "X-Trace-Id";

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtAuthentication
        && !request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
      request.getHeaders().setBearerAuth(jwtAuthentication.getToken().getTokenValue());
    }
    String traceId = MDC.get("traceId");
    if (traceId != null && !request.getHeaders().containsKey(TRACE_ID)) {
      request.getHeaders().set(TRACE_ID, traceId);
    }
    return execution.execute(request, body);
  }
}
