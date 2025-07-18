package com.example.demo.domain.follow;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * FollowersByUser 엔티티를 위한 Cassandra Repository
 * 
 * API 지원 기능:
 * - POST /follow/{userId}: 팔로우 생성
 * - DELETE /follow/{userId}: 팔로우 삭제
 * - Fan-out on Write 지원
 */
@Repository
public interface FollowRepository extends CassandraRepository<FollowersByUser, FollowersByUserKey> {

    /**
     * 특정 사용자의 모든 팔로워 ID 조회
     * Fan-out on Write에서 핵심적으로 사용
     * @param followedUserId 팔로우 당하는 사용자 ID
     * @return 팔로워 ID 목록
     */
    @Query("SELECT follower_id FROM followers_by_user WHERE followed_user_id = ?0")
    List<UUID> findFollowerIdsByFollowedUserId(UUID followedUserId);

    /**
     * 팔로우 관계 존재 여부 확인
     * 중복 팔로우 방지용
     * @param followedUserId 팔로우 당하는 사용자 ID
     * @param followerId 팔로우 하는 사용자 ID
     * @return 팔로우 관계 존재 여부
     */
    @Query("SELECT COUNT(*) FROM followers_by_user WHERE followed_user_id = ?0 AND follower_id = ?1")
    boolean existsByKeyFollowedUserIdAndKeyFollowerId(UUID followedUserId, UUID followerId);

    /**
     * 팔로워 수 조회
     * 인플루언서 판단 기준
     * @param followedUserId 팔로우 당하는 사용자 ID
     * @return 팔로워 수
     */
    @Query("SELECT COUNT(*) FROM followers_by_user WHERE followed_user_id = ?0")
    long countByKeyFollowedUserId(UUID followedUserId);

    /**
     * 팔로우 관계 삭제 (언팔로우)
     * @param followedUserId 팔로우 당하는 사용자 ID
     * @param followerId 팔로우 하는 사용자 ID
     */
    @Query("DELETE FROM followers_by_user WHERE followed_user_id = ?0 AND follower_id = ?1")
    void deleteByKeyFollowedUserIdAndKeyFollowerId(UUID followedUserId, UUID followerId);
} 