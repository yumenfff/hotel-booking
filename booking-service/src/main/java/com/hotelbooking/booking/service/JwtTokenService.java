package com.hotelbooking.booking.service;

import com.hotelbooking.booking.config.JwtProperties;
import com.hotelbooking.booking.dto.AuthResponse;
import com.hotelbooking.booking.entity.User;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtTokenService implements TokenService {

  private final JwtEncoder jwtEncoder;
  private final JwtProperties properties;

  @Override
  public AuthResponse issueToken(User user) {
    Duration lifetime = Duration.ofMinutes(properties.expirationMinutes());
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(user.getUsername())
            .issuedAt(now)
            .expiresAt(now.plus(lifetime))
            .claim("uid", user.getId())
            .claim("role", user.getRole().name())
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return AuthResponse.bearer(token, lifetime.toSeconds());
  }
}
