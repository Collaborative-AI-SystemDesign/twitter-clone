package com.example.demo.domain.tweet.entity;

import com.example.demo.domain.CassandraBaseEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자별 트윗 보조 테이블
 * - 사용자 ID 기준으로 최신 트윗들을 빠르게 조회하기 위한 목적
 * - 정렬/커서 기반 페이지네이션 등을 지원
 * - tweets 테이블의 원본 일부를 비정규화하여 포함
 */
@Table("tweets_by_user")
@Getter
@Setter
@NoArgsConstructor
public class TweetByUser extends CassandraBaseEntity {

    /**
     * 복합 키 (user_id, created_at, tweet_id)
     */
    @PrimaryKey
    private TweetByUserKey key;

    /**
     * 트윗 본문
     */
    @Column("tweet_text")
    private String tweetText;

    /**
     * 사용자별 트윗 객체 생성자
     * @param userId 작성자 ID
     * @param tweetId 트윗 고유 ID
     * @param tweetText 트윗 내용
     * @param createdAt 생성 시간
     */
    @Builder
    public TweetByUser(UUID userId, UUID tweetId, String tweetText, LocalDateTime createdAt) {
        this.key = new TweetByUserKey(userId, createdAt, tweetId);
        this.tweetText = tweetText;
        this.setCreatedAt(createdAt);
    }

    /**
     * 작성자 ID 조회 (키에서 가져옴)
     * @return 작성자 ID, key가 null인 경우 예외 발생
     * @throws IllegalStateException key가 초기화되지 않은 경우
     */
    public UUID getAuthorId() {
        if (this.key == null) {
            throw new IllegalStateException("TweetByUserKey가 초기화되지 않았습니다.");
        }
        return this.key.getUserId();
    }
}
