package com.example.demo.domain.follow;

import com.example.demo.domain.CassandraBaseEntity;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("followings_by_user")
@Getter
@Setter
@NoArgsConstructor
public class FollowingsByUser extends CassandraBaseEntity {

  @PrimaryKey
  private FollowingsByUserKey key;

  public FollowingsByUser(UUID followerId, UUID followedUserId, LocalDateTime createdAt) {
    this.key = new FollowingsByUserKey(followerId, followedUserId);
    this.setCreatedAt(createdAt);
  }

  public LocalDateTime getFollowingAt() {
    return this.getCreatedAt();
  }
}