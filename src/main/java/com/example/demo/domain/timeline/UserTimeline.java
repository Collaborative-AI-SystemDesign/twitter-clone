package com.example.demo.domain.timeline;

import com.example.demo.domain.CassandraBaseEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.core.mapping.Column;

import java.time.LocalDateTime;
import java.util.UUID;

/**

 */
@Table("user_timeline")
@Getter
@Setter
@NoArgsConstructor
public class UserTimeline extends CassandraBaseEntity {

    /**
     * 복합 Primary Key
     * - 타임라인 소유자 + 시간 + 트윗ID 조합
     */
    @PrimaryKey
    private UserTimelineKey key;

    /**
     * 트윗 작성자 ID
     * - 이 트윗을 누가 작성했는지 식별
     * - 타임라인에서 "누구의 트윗인지" 표시하는 용도
     */
    @Column("author_id")
    private UUID authorId;

    /**
     * 트윗 내용
     * - 실제 트윗 텍스트 내용
     * - tweets 테이블의 원본 데이터를 복사하여 저장 (비정규화)
     * - 타임라인 조회 시 추가 조인 없이 모든 정보 제공
     */
    @Column("tweet_text")
    private String tweetText;

    /**
     * 타임라인 엔트리 생성 생성자
     * @param followerId 타임라인 소유자 ID (이 사용자의 타임라인에 추가됨)
     * @param tweetId 트윗 고유 ID
     * @param authorId 트윗 작성자 ID
     * @param tweetText 트윗 내용
     */
    @Builder
    public UserTimeline(UUID followerId, UUID tweetId, UUID authorId, String tweetText, LocalDateTime createdAt) {
        this.key = new UserTimelineKey(followerId, createdAt, tweetId);
        this.authorId = authorId;
        this.tweetText = tweetText;
        this.setCreatedAt(createdAt);
    }
}