package com.hotelbooking.booking;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotelbooking.booking.entity.Role;
import com.hotelbooking.booking.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class UserControllerTest extends AbstractIntegrationTest {

  private static final String NEW_USER =
      """
      {"username":"created","password":"secret123","role":"USER"}""";

  @Test
  void create_asAdmin_returns201AndHidesPassword() throws Exception {
    // given
    String token = adminToken();

    // when / then
    mockMvc
        .perform(
            post("/api/user")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(NEW_USER))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("created"))
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.password").doesNotExist());
  }

  @Test
  void create_asUser_returns403() throws Exception {
    // given
    String token = userToken();

    // when / then
    mockMvc
        .perform(
            post("/api/user")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(NEW_USER))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  void create_duplicateUsername_returns409() throws Exception {
    // given
    String token = adminToken();
    createUser("created", Role.USER);

    // when / then
    mockMvc
        .perform(
            post("/api/user")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(NEW_USER))
        .andExpect(status().isConflict());
  }

  @Test
  void create_withoutRole_returns400() throws Exception {
    String token = adminToken();

    mockMvc
        .perform(
            post("/api/user")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"created\",\"password\":\"secret123\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void findAll_asAdmin_returnsUsersOrderedById() throws Exception {
    // given
    String token = adminToken();
    createUser("second", Role.USER);

    // when / then
    mockMvc
        .perform(get("/api/user").header("Authorization", bearer(token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].username").value(contains("admin", "second")));
  }

  @Test
  void update_changesOnlyProvidedFields() throws Exception {
    // given
    String token = adminToken();
    User target = createUser("target", Role.USER);

    // when
    mockMvc
        .perform(
            patch("/api/user/{id}", target.getId())
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("ADMIN"))
        .andExpect(jsonPath("$.username").value("target"));
  }

  @Test
  void update_password_newPasswordWorksForLogin() throws Exception {
    // given
    String token = adminToken();
    User target = createUser("target", Role.USER);

    // when
    mockMvc
        .perform(
            patch("/api/user/{id}", target.getId())
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"brand-new-pass\"}"))
        .andExpect(status().isOk());

    // then
    mockMvc
        .perform(
            post("/api/user/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials("target", "brand-new-pass")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/user/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials("target", PASSWORD)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void update_unknownUser_returns404() throws Exception {
    String token = adminToken();

    mockMvc
        .perform(
            patch("/api/user/{id}", 999_999)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void delete_asAdmin_returns204AndUserIsGone() throws Exception {
    // given
    String token = adminToken();
    User target = createUser("target", Role.USER);

    // when / then
    mockMvc
        .perform(delete("/api/user/{id}", target.getId()).header("Authorization", bearer(token)))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(get("/api/user/{id}", target.getId()).header("Authorization", bearer(token)))
        .andExpect(status().isNotFound());
  }

  @Test
  void delete_asUser_returns403() throws Exception {
    // given
    String token = userToken();
    User target = createUser("target", Role.USER);

    // when / then
    mockMvc
        .perform(delete("/api/user/{id}", target.getId()).header("Authorization", bearer(token)))
        .andExpect(status().isForbidden());
  }
}
