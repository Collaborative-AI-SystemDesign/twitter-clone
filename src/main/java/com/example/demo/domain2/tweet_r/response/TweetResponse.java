package com.example.demo.domain2.tweet_r.response;

import com.example.demo.domain2.tweet_r.entity.Tweet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RDB 버전 트윗 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TweetResponse {

    private UUID tweetId;
    private UUID userId;
    private String tweetText;
    private LocalDateTime createdAt;

    /**
     * Tweet 엔터티로부터 TweetResponse 생성
     */
    public static TweetResponse of(Tweet tweet) {
        return new TweetResponse(
            tweet.getTweetId(),
            tweet.getUserId(),
            tweet.getTweetText(),
            tweet.getCreatedAt()
        );
    }
} 