package com.example.demo.domain.follow;

import java.util.List;
import java.util.UUID;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowingsByUserRepository extends CassandraRepository<FollowingsByUser, FollowingsByUserKey> {

  /**
   * 특정 사용자가 팔로우 하는 사용자 리스트 조회
   * @param followerId 팔로우 하는 사용자 ID (파티션 키)
   * @return 팔로잉 목록
   */
  List<FollowingsByUser> findByKeyFollowerId(UUID followerId);

  /**
   * 팔로잉 관계 존재 여부 조회
   * @param followerId 팔로우 하는 사용자 ID
   * @param followedUserId 팔로우 당하는 사용자 ID
   * @return 존재 여부
   */
  boolean existsByKeyFollowerIdAndKeyFollowedUserId(UUID followerId, UUID followedUserId);
}