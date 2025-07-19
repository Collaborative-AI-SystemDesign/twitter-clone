package com.example.demo.domain.follow.controller;

import com.example.demo.domain.follow.FollowersByUser;
import com.example.demo.domain.follow.request.FollowRequest;
import com.example.demo.domain.follow.service.FollowService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/follows")
@RequiredArgsConstructor
public class FollowController {

  private final FollowService followService;

  @PostMapping("/{userId}")
  public ResponseEntity<FollowersByUser> followUser(@PathVariable("userId") UUID followedUserId, @RequestBody FollowRequest request) {
    return new ResponseEntity<>(
        followService.follow(followedUserId, request),
        HttpStatus.CREATED
    );
  }

  @DeleteMapping("/{userId}")
  public ResponseEntity<Void> unfollowUser(@PathVariable("userId") UUID followedUserId, @RequestBody FollowRequest request) {
    followService.unfollow(followedUserId, request);
    return ResponseEntity.ok().build();
  }
}
