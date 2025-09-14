package com.example.demo.domain.tweet.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.domain.tweet.response.TweetListResponse;
import com.example.demo.domain.tweet.service.TweetService;
import com.example.demo.ratelimiter.annotation.RedisRateLimit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/tweets/{userId}")
@RequiredArgsConstructor
public class GetUserTimelineRateLimitController {

    private final TweetService tweetService;

    // 본인 트윗 조회 API의 5가지 알고리즘 적용

    //token-bucket 알고리즘
    @GetMapping("/token-bucket")
    @RedisRateLimit(
            algorithm = RedisRateLimit.RedisAlgorithmType.REDIS_TOKEN_BUCKET,
            limit = 50,
            refillRate = 10,
            message = "Token Bucket 제한 초과! 잠시 후 다시 시도해주세요."
    )
    public ApiResponse<TweetListResponse> getUserTweetsByTokenBucket(
            @PathVariable UUID userId,
            @RequestParam(value = "last", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastTimestamp,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        TweetListResponse response = tweetService.getUserTweets(userId, lastTimestamp, size);

        log.info("사용자 트윗 조회 API 완료 - userId: {}, 조회된 트윗 수: {}",
                userId, response.getTweets().size());

        return ApiResponse.success("사용자 트윗 조회가 완료되었습니다", response);
    }


    // leaky-bucket 알고리즘
    @GetMapping("/leaky-bucket")
    @RedisRateLimit(
            algorithm = RedisRateLimit.RedisAlgorithmType.REDIS_LEAKY_BUCKET,
            limit = 50,
            refillRate = 10,
            message = "Leaky Bucket 제한 초과! 버킷이 가득 찼습니다."
    )
    public ApiResponse<TweetListResponse> getUserTweetsByLeakyBucket(
            @PathVariable UUID userId,
            @RequestParam(value = "last", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastTimestamp,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        TweetListResponse response = tweetService.getUserTweets(userId, lastTimestamp, size);

        log.info("사용자 트윗 조회 API 완료 - userId: {}, 조회된 트윗 수: {}",
                userId, response.getTweets().size());

        return ApiResponse.success("사용자 트윗 조회가 완료되었습니다", response);
    }


    // fixed-window 알고리즘
    @GetMapping("/fixed-window")
    @RedisRateLimit(
            algorithm = RedisRateLimit.RedisAlgorithmType.REDIS_FIXED_WINDOW,
            limit = 50,
            refillRate = 10,
            message = "Fixed Window 제한 초과! 1분 후 다시 시도해주세요."
    )
    public ApiResponse<TweetListResponse> getUserTweetsByFixedWindow(
            @PathVariable UUID userId,
            @RequestParam(value = "last", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastTimestamp,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        TweetListResponse response = tweetService.getUserTweets(userId, lastTimestamp, size);

        log.info("사용자 트윗 조회 API 완료 - userId: {}, 조회된 트윗 수: {}",
                userId, response.getTweets().size());

        return ApiResponse.success("사용자 트윗 조회가 완료되었습니다", response);
    }

    // sliding-window-log 알고리즘
    @GetMapping("/sliding-window-log")
    @RedisRateLimit(
            algorithm = RedisRateLimit.RedisAlgorithmType.REDIS_SLIDING_WINDOW_LOG,
            limit = 50,
            refillRate = 10,
            message = "Sliding Window Log 제한 초과! 정확한 시간 기반 제한입니다."
    )
    public ApiResponse<TweetListResponse> getUserTweetsBySlidingWindowLog(
            @PathVariable UUID userId,
            @RequestParam(value = "last", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastTimestamp,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        TweetListResponse response = tweetService.getUserTweets(userId, lastTimestamp, size);

        log.info("사용자 트윗 조회 API 완료 - userId: {}, 조회된 트윗 수: {}",
                userId, response.getTweets().size());

        return ApiResponse.success("사용자 트윗 조회가 완료되었습니다", response);
    }

    // sliding-window-counter 알고리즘
    @GetMapping("/sliding-window-counter")
    @RedisRateLimit(
            algorithm = RedisRateLimit.RedisAlgorithmType.REDIS_SLIDING_WINDOW_COUNTER,
            limit = 50,
            refillRate = 10,
            message = "Sliding Window Counter 제한 초과! 근사치 기반 제한입니다."
    )
    public ApiResponse<TweetListResponse> getUserTweetsBySlidingWindowCounter(
            @PathVariable UUID userId,
            @RequestParam(value = "last", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastTimestamp,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        TweetListResponse response = tweetService.getUserTweets(userId, lastTimestamp, size);

        log.info("사용자 트윗 조회 API 완료 - userId: {}, 조회된 트윗 수: {}",
                userId, response.getTweets().size());

        return ApiResponse.success("사용자 트윗 조회가 완료되었습니다", response);
    }
}
