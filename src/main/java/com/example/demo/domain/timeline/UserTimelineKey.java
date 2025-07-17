// src/main/java/com/example/demo/domain/timeline/UserTimelineKey.java
package com.example.demo.domain.timeline;

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
public class UserTimelineKey implements Serializable {

    /**
     * 타임라인 소유자 ID (파티션 키)
     * - 이 사용자가 보게 될 타임라인을 의미
     * - 각 사용자의 타임라인이 별도 파티션에 저장됨
     */
    @PrimaryKeyColumn(name = "follower_id", type = PrimaryKeyType.PARTITIONED)
    private UUID followerId;

    /**
     * 트윗 생성 시간 (첫 번째 클러스터링 키, 내림차순)
     * - 타임라인에서 최신 트윗부터 보이도록 DESC 정렬
     * - 페이지네이션의 커서 역할
     */
    @PrimaryKeyColumn(name = "created_at", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private LocalDateTime createdAt;

    /**
     * 트윗 고유 ID (두 번째 클러스터링 키)
     * - 동일한 created_at을 가진 트윗들의 고유성 보장
     * - 정확한 커서 기반 페이지네이션 지원
     */
    @PrimaryKeyColumn(name = "tweet_id", type = PrimaryKeyType.CLUSTERED)
    private UUID tweetId;
}