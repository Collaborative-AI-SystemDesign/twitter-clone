package com.example.demo.domain2.tweet_r.entity;

import com.example.demo.domain.MySqlBaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RDB 버전 사용자별 트윗 엔터티
 * 사용자별 트윗 목록 조회 최적화를 위한 비정규화된 테이블
 */
@Entity
@Table(name = "tweets_by_user", indexes = {
    @Index(name = "idx_tweet_by_user_created", columnList = "userId, createdAt DESC")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TweetByUser extends MySqlBaseEntity {

    @EmbeddedId
    private TweetByUserKey key;

    @Column(name = "tweet_text", nullable = false, length = 280)
    private String tweetText;

    public UUID getUserId() {
        return key.getUserId();
    }

    public UUID getTweetId() {
        return key.getTweetId();
    }

    public LocalDateTime getCreatedAt() {
        return key.getCreatedAt();
    }
} 