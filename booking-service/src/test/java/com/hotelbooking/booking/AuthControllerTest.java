package com.hotelbooking.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotelbooking.booking.entity.Role;
import com.hotelbooking.booking.entity.User;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class AuthControllerTest extends AbstractIntegrationTest {

  @Autowired private JwtDecoder jwtDecoder;

  @Test
  void register_newUser_returns201WithTokenAndUserRole() throws Exception {
    // when / then
    mockMvc
        .perform(
            post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials("newbie", PASSWORD)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.tokenType").value("Bearer"));

    User saved = userRepository.findByUsername("newbie").orElseThrow();
    assertThat(saved.getRole()).isEqualTo(Role.USER);
    assertThat(saved.getPassword()).isNotEqualTo(PASSWORD).startsWith("$2");
  }

  @Test
  void register_duplicateUsername_returns409() throws Exception {
    // given
    createUser("taken", Role.USER);

    // when / then
    mockMvc
        .perform(
            post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials("taken", PASSWORD)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409));
  }

  @Test
  void register_tooShortPassword_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials("newbie", "123")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("password")));
  }

  @Test
  void login_validCredentials_returnsTokenWithRoleAndOneHourLifetime() throws Exception {
    // given
    createUser("ivan", Role.USER);

    // when
    String token = loginAndGetToken("ivan");

    // then
    Jwt jwt = jwtDecoder.decode(token);
    assertThat(jwt.getSubject()).isEqualTo("ivan");
    assertThat(jwt.<String>getClaim("role")).isEqualTo("USER");
    assertThat(jwt.<Long>getClaim("uid")).isNotNull();
    assertThat(jwt.getExpiresAt())
        .isBetween(Instant.now().plusSeconds(3500), Instant.now().plusSeconds(3700));
  }

  @Test
  void login_wrongPassword_returns401() throws Exception {
    // given
    createUser("ivan", Role.USER);

    // when / then
    mockMvc
        .perform(
            post("/api/user/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials("ivan", "wrong-password")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid username or password"));
  }

  @Test
  void login_unknownUser_returns401WithSameMessage() throws Exception {
    mockMvc
        .perform(
            post("/api/user/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials("ghost", PASSWORD)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid username or password"));
  }

  @Test
  void protectedEndpoint_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(get("/api/user"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void protectedEndpoint_withGarbageToken_returns401() throws Exception {
    mockMvc
        .perform(get("/api/user").header("Authorization", bearer("not-a-jwt")))
        .andExpect(status().isUnauthorized());
  }
}
