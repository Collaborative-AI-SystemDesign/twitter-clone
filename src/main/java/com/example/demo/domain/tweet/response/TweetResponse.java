package com.example.demo.domain.tweet.response;

import com.example.demo.domain.tweet.entity.Tweet;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 트윗 응답 DTO
 * 
 * API 명세 대응:
 * - GET /tweets/{userId} 응답
 * - GET /timeline 응답의 개별 트윗 항목
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TweetResponse {
    
    /**
     * 트윗 고유 ID
     */
    private UUID tweetId;
    
    /**
     * 트윗 작성자 ID
     */
    private UUID userId;
    
    /**
     * 트윗 내용
     */
    private String content;
    
    /**
     * 트윗 생성 시간
     * ISO 8601 형식으로 직렬화됨 (예: "2025-07-10T20:14:30")
     */
    private LocalDateTime createdAt;

    public static TweetResponse of(Tweet tweet) {
        return TweetResponse.builder()
                .tweetId(tweet.getTweetId())
                .userId(tweet.getUserId())
                .content(tweet.getTweetText())
                .createdAt(tweet.getCreatedAt())
                .build();
    }
} 