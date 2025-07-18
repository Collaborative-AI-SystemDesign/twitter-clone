package com.example.demo.domain.tweet;

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

/**
 * 사용자별 트윗 조회를 위한 복합 키
 * - user_id를 파티션 키로 설정하여 한 유저의 트윗을 동일 노드에 저장
 * - created_at DESC 정렬로 최신 트윗 우선 조회 가능
 * - tweet_id는 동일 시간 충돌 방지 및 커서 페이징 용도
 */
@PrimaryKeyClass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TweetByUserKey implements Serializable {

    /**
     * 트윗 작성자 ID (파티션 키)
     */
    @PrimaryKeyColumn(name = "user_id", type = PrimaryKeyType.PARTITIONED)
    private UUID userId;

    /**
     * 트윗 작성 시각 (클러스터링 키, 내림차순 정렬)
     */
    @PrimaryKeyColumn(name = "created_at", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private LocalDateTime createdAt;

    /**
     * 트윗 고유 ID (클러스터링 키)
     * - 같은 시각에 여러 트윗이 있을 경우 고유성 보장
     */
    @PrimaryKeyColumn(name = "tweet_id", type = PrimaryKeyType.CLUSTERED)
    private UUID tweetId;
}
