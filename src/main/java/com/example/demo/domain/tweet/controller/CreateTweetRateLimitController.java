package com.example.demo.domain.tweet.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.domain.tweet.request.CreateTweetRequest;
import com.example.demo.domain.tweet.response.TweetResponse;
import com.example.demo.domain.tweet.service.TweetServiceAdvanced;
import com.example.demo.ratelimiter.annotation.RedisRateLimit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/tweets")
@RequiredArgsConstructor
public class CreateTweetRateLimitController {

    private final TweetServiceAdvanced service;

    //트윗 생성 API의 5가지 알고리즘 적용

    //token-bucket 알고리즘
    @PostMapping("/token-bucket")
    @RedisRateLimit(
            algorithm = RedisRateLimit.RedisAlgorithmType.REDIS_TOKEN_BUCKET,
            limit = 5,
            refillRate = 1,
            message = "Token Bucket 제한 초과! 잠시 후 다시 시도해주세요."
    )
    public ApiResponse<TweetResponse> createTweetByTokenBucket(
            @RequestHeader("Tweet-User-Id") UUID userId,
            @Valid @RequestBody CreateTweetRequest createTweetRequest) {

        TweetResponse response = service.createTweet(userId, createTweetRequest);

        log.info("트윗 생성 API 완료 (최적화 버전) - userId: {}, tweetId: {}",
                userId, response.getTweetId());

        return ApiResponse.success("트윗이 성공적으로 생성되었습니다 (최적화 버전)", response);
    }


    // leaky-bucket 알고리즘
    @PostMapping("/leaky-bucket")
    @RedisRateLimit(
            algorithm = RedisRateLimit.RedisAlgorithmType.REDIS_LEAKY_BUCKET,
            limit = 5,
            refillRate = 1,
            message = "Leaky Bucket 제한 초과! 버킷이 가득 찼습니다."
    )
    public ApiResponse<TweetResponse> createTweetByLeakyBucket(
            @RequestHeader("Tweet-User-Id") UUID userId,
            @Valid @RequestBody CreateTweetRequest createTweetRequest) {

        TweetResponse response = service.createTweet(userId, createTweetRequest);

        log.info("트윗 생성 API 완료 (최적화 버전) - userId: {}, tweetId: {}",
                userId, response.getTweetId());

        return ApiResponse.success("트윗이 성공적으로 생성되었습니다 (최적화 버전)", response);
    }


    // fixed-window 알고리즘
    @PostMapping("/fixed-window")
    @RedisRateLimit(
            algorithm = RedisRateLimit.RedisAlgorithmType.REDIS_FIXED_WINDOW,
            limit = 5,
            refillRate = 1,
            message = "Fixed Window 제한 초과! 1분 후 다시 시도해주세요."
    )
    public ApiResponse<TweetResponse> createTweetByFixedWindow(
            @RequestHeader("Tweet-User-Id") UUID userId,
            @Valid @RequestBody CreateTweetRequest createTweetRequest) {

        TweetResponse response = service.createTweet(userId, createTweetRequest);

        log.info("트윗 생성 API 완료 (최적화 버전) - userId: {}, tweetId: {}",
                userId, response.getTweetId());

        return ApiResponse.success("트윗이 성공적으로 생성되었습니다 (최적화 버전)", response);
    }

    // sliding-window-log 알고리즘
    @PostMapping("/sliding-window-log")
    @RedisRateLimit(
            algorithm = RedisRateLimit.RedisAlgorithmType.REDIS_SLIDING_WINDOW_LOG,
            limit = 5,
            refillRate = 1,
            message = "Sliding Window Log 제한 초과! 정확한 시간 기반 제한입니다."
    )
    public ApiResponse<TweetResponse> createTweetBySlidingWindowLog(
            @RequestHeader("Tweet-User-Id") UUID userId,
            @Valid @RequestBody CreateTweetRequest createTweetRequest) {

        TweetResponse response = service.createTweet(userId, createTweetRequest);

        log.info("트윗 생성 API 완료 (최적화 버전) - userId: {}, tweetId: {}",
                userId, response.getTweetId());

        return ApiResponse.success("트윗이 성공적으로 생성되었습니다 (최적화 버전)", response);
    }

    // sliding-window-counter 알고리즘
    @PostMapping("/sliding-window-counter")
    @RedisRateLimit(
            algorithm = RedisRateLimit.RedisAlgorithmType.REDIS_SLIDING_WINDOW_COUNTER,
            limit = 5,
            refillRate = 1,
            message = "Sliding Window Counter 제한 초과! 근사치 기반 제한입니다."
    )
    public ApiResponse<TweetResponse> createTweetBySlidingWindowCounter(
            @RequestHeader("Tweet-User-Id") UUID userId,
            @Valid @RequestBody CreateTweetRequest createTweetRequest) {

        TweetResponse response = service.createTweet(userId, createTweetRequest);

        log.info("트윗 생성 API 완료 (최적화 버전) - userId: {}, tweetId: {}",
                userId, response.getTweetId());

        return ApiResponse.success("트윗이 성공적으로 생성되었습니다 (최적화 버전)", response);
    }




    // 본인 트윗 조회 API의 5가지 알고리즘 적용
    // Token-Bucket 알고리즘
    // Leaky-Bucket 알고리즘
    // Fixed-Window 알고리즘
    // Sliding-Window-Log 알고리즘
    // Sliding-Window-Counter 알고리즘


    // 타임 라인 조회 API의 5가지 알고리즘 적용
    // Token-Bucket 알고리즘
    // Leaky-Bucket 알고리즘
    // Fixed-Window 알고리즘
    // Sliding-Window-Log 알고리즘
    // Sliding-Window-Counter 알고리즘
}
