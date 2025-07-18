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
     * @param followerId 타임라인 소유자 ID
     * @return 최신순 타임라인 목록 (최대 20개)
     */
    @Query("SELECT * FROM user_timeline WHERE follower_id = ?0 LIMIT 20")
    List<UserTimeline> findLatestTimeline(UUID followerId);

    /**
     * 커서 기반 페이지네이션
     * @param followerId 타임라인 소유자 ID
     * @param cursor 기준 시간
     * @return 타임라인 목록
     */
    @Query("SELECT * FROM user_timeline WHERE follower_id = ?0 AND created_at < ?1 LIMIT 20")
    List<UserTimeline> findTimelineWithCursor(UUID followerId, LocalDateTime cursor);
} 