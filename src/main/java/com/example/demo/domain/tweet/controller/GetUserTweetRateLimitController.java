package com.example.demo.domain.tweet.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.domain.tweet.request.CreateTweetRequest;
import com.example.demo.domain.tweet.response.TweetListResponse;
import com.example.demo.domain.tweet.response.TweetResponse;
import com.example.demo.domain.tweet.service.TweetFanoutOnReadService;
import com.example.demo.domain.tweet.service.TweetService;
import com.example.demo.domain.tweet.service.TweetServiceAdvanced;
import com.example.demo.ratelimiter.annotation.RedisRateLimit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/tweets/{userId}/timeline")
@RequiredArgsConstructor
public class GetUserTweetRateLimitController {

    private final TweetFanoutOnReadService service;

    // 본인 트윗 조회 API의 5가지 알고리즘 적용

    // 타임라인 조회 fan-out on read 방식

    //token-bucket 알고리즘
    @GetMapping("/token-bucket")
    @RedisRateLimit(
            algorithm = RedisRateLimit.RedisAlgorithmType.REDIS_TOKEN_BUCKET,
            limit = 50,
            refillRate = 10,
            message = "Token Bucket 제한 초과! 잠시 후 다시 시도해주세요."
    )
    public ApiResponse<TweetListResponse> getTimelineByTokenBucket(
            @PathVariable UUID userId,
            @RequestParam(required = false) LocalDateTime cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        TweetListResponse res = service.getTimeline(userId, cursor, size);
        return ApiResponse.success("타임라인 조회 완료", res);
    }


    // leaky-bucket 알고리즘
    @GetMapping("/leaky-bucket")
    @RedisRateLimit(
            algorithm = RedisRateLimit.RedisAlgorithmType.REDIS_LEAKY_BUCKET,
            limit = 50,
            refillRate = 10,
            message = "Leaky Bucket 제한 초과! 버킷이 가득 찼습니다."
    )
    public ApiResponse<TweetListResponse> getTimelineByLeakyBucket(
            @PathVariable UUID userId,
            @RequestParam(required = false) LocalDateTime cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        TweetListResponse res = service.getTimeline(userId, cursor, size);
        return ApiResponse.success("타임라인 조회 완료", res);
    }


    // fixed-window 알고리즘
    @GetMapping("/fixed-window")
    @RedisRateLimit(
            algorithm = RedisRateLimit.RedisAlgorithmType.REDIS_FIXED_WINDOW,
            limit = 50,
            refillRate = 10,
            message = "Fixed Window 제한 초과! 1분 후 다시 시도해주세요."
    )
    public ApiResponse<TweetListResponse> getTimelineByFixedWindow(
            @PathVariable UUID userId,
            @RequestParam(required = false) LocalDateTime cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        TweetListResponse res = service.getTimeline(userId, cursor, size);
        return ApiResponse.success("타임라인 조회 완료", res);
    }

    // sliding-window-log 알고리즘
    @GetMapping("/sliding-window-log")
    @RedisRateLimit(
            algorithm = RedisRateLimit.RedisAlgorithmType.REDIS_SLIDING_WINDOW_LOG,
            limit = 50,
            refillRate = 10,
            message = "Sliding Window Log 제한 초과! 정확한 시간 기반 제한입니다."
    )
    public ApiResponse<TweetListResponse> getTimelineBySlidingWindowLog(
            @PathVariable UUID userId,
            @RequestParam(required = false) LocalDateTime cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        TweetListResponse res = service.getTimeline(userId, cursor, size);
        return ApiResponse.success("타임라인 조회 완료", res);
    }

    // sliding-window-counter 알고리즘
    @GetMapping("/sliding-window-counter")
    @RedisRateLimit(
            algorithm = RedisRateLimit.RedisAlgorithmType.REDIS_SLIDING_WINDOW_COUNTER,
            limit = 50,
            refillRate = 10,
            message = "Sliding Window Counter 제한 초과! 근사치 기반 제한입니다."
    )
    public ApiResponse<TweetListResponse> getTimelineBySlidingWindowCounter(
            @PathVariable UUID userId,
            @RequestParam(required = false) LocalDateTime cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        TweetListResponse res = service.getTimeline(userId, cursor, size);
        return ApiResponse.success("타임라인 조회 완료", res);
    }
}
