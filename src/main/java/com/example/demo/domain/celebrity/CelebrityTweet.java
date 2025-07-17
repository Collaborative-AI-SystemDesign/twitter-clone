// src/main/java/com/example/demo/domain/celebrity/CelebrityTweet.java
package com.example.demo.domain.celebrity;

import com.example.demo.domain.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.core.mapping.Column;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("celebrity_tweets")
@Getter
@Setter
@NoArgsConstructor
public class CelebrityTweet extends BaseEntity {

    /**
     * 복합 Primary Key
     * - 인플루언서 ID + 시간 + 트윗ID 조합
     */
    @PrimaryKey
    private CelebrityTweetKey key;

    /**
     * 트윗 내용
     * - 인플루언서가 작성한 실제 트윗 텍스트
     * - tweets 테이블의 원본 데이터를 복사하여 저장 (비정규화)
     * - 타임라인 병합 시 추가 조회 없이 모든 정보 제공
     */
    @Column("tweet_text")
    private String tweetText;

    /**
     * 인플루언서 트윗 생성 생성자
     * @param authorId 인플루언서 ID (파티션 키가 됨)
     * @param tweetId 트윗 고유 ID (tweets 테이블과 연결)
     * @param tweetText 트윗 내용
     */
    public CelebrityTweet(UUID authorId, UUID tweetId, String tweetText) {
        this.key = new CelebrityTweetKey(authorId, LocalDateTime.now(), tweetId);
        this.tweetText = tweetText;
        // createdAt, modifiedAt은 BaseEntity에서 자동 관리
    }
}