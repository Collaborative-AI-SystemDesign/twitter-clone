package com.example.demo.domain2.timeline_r.repository;

import com.example.demo.domain2.timeline_r.entity.UserTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * RDB 버전 사용자 타임라인 리포지토리
 * 커서 기반 페이지네이션 지원
 */
@Repository
public interface RdbUserTimelineRepository extends JpaRepository<UserTimeline, Long> {

    /**
     * 팔로워의 최신 타임라인 조회 (첫 페이지)
     * 
     * @param followerId 팔로워 ID
     * @return 최신 타임라인 목록
     */
    @Query("SELECT t FROM UserTimeline t WHERE t.followerId = :followerId ORDER BY t.createdAt DESC")
    List<UserTimeline> findLatestTimeline(@Param("followerId") UUID followerId);

    /**
     * 커서 기반 타임라인 조회 (다음 페이지)
     * 
     * @param followerId 팔로워 ID
     * @param cursor 마지막 트윗 시간
     * @return 타임라인 목록
     */
    @Query("SELECT t FROM UserTimeline t WHERE t.followerId = :followerId AND t.createdAt < :cursor ORDER BY t.createdAt DESC")
    List<UserTimeline> findTimelineWithCursor(@Param("followerId") UUID followerId, @Param("cursor") LocalDateTime cursor);

    /**
     * 특정 트윗을 특정 팔로워의 타임라인에서 삭제
     * (트윗 삭제 시 사용)
     * 
     * @param tweetId 트윗 ID
     * @param followerId 팔로워 ID
     */
    void deleteByTweetIdAndFollowerId(UUID tweetId, UUID followerId);

    /**
     * 특정 트윗을 모든 팔로워의 타임라인에서 삭제
     * (트윗 삭제 시 사용)
     * 
     * @param tweetId 트윗 ID
     */
    void deleteByTweetId(UUID tweetId);

    /**
     * 특정 작성자의 모든 트윗을 특정 팔로워의 타임라인에서 삭제
     * (언팔로우 시 사용)
     * 
     * @param authorId 작성자 ID
     * @param followerId 팔로워 ID
     */
    void deleteByAuthorIdAndFollowerId(UUID authorId, UUID followerId);
} 