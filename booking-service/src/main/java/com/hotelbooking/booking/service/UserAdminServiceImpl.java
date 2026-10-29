package com.hotelbooking.booking.service;

import com.hotelbooking.booking.dto.UserCreateRequest;
import com.hotelbooking.booking.dto.UserResponse;
import com.hotelbooking.booking.dto.UserUpdateRequest;
import com.hotelbooking.booking.entity.User;
import com.hotelbooking.booking.exception.ConflictException;
import com.hotelbooking.booking.exception.NotFoundException;
import com.hotelbooking.booking.mapper.UserMapper;
import com.hotelbooking.booking.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAdminServiceImpl implements UserAdminService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;

  @Override
  @Transactional
  public UserResponse create(UserCreateRequest request) {
    ensureUsernameFree(request.username());
    User user = new User();
    user.setUsername(request.username());
    user.setPassword(passwordEncoder.encode(request.password()));
    user.setRole(request.role());
    userRepository.save(user);
    log.info(
        "Admin created user: id={}, username={}, role={}",
        user.getId(),
        user.getUsername(),
        user.getRole());
    return userMapper.toResponse(user);
  }

  @Override
  public List<UserResponse> findAll() {
    return userRepository.findAll(Sort.by("id")).stream().map(userMapper::toResponse).toList();
  }

  @Override
  public UserResponse getById(Long id) {
    return userMapper.toResponse(findOrThrow(id));
  }

  @Override
  @Transactional
  public UserResponse update(Long id, UserUpdateRequest request) {
    User user = findOrThrow(id);
    if (request.username() != null && !request.username().equals(user.getUsername())) {
      ensureUsernameFree(request.username());
      user.setUsername(request.username());
    }
    if (request.password() != null) {
      user.setPassword(passwordEncoder.encode(request.password()));
    }
    if (request.role() != null) {
      user.setRole(request.role());
    }
    log.info("Admin updated user: id={}", id);
    return userMapper.toResponse(user);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    userRepository.delete(findOrThrow(id));
    log.info("Admin deleted user: id={}", id);
  }

  private User findOrThrow(Long id) {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("User " + id + " not found"));
  }

  private void ensureUsernameFree(String username) {
    if (userRepository.existsByUsername(username)) {
      throw new ConflictException("Username '" + username + "' is already taken");
    }
  }
}
