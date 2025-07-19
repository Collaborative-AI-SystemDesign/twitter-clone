package com.example.demo.domain.user.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateUserResponse {

  private UUID userId;
}
