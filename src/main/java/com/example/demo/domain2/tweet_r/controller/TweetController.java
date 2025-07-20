package com.example.demo.domain2.tweet_r.controller;

import com.example.demo.common.ApiResponse;
import com.example.demo.domain2.tweet_r.service.TweetService;
import com.example.demo.domain2.tweet_r.request.CreateTweetRequest;
import com.example.demo.domain2.tweet_r.response.TweetResponse;
import com.example.demo.domain2.tweet_r.response.TweetListResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RDB 버전 트윗 API 컨트롤러
 * 
 * API 명세:
 * - POST /tweets-r: 새 트윗 생성
 * - GET /tweets-r/{userId}: 사용자 트윗 조회
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/tweets-r")
public class TweetController {

    private final TweetService tweetService;

    /**
     * 새 트윗 생성
     * 
     * POST /tweets-r
     * Header: Tweet-User-Id (현재 로그인한 사용자 ID)
     * Body: { "content": "트윗 내용" }
     */
    @PostMapping
    public ApiResponse<TweetResponse> createTweet(
            @RequestHeader("Tweet-User-Id") UUID userId,
            @Valid @RequestBody CreateTweetRequest createTweetRequest) {
        
        TweetResponse response = tweetService.createTweet(userId, createTweetRequest);
        
        log.info("RDB 트윗 생성 API 완료 - userId: {}, tweetId: {}", 
                userId, response.getTweetId());
        
        return ApiResponse.success("트윗이 성공적으로 생성되었습니다", response);
    }

    /**
     * 사용자 트윗 목록 조회
     * 
     * GET /tweets-r/{userId}?last={timestamp}&size={size}
     * 
     * @param userId 조회할 사용자 ID
     * @param lastTimestamp 마지막 트윗 시간 (커서 페이지네이션)
     * @param size 조회할 트윗 수 (기본값: 20, 최대: 50)
     * @return 트윗 목록
     */
    @GetMapping("/{userId}")
    public ApiResponse<TweetListResponse> getUserTweets(
            @PathVariable UUID userId,
            @RequestParam(value = "last", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastTimestamp,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        
        TweetListResponse response = tweetService.getUserTweets(userId, lastTimestamp, size);
        
        log.info("RDB 사용자 트윗 조회 API 완료 - userId: {}, 조회된 트윗 수: {}", 
                userId, response.getTweets().size());
        
        return ApiResponse.success("사용자 트윗 조회가 완료되었습니다", response);
    }
} 