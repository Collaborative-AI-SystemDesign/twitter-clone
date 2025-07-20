package com.example.demo.domain.user.service;

import static com.example.demo.util.ShardUtil.selectShardKeyByUserId;

import com.example.demo.config.DataSourceConfig;
import com.example.demo.domain.user.User;
import com.example.demo.domain.user.UserRepository;
import com.example.demo.domain.user.request.CreatedUserRequest;
import com.example.demo.domain.user.response.CreateUserResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  public CreateUserResponse create(CreatedUserRequest request) {
    User create = new User(
        UUID.randomUUID(),
        request.getName(),
        request.getEmail(),
        "1234"
    );

    userRepository.findById(create.getUserId()).ifPresent(user1 -> {
      throw new RuntimeException("Duplicated User UUID");
    });

    User user = userRepository.save(create);

    return new CreateUserResponse(user.getUserId());
  }

  public CreateUserResponse shardCreate(CreatedUserRequest request) {
    UUID userId = UUID.randomUUID();
    String shardKey = selectShardKeyByUserId(userId);
    DataSourceConfig.setShard(shardKey);
    try {
      User create = new User(
          userId,
          request.getName(),
          request.getEmail(),
          "1234"
      );
      userRepository.findById(create.getUserId()).ifPresent(user1 -> {
        throw new RuntimeException("Duplicated User UUID");
      });
      User user = userRepository.save(create);
      return new CreateUserResponse(user.getUserId());
    } finally {
      DataSourceConfig.clearShard();
    }
  }
}
