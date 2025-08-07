package com.example.demo.domain.tweet.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.domain.tweet.response.TweetListResponse;
import com.example.demo.domain.tweet.service.TweetFanoutOnReadService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/tweets/fan-out-read")
@RequiredArgsConstructor
public class TweetFanOutReadController {

  private final TweetFanoutOnReadService service;

  @GetMapping("/{userId}")
  public ApiResponse<TweetListResponse> getTimeline(
      @PathVariable UUID userId,
      @RequestParam(required = false) LocalDateTime cursor,
      @RequestParam(defaultValue = "20") int size
  ) {
    TweetListResponse res = service.getTimeline(userId, cursor, size);
    return ApiResponse.success("타임라인 조회 완료", res);
  }
}
