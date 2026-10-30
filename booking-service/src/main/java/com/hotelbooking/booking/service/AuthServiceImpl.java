package com.hotelbooking.booking.service;

import com.hotelbooking.booking.dto.AuthRequest;
import com.hotelbooking.booking.dto.AuthResponse;
import com.hotelbooking.booking.dto.RegisterRequest;
import com.hotelbooking.booking.entity.Role;
import com.hotelbooking.booking.entity.User;
import com.hotelbooking.booking.exception.AuthenticationFailedException;
import com.hotelbooking.booking.exception.ConflictException;
import com.hotelbooking.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;

  @Override
  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByUsername(request.username())) {
      throw new ConflictException("Username '" + request.username() + "' is already taken");
    }
    User user = new User();
    user.setUsername(request.username());
    user.setPassword(passwordEncoder.encode(request.password()));
    user.setRole(Role.USER);
    userRepository.save(user);
    log.info("User registered: id={}, username={}", user.getId(), user.getUsername());
    return tokenService.issueToken(user);
  }

  @Override
  @Transactional(readOnly = true)
  public AuthResponse authenticate(AuthRequest request) {
    User user =
        userRepository
            .findByUsername(request.username())
            .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
            .orElseThrow(
                () -> {
                  log.warn("Failed login attempt for username={}", request.username());
                  return new AuthenticationFailedException("Invalid username or password");
                });
    log.info("User authenticated: id={}, username={}", user.getId(), user.getUsername());
    return tokenService.issueToken(user);
  }
}
