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
 * RDB 버전 트윗 엔터티
 * 원본 트윗 정보 저장
 */
@Entity
@Table(name = "tweets", indexes = {
    @Index(name = "idx_tweet_user_created", columnList = "userId, createdAt DESC"),
    @Index(name = "idx_tweet_created", columnList = "createdAt DESC")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tweet extends MySqlBaseEntity {

    @Id
    @Column(name = "tweet_id", columnDefinition = "BINARY(16)")
    private UUID tweetId;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "tweet_text", nullable = false, length = 280)
    private String tweetText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
} 