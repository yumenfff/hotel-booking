package com.hotelbooking.booking.config;

import com.hotelbooking.booking.entity.Booking;
import com.hotelbooking.booking.entity.BookingStatus;
import com.hotelbooking.booking.entity.Role;
import com.hotelbooking.booking.entity.User;
import com.hotelbooking.booking.repository.BookingRepository;
import com.hotelbooking.booking.repository.UserRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

  private final UserRepository userRepository;
  private final BookingRepository bookingRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(ApplicationArguments args) {
    user("admin", "admin123", Role.ADMIN);
    User ivan = user("ivan", "user123", Role.USER);
    User maria = user("maria", "user123", Role.USER);
    booking(ivan, 1L, 5, 8, "seed-booking-1");
    booking(maria, 3L, 10, 12, "seed-booking-2");
    log.info("Booking Service demo data loaded: 3 users and 2 confirmed bookings");
  }

  private User user(String username, String rawPassword, Role role) {
    User user = new User();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(rawPassword));
    user.setRole(role);
    return userRepository.save(user);
  }

  private void booking(User user, Long roomId, int startOffset, int endOffset, String requestId) {
    Booking booking = new Booking();
    booking.setUser(user);
    booking.setRoomId(roomId);
    booking.setAutoSelect(false);
    booking.setStartDate(LocalDate.now().plusDays(startOffset));
    booking.setEndDate(LocalDate.now().plusDays(endOffset));
    booking.setStatus(BookingStatus.CONFIRMED);
    booking.setRequestId(requestId);
    bookingRepository.save(booking);
  }
}
