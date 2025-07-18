package com.example.demo.domain.timeline;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * UserTimeline 엔티티를 위한 Cassandra Repository
 * 
 * 주요 기능:
 * - Fan-out on Write 결과물 저장/조회
 * - 시간순 타임라인 조회 (최신순)
 * - 커서 기반 페이지네이션 지원
 * - 특정 트윗 타임라인에서 제거
 */
@Repository
public interface UserTimelineRepository extends CassandraRepository<UserTimeline, UserTimelineKey> {

    /**
     * 특정 사용자의 최신 타임라인 조회
     * 시간순 내림차순으로 정렬되어 있음 (Cassandra 클러스터링 키 활용)
     * @param followerId 타임라인 소유자 ID
     * @param limit 조회할 트윗 수
     * @return 타임라인 목록
     */
    @Query("SELECT * FROM user_timeline WHERE follower_id = ?0 ORDER BY created_at DESC LIMIT ?1")
    List<UserTimeline> findByKeyFollowerIdOrderByKeyCreatedAtDesc(UUID followerId, int limit);

    /**
     * 커서 기반 페이지네이션 - 특정 시간 이전 트윗 조회
     * @param followerId 타임라인 소유자 ID
     * @param cursor 기준 시간 (이 시간 이전 트윗들 조회)
     * @param limit 조회할 트윗 수
     * @return 타임라인 목록
     */
    @Query("SELECT * FROM user_timeline WHERE follower_id = ?0 AND created_at < ?1 ORDER BY created_at DESC LIMIT ?2")
    List<UserTimeline> findByKeyFollowerIdAndKeyCreatedAtBefore(UUID followerId, LocalDateTime cursor, int limit);

    /**
     * 특정 시간 이후의 타임라인 조회
     * (실시간 업데이트 시 사용)
     * @param followerId 타임라인 소유자 ID
     * @param cursor 기준 시간 (이 시간 이후 트윗들 조회)
     * @param limit 조회할 트윗 수
     * @return 타임라인 목록
     */
    @Query("SELECT * FROM user_timeline WHERE follower_id = ?0 AND created_at > ?1 ORDER BY created_at DESC LIMIT ?2")
    List<UserTimeline> findByKeyFollowerIdAndKeyCreatedAtAfter(UUID followerId, LocalDateTime cursor, int limit);

    /**
     * 특정 작성자의 트윗들을 특정 사용자 타임라인에서 조회
     * (언팔로우 시 제거할 트윗들 찾기)
     * @param followerId 타임라인 소유자 ID
     * @param authorId 트윗 작성자 ID
     * @return 해당 작성자의 타임라인 목록
     */
    @Query("SELECT * FROM user_timeline WHERE follower_id = ?0 AND author_id = ?1 ALLOW FILTERING")
    List<UserTimeline> findByKeyFollowerIdAndAuthorId(UUID followerId, UUID authorId);

    /**
     * 특정 트윗을 타임라인에서 제거
     * (트윗 삭제 시 모든 타임라인에서 제거)
     * @param followerId 타임라인 소유자 ID
     * @param createdAt 생성 시간
     * @param tweetId 트윗 ID
     */
    @Query("DELETE FROM user_timeline WHERE follower_id = ?0 AND created_at = ?1 AND tweet_id = ?2")
    void deleteByKeyFollowerIdAndKeyCreatedAtAndKeyTweetId(UUID followerId, LocalDateTime createdAt, UUID tweetId);

    /**
     * 특정 사용자의 전체 타임라인 삭제
     * (계정 삭제 시 사용)
     * @param followerId 타임라인 소유자 ID
     */
    @Query("DELETE FROM user_timeline WHERE follower_id = ?0")
    void deleteByKeyFollowerId(UUID followerId);

    /**
     * 특정 작성자의 모든 트윗을 특정 사용자 타임라인에서 제거
     * (언팔로우 시 사용)
     * @param followerId 타임라인 소유자 ID
     * @param authorId 제거할 트윗 작성자 ID
     */
    @Query("DELETE FROM user_timeline WHERE follower_id = ?0 AND author_id = ?1")
    void deleteByKeyFollowerIdAndAuthorId(UUID followerId, UUID authorId);

    /**
     * 특정 사용자 타임라인의 트윗 수 조회
     * @param followerId 타임라인 소유자 ID
     * @return 타임라인의 트윗 수
     */
    @Query("SELECT COUNT(*) FROM user_timeline WHERE follower_id = ?0")
    long countByKeyFollowerId(UUID followerId);

    /**
     * 배치 삽입을 위한 여러 타임라인 엔트리 저장
     * (Fan-out 시 성능 최적화)
     * @param timelines 저장할 타임라인 목록
     */
    default void saveAllTimelines(List<UserTimeline> timelines) {
        saveAll(timelines);
    }

    /**
     * 특정 기간 동안의 타임라인 조회
     * @param followerId 타임라인 소유자 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 해당 기간의 타임라인 목록
     */
    @Query("SELECT * FROM user_timeline WHERE follower_id = ?0 AND created_at >= ?1 AND created_at <= ?2 ORDER BY created_at DESC")
    List<UserTimeline> findByKeyFollowerIdAndKeyCreatedAtBetween(UUID followerId, LocalDateTime startTime, LocalDateTime endTime);
} 