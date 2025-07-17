// src/main/java/com/example/demo/domain/celebrity/CelebrityTweetKey.java
package com.example.demo.domain.celebrity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@PrimaryKeyClass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CelebrityTweetKey implements Serializable {

    /**
     * 인플루언서/셀럽 ID (파티션 키)
     * - 특정 인플루언서의 모든 트윗이 한 파티션에 저장됨
     * - 팔로워 수가 많아 Fan-out on Write가 비효율적인 사용자들
     */
    @PrimaryKeyColumn(name = "author_id", type = PrimaryKeyType.PARTITIONED)
    private UUID authorId;

    /**
     * 트윗 생성 시간 (첫 번째 클러스터링 키, 내림차순)
     * - 최신 트윗부터 조회되도록 DESC 정렬
     * - 타임라인 병합 시 시간 기준으로 정렬에 활용
     */
    @PrimaryKeyColumn(name = "created_at", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private LocalDateTime createdAt;

    /**
     * 트윗 고유 ID (두 번째 클러스터링 키)
     * - 동일한 created_at을 가진 트윗들의 고유성 보장
     * - tweets 테이블의 원본 트윗과 연결되는 참조 키
     */
    @PrimaryKeyColumn(name = "tweet_id", type = PrimaryKeyType.CLUSTERED)
    private UUID tweetId;
}