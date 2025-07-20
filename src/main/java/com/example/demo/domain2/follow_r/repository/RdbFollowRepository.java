package com.example.demo.domain2.follow_r.repository;

import com.example.demo.domain2.follow_r.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RDB 버전 팔로우 리포지토리
 */
@Repository
public interface RdbFollowRepository extends JpaRepository<Follow, Long> {

    /**
     * 특정 사용자의 팔로워 ID 목록 조회
     * 
     * @param userId 사용자 ID
     * @return 팔로워 ID 목록
     */
    @Query("SELECT f.followerId FROM Follow f WHERE f.userId = :userId")
    List<UUID> findFollowerIds(@Param("userId") UUID userId);

    /**
     * 특정 사용자가 팔로우하는 사용자 ID 목록 조회
     * 
     * @param followerId 팔로워 ID
     * @return 팔로우하는 사용자 ID 목록
     */
    @Query("SELECT f.userId FROM Follow f WHERE f.followerId = :followerId")
    List<UUID> findFollowingIds(@Param("followerId") UUID followerId);

    /**
     * 팔로우 관계 존재 여부 확인
     * 
     * @param userId 사용자 ID
     * @param followerId 팔로워 ID
     * @return 팔로우 관계 존재 여부
     */
    boolean existsByUserIdAndFollowerId(UUID userId, UUID followerId);

    /**
     * 특정 사용자의 팔로워 수 조회
     * 
     * @param userId 사용자 ID
     * @return 팔로워 수
     */
    long countByUserId(UUID userId);

    /**
     * 특정 사용자가 팔로우하는 사용자 수 조회
     * 
     * @param followerId 팔로워 ID
     * @return 팔로잉 수
     */
    long countByFollowerId(UUID followerId);

    /**
     * 팔로우 관계 조회
     * 
     * @param userId 사용자 ID
     * @param followerId 팔로워 ID
     * @return 팔로우 엔티티
     */
    Optional<Follow> findByUserIdAndFollowerId(UUID userId, UUID followerId);
} 