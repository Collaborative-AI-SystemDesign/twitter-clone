package com.example.demo.domain.follow.service;

import com.example.demo.domain.follow.FollowRepository;
import com.example.demo.domain.follow.FollowersByUser;
import com.example.demo.domain.follow.FollowersByUserKey;
import com.example.demo.domain.follow.request.FollowRequest;
import com.example.demo.domain.user.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FollowService {

  private final UserRepository userRepository;
  private final FollowRepository followRepository;

  public FollowersByUser follow(UUID followedUserId, FollowRequest request) {
    UUID followerId = request.getFollowerId();
    validateFollowUsers(followedUserId, followerId);

    FollowersByUser followersByUser = new FollowersByUser(
        followedUserId,
        followerId,
        LocalDateTime.now()
    );

    // 이미 팔로우하고 있는지 확인
    if (followRepository.findById(followersByUser.getKey()).isPresent()) {
      throw new RuntimeException("Already following this user.");
    }

    return followRepository.save(followersByUser);
  }

  public void unfollow(UUID followedUserId, FollowRequest request) {
    UUID followerId = request.getFollowerId();
    validateFollowUsers(followedUserId, followerId);

    FollowersByUserKey key = new FollowersByUserKey(followedUserId, followerId);

    if (followRepository.findById(key).isEmpty()) {
      // 팔로우하고 있지 않다면 예외 발생
      throw new RuntimeException("Not following this user.");
    }

    followRepository.deleteById(key);
  }

  private void validateFollowUsers(UUID followedUserId, UUID followerId) {
    // 팔로우 당하는 사용자 존재 여부 확인
    if (userRepository.findById(followedUserId).isEmpty()) {
      throw new RuntimeException("Followed user not found with ID: " + followedUserId);
    }

    // 팔로우 하는 사용자 존재 여부 확인
    if (userRepository.findById(followerId).isEmpty()) {
      throw new RuntimeException("Follower user not found with ID: " + followerId);
    }

    // 자기 자신을 팔로우하는지 확인
    if (followedUserId.equals(followerId)) {
      throw new RuntimeException("Cannot follow yourself.");
    }
  }
}
