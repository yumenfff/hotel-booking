package com.hotelbooking.booking.controller;

import com.hotelbooking.booking.dto.UserCreateRequest;
import com.hotelbooking.booking.dto.UserResponse;
import com.hotelbooking.booking.dto.UserUpdateRequest;
import com.hotelbooking.booking.service.UserAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users (admin)", description = "User management, ADMIN only")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

  private final UserAdminService userAdminService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
    return userAdminService.create(request);
  }

  @GetMapping
  public List<UserResponse> findAll() {
    return userAdminService.findAll();
  }

  @GetMapping("/{id}")
  public UserResponse getById(@PathVariable Long id) {
    return userAdminService.getById(id);
  }

  @PatchMapping("/{id}")
  public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
    return userAdminService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    userAdminService.delete(id);
  }
}
