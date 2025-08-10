package com.example.demo.domain.follow;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

@PrimaryKeyClass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FollowingsByUserKey implements Serializable {

  @PrimaryKeyColumn(name = "follower_id", type = PrimaryKeyType.PARTITIONED)
  private UUID followerId;

  @PrimaryKeyColumn(name = "followed_user_id", type = PrimaryKeyType.CLUSTERED)
  private UUID followedUserId;
}