package com.example.demo.domain2.timeline_r.entity;

import com.example.demo.domain.MySqlBaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RDB 버전 사용자 타임라인 엔티티
 * Fan-out-on-write 전략으로 팔로워들의 타임라인에 트윗을 미리 저장
 */
@Entity
@Table(name = "user_timelines", indexes = {
    @Index(name = "idx_timeline_follower_created", columnList = "follower_id, created_at DESC"),
    @Index(name = "idx_timeline_author", columnList = "author_id"),
    @Index(name = "idx_timeline_tweet", columnList = "tweet_id")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTimeline extends MySqlBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    /**
     * 팔로워 ID (타임라인 소유자)
     */
    @Column(name = "follower_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID followerId;

    /**
     * 트윗 ID
     */
    @Column(name = "tweet_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID tweetId;

    /**
     * 트윗 작성자 ID
     */
    @Column(name = "author_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID authorId;

    /**
     * 트윗 내용 (비정규화된 데이터, 빠른 조회를 위해)
     */
    @Column(name = "tweet_text", nullable = false, length = 280)
    private String tweetText;

    /**
     * 트윗 생성 시간 (원본 시간)
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
} 