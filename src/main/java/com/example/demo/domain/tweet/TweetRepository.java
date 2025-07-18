package com.example.demo.domain.tweet;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Tweet 엔티티를 위한 Cassandra Repository
 * 
 * API 지원 기능:
 * - POST /tweets: 트윗 생성
 * - GET /tweets/{userId}: 사용자별 트윗 조회
 */
@Repository
public interface TweetRepository extends CassandraRepository<Tweet, UUID> {

    /**
     * 트윗 ID로 개별 트윗 조회
     * @param tweetId 트윗 고유 ID
     * @return 트윗 정보
     */
    Optional<Tweet> findByTweetId(UUID tweetId);

    /**
     * 특정 사용자의 최신 트윗 조회
     * GET /tweets/{userId} API 지원
     * 참고: 성능 최적화를 위해 별도 테이블 설계 권장
     * @param userId 사용자 ID
     * @param limit 조회할 트윗 수
     * @return 사용자의 트윗 목록
     */
    @Query("SELECT * FROM tweets WHERE user_id = ?0 ORDER BY created_at DESC LIMIT ?1 ALLOW FILTERING")
    List<Tweet> findByUserIdOrderByCreatedAtDesc(UUID userId, int limit);

    /**
     * 커서 기반 사용자 트윗 조회
     * GET /tweets/{userId}?last={timestamp} API 지원
     * @param userId 사용자 ID
     * @param cursor 기준 시간
     * @param limit 조회할 트윗 수
     * @return 사용자의 트윗 목록
     */
    @Query("SELECT * FROM tweets WHERE user_id = ?0 AND created_at < ?1 ORDER BY created_at DESC LIMIT ?2 ALLOW FILTERING")
    List<Tweet> findByUserIdAndCreatedAtBefore(UUID userId, LocalDateTime cursor, int limit);

    /**
     * 트윗 삭제 (작성자 본인만 가능)
     * @param tweetId 삭제할 트윗 ID
     */
    void deleteByTweetId(UUID tweetId);

    /**
     * 권한 검증용: 특정 사용자가 작성한 트윗인지 확인
     * @param tweetId 트윗 ID
     * @param userId 사용자 ID
     * @return 존재 여부
     */
    @Query("SELECT COUNT(*) FROM tweets WHERE tweet_id = ?0 AND user_id = ?1")
    boolean existsByTweetIdAndUserId(UUID tweetId, UUID userId);
} 