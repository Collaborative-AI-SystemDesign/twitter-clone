package com.example.demo.domain2.tweet_r.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RDB 버전 트윗 목록 응답 DTO
 * 커서 기반 페이지네이션 지원
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TweetListResponse {

    private List<TweetResponse> tweets;
    private LocalDateTime nextCursor;
    private boolean hasNext;
} 