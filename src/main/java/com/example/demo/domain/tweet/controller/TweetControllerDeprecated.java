package com.example.demo.domain.tweet.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.domain.tweet.service.TweetService;
import com.example.demo.domain.tweet.service.TweetServiceAdvanced;
import com.example.demo.domain.tweet.request.CreateTweetRequest;
import com.example.demo.domain.tweet.response.TweetResponse;
import com.example.demo.domain.tweet.response.TweetListResponse;
import jakarta.validation.Valid;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 트윗 API 컨트롤러
 *
 * API 명세:
 * - POST /tweets: 새 트윗 생성 (기존 버전)
 * - POST /tweets/optimized: 새 트윗 생성 (최적화 버전) 🚀
 * - GET /tweets/{userId}: 사용자 트윗 조회
 * - GET /tweets/{userId}/timeline: 사용자 타임라인 조회 (Cassandra 버전)
 * - GET /tweets/{userId}/timeline/optimized: 사용자 타임라인 조회 (Redis 최적화 버전) 🚀
 */
@Slf4j
@RestController
@RequestMapping("/v1/tweets")
public class TweetControllerDeprecated {

    private final TweetService tweetService;
    private final TweetServiceAdvanced tweetServiceAdvanced;

    public TweetControllerDeprecated(TweetService tweetService,
                                     @Qualifier("tweetServiceAdvanced") TweetServiceAdvanced tweetServiceAdvanced) {
        this.tweetService = tweetService;
        this.tweetServiceAdvanced = tweetServiceAdvanced;
    }

    /**
     * 새 트윗 생성 (기존 버전)
     *
     * POST /tweets
     * Header: Tweet-User-Id (현재 로그인한 사용자 ID)
     * Body: { "content": "트윗 내용" }
     */
//    @Deprecated
//    @PostMapping
//    public ApiResponse<TweetResponse> createTweet(
//            @RequestHeader("Tweet-User-Id") UUID userId,
//            @Valid @RequestBody CreateTweetRequest createTweetRequest) {
//
//        TweetResponse response = tweetService.createTweet(userId, createTweetRequest);
//
//        log.info("트윗 생성 API 완료 (기존 버전) - userId: {}, tweetId: {}",
//                userId, response.getTweetId());
//
//        return ApiResponse.success("트윗이 성공적으로 생성되었습니다", response);
//    }

    /**
     * 🔥 사용자 타임라인 조회 (Cassandra 직접 조회 버전)
     *
     * GET /tweets/{userId}/timeline?last={timestamp}&size={size}
     *
     * @param userId 타임라인을 조회할 사용자 ID
     * @param lastTimestamp 마지막 트윗 시간 (커서 페이지네이션)
     * @param size 조회할 트윗 수 (기본값: 20, 최대: 50)
     * @return 타임라인 트윗 목록
     */
//    @Deprecated
//    @GetMapping("/{userId}/timeline")
//    public ApiResponse<TweetListResponse> getUserTimeline(
//            @PathVariable UUID userId,
//            @RequestParam(value = "last", required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastTimestamp,
//            @RequestParam(value = "size", defaultValue = "20") int size) {
//
//        TweetListResponse response = tweetService.getUserTimeline(userId, lastTimestamp, size);
//
//        log.info("사용자 타임라인 조회 API 완료 (Cassandra 버전) - userId: {}, 조회된 트윗 수: {}",
//                userId, response.getTweets().size());
//
//        return ApiResponse.success("타임라인 조회가 완료되었습니다 (Cassandra 버전)", response);
//    }

    /**
     * 🚀 사용자 타임라인 조회 (Redis SortedSet 최적화 버전)
     *
     * GET /tweets/{userId}/timeline/optimized?last={timestamp}&size={size}
     *
     * @param userId 타임라인을 조회할 사용자 ID
     * @param lastTimestamp 마지막 트윗 시간 (커서 페이지네이션)
     * @param size 조회할 트윗 수 (기본값: 20, 최대: 50)
     * @return 타임라인 트윗 목록
     */
//    @Deprecated
//    @GetMapping("/{userId}/timeline")
//    public ApiResponse<TweetListResponse> getUserTimelineOptimized(
//            @PathVariable UUID userId,
//            @RequestParam(value = "last", required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastTimestamp,
//            @RequestParam(value = "size", defaultValue = "20") int size) {
//
//        TweetListResponse response = tweetServiceAdvanced.getUserTimelineOptimized(userId, lastTimestamp, size);
//
//        log.info("사용자 타임라인 조회 API 완료 (Redis 최적화 버전) - userId: {}, 조회된 트윗 수: {}",
//                userId, response.getTweets().size());
//
//        return ApiResponse.success("타임라인 조회가 완료되었습니다 (Redis 최적화 버전)", response);
//    }
}
