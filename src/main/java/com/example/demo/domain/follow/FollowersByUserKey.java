// src/main/java/com/example/demo/domain/follow/FollowersByUserKey.java
package com.example.demo.domain.follow;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.util.UUID;

@PrimaryKeyClass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FollowersByUserKey implements Serializable {

    /**
     * 팔로우 당하는 사용자 ID (파티션 키)
     * - 이 값이 같은 모든 레코드는 동일한 Cassandra 노드에 저장됨
     * - "누구의 팔로워 목록인가?"를 나타내는 기준
     */
    @PrimaryKeyColumn(name = "followed_user_id", type = PrimaryKeyType.PARTITIONED)
    private UUID followedUserId;

    /**
     * 팔로우 하는 사용자 ID (클러스터링 키)
     * - 같은 파티션 내에서 이 값으로 정렬됨
     * - 중복 방지: 같은 사람이 같은 사람을 두 번 팔로우할 수 없음
     */
    @PrimaryKeyColumn(name = "follower_id", type = PrimaryKeyType.CLUSTERED)
    private UUID followerId;
}