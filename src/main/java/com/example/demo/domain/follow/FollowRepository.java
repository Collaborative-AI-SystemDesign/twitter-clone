package com.example.demo.domain.follow;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * FollowersByUser 엔티티를 위한 Cassandra Repository
 * 
 * 주요 기능:
 * - 팔로우 관계 생성/삭제
 * - 특정 사용자의 모든 팔로워 조회 (Fan-out on Write에서 핵심)
 * - 팔로우 관계 존재 여부 확인
 * - 팔로워 수 카운팅
 */
@Repository
public interface FollowRepository extends CassandraRepository<FollowersByUser, FollowersByUserKey> {

    /**
     * 특정 사용자의 모든 팔로워 조회
     * Fan-out on Write에서 핵심적으로 사용되는 쿼리
     * @param followedUserId 팔로우 당하는 사용자 ID
     * @return 팔로워 목록
     */
    @Query("SELECT * FROM followers_by_user WHERE followed_user_id = ?0")
    List<FollowersByUser> findByKeyFollowedUserId(UUID followedUserId);

    /**
     * 특정 사용자의 팔로워 ID 목록만 조회 (성능 최적화)
     * @param followedUserId 팔로우 당하는 사용자 ID
     * @return 팔로워 ID 목록
     */
    @Query("SELECT follower_id FROM followers_by_user WHERE followed_user_id = ?0")
    List<UUID> findFollowerIdsByFollowedUserId(UUID followedUserId);

    /**
     * 팔로우 관계 존재 여부 확인
     * @param followedUserId 팔로우 당하는 사용자 ID
     * @param followerId 팔로우 하는 사용자 ID
     * @return 팔로우 관계 존재 여부
     */
    @Query("SELECT COUNT(*) FROM followers_by_user WHERE followed_user_id = ?0 AND follower_id = ?1")
    boolean existsByKeyFollowedUserIdAndKeyFollowerId(UUID followedUserId, UUID followerId);

    /**
     * 팔로워 수 카운팅
     * 인플루언서 판단 기준으로 사용
     * @param followedUserId 팔로우 당하는 사용자 ID
     * @return 팔로워 수
     */
    @Query("SELECT COUNT(*) FROM followers_by_user WHERE followed_user_id = ?0")
    long countByKeyFollowedUserId(UUID followedUserId);

    /**
     * 특정 기간 이후의 팔로워 조회
     * @param followedUserId 팔로우 당하는 사용자 ID
     * @param followedAt 기준 시간
     * @return 해당 기간 이후 팔로워 목록
     */
    @Query("SELECT * FROM followers_by_user WHERE followed_user_id = ?0 AND followed_at >= ?1 ALLOW FILTERING")
    List<FollowersByUser> findByKeyFollowedUserIdAndFollowedAtAfter(UUID followedUserId, LocalDateTime followedAt);

    /**
     * 팔로우 관계 삭제 (언팔로우)
     * @param followedUserId 팔로우 당하는 사용자 ID
     * @param followerId 팔로우 하는 사용자 ID
     */
    @Query("DELETE FROM followers_by_user WHERE followed_user_id = ?0 AND follower_id = ?1")
    void deleteByKeyFollowedUserIdAndKeyFollowerId(UUID followedUserId, UUID followerId);

    /**
     * 특정 사용자의 모든 팔로워 관계 삭제
     * (계정 삭제 시 사용)
     * @param followedUserId 팔로우 당하는 사용자 ID
     */
    @Query("DELETE FROM followers_by_user WHERE followed_user_id = ?0")
    void deleteByKeyFollowedUserId(UUID followedUserId);

    /**
     * 팔로워 수 기준으로 인플루언서 후보 조회
     * @param minFollowerCount 최소 팔로워 수
     * @return 인플루언서 후보 사용자 ID 목록
     */
    @Query("SELECT followed_user_id FROM followers_by_user GROUP BY followed_user_id HAVING COUNT(*) >= ?0 ALLOW FILTERING")
    List<UUID> findUsersWithMinFollowers(long minFollowerCount);
} 