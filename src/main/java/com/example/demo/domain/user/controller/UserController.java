package com.example.demo.domain.user.controller;

import com.example.demo.domain.user.request.CreatedUserRequest;
import com.example.demo.domain.user.response.CreateUserResponse;
import com.example.demo.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/users")
@RestController
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @PostMapping()
  public CreateUserResponse createUser(@RequestBody CreatedUserRequest request) {
    return userService.create(request);
  }
}
