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
 * API 지원 기능:
 * - GET /timeline: 타임라인 조회 (커서 기반 페이지네이션)
 * - Fan-out on Write 전략 지원
 */
@Repository
public interface UserTimelineRepository extends CassandraRepository<UserTimeline, UserTimelineKey> {

    /**
     * 특정 사용자의 최신 타임라인 조회
     * GET /timeline?last={timestamp} API 지원
     * @param followerId 타임라인 소유자 ID
     * @param limit 조회할 트윗 수 (기본 20개)
     * @return 시간순 정렬된 타임라인 목록
     */
    @Query("SELECT * FROM user_timeline WHERE follower_id = ?0 ORDER BY created_at DESC LIMIT ?1")
    List<UserTimeline> findByKeyFollowerIdOrderByKeyCreatedAtDesc(UUID followerId, int limit);

    /**
     * 커서 기반 페이지네이션 - 특정 시간 이전 트윗 조회
     * GET /timeline?last={timestamp} API의 다음 페이지 조회
     * @param followerId 타임라인 소유자 ID
     * @param cursor 기준 시간 (이 시간 이전 트윗들 조회)
     * @param limit 조회할 트윗 수
     * @return 타임라인 목록
     */
    @Query("SELECT * FROM user_timeline WHERE follower_id = ?0 AND created_at < ?1 ORDER BY created_at DESC LIMIT ?2")
    List<UserTimeline> findByKeyFollowerIdAndKeyCreatedAtBefore(UUID followerId, LocalDateTime cursor, int limit);

    /**
     * 특정 트윗을 타임라인에서 제거
     * 트윗 삭제 시 모든 타임라인에서 제거용
     * @param followerId 타임라인 소유자 ID
     * @param createdAt 생성 시간
     * @param tweetId 트윗 ID
     */
    @Query("DELETE FROM user_timeline WHERE follower_id = ?0 AND created_at = ?1 AND tweet_id = ?2")
    void deleteByKeyFollowerIdAndKeyCreatedAtAndKeyTweetId(UUID followerId, LocalDateTime createdAt, UUID tweetId);

    /**
     * 배치 삽입을 위한 여러 타임라인 엔트리 저장
     * Fan-out on Write 시 성능 최적화
     * @param timelines 저장할 타임라인 목록
     */
    default void saveAllTimelines(List<UserTimeline> timelines) {
        saveAll(timelines);
    }
} 