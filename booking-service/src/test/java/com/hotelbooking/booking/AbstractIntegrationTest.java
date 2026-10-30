package com.hotelbooking.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hotelbooking.booking.entity.Role;
import com.hotelbooking.booking.entity.User;
import com.hotelbooking.booking.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

  protected static final String PASSWORD = "secret123";

  @Autowired protected MockMvc mockMvc;
  @Autowired protected UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @BeforeEach
  protected void cleanDatabase() {
    userRepository.deleteAll();
  }

  protected User createUser(String username, Role role) {
    User user = new User();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(PASSWORD));
    user.setRole(role);
    return userRepository.save(user);
  }

  protected String loginAndGetToken(String username) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/user/auth")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(credentials(username, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.token");
  }

  protected String adminToken() throws Exception {
    createUser("admin", Role.ADMIN);
    return loginAndGetToken("admin");
  }

  protected String userToken() throws Exception {
    createUser("regular", Role.USER);
    return loginAndGetToken("regular");
  }

  protected static String bearer(String token) {
    return "Bearer " + token;
  }

  protected static String credentials(String username, String password) {
    return "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password);
  }
}
