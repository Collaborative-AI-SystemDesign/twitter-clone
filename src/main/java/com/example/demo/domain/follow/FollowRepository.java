package com.example.demo.domain.follow;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * FollowersByUser 엔티티를 위한 Cassandra Repository
 * Fan-out on Write 지원을 위한 팔로워 조회
 */
@Repository
public interface FollowRepository extends CassandraRepository<FollowersByUser, FollowersByUserKey> {

    /**
     * 특정 사용자의 팔로워 목록 조회 (Fan-out on Write 핵심)
     * @param followedUserId 팔로우 당하는 사용자 ID
     * @return 팔로워 목록
     */
    List<FollowersByUser> findByKeyFollowedUserId(UUID followedUserId);

    /**
     * 팔로우 관계 존재 여부 확인
     * @param followedUserId 팔로우 당하는 사용자 ID
     * @param followerId 팔로우 하는 사용자 ID
     * @return 존재 여부
     */
    boolean existsByKeyFollowedUserIdAndKeyFollowerId(UUID followedUserId, UUID followerId);

    /**
     * 내가 팔로우하는 사람들 조회
     * @param followerId 팔로우 하는 사용자 ID (나)
     * @return 나의 팔로잉 목록
     */
    List<FollowersByUser> findByKeyFollowerId(UUID followerId);
} 