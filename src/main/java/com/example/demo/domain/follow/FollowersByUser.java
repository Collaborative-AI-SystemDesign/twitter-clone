// src/main/java/com/example/demo/domain/follow/FollowersByUser.java
package com.example.demo.domain.follow;

import com.example.demo.domain.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.core.mapping.Column;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("followers_by_user")
@Getter
@Setter
@NoArgsConstructor
public class FollowersByUser extends BaseEntity {

    /**
     * 복합 Primary Key
     * - followed_user_id + follower_id 조합으로 고유성 보장
     */
    @PrimaryKey
    private FollowersByUserKey key;

    /**
     * 팔로우 시작 시간
     * - 언제부터 팔로우 관계가 시작되었는지 기록
     * - 팔로우 순서나 통계 분석에 활용 가능
     */
    @Column("followed_at")
    private LocalDateTime followedAt;

    /**
     * 팔로우 관계 생성 생성자
     * @param followedUserId 팔로우 당하는 사용자 ID  
     * @param followerId 팔로우 하는 사용자 ID
     */
    public FollowersByUser(UUID followedUserId, UUID followerId) {
        this.key = new FollowersByUserKey(followedUserId, followerId);
        this.followedAt = LocalDateTime.now();
        // createdAt, modifiedAt은 BaseEntity에서 자동 관리
    }
}