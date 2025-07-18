
package com.example.demo.domain.follow;

import com.example.demo.domain.CassandraBaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("followers_by_user")
@Getter
@Setter
@NoArgsConstructor
public class FollowersByUser extends CassandraBaseEntity {

    /**
     * 복합 Primary Key
     * - followed_user_id + follower_id 조합으로 고유성 보장
     */
    @PrimaryKey
    private FollowersByUserKey key;

    /**
     * 팔로우 관계 생성 생성자
     * @param followedUserId 팔로우 당하는 사용자 ID  
     * @param followerId 팔로우 하는 사용자 ID
     * @param createdAt 팔로우 시작 시간
     */
    public FollowersByUser(UUID followedUserId, UUID followerId, LocalDateTime createdAt) {
        this.key = new FollowersByUserKey(followedUserId, followerId);
        this.setCreatedAt(createdAt);
    }

    /**
     * 팔로우 시작 시간 조회 (createdAt 활용)
     */
    public LocalDateTime getFollowedAt() {
        return this.getCreatedAt();
    }
}