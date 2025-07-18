package com.example.demo.domain.celebrity;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * CelebrityTweet 엔티티를 위한 Cassandra Repository
 * 
 * API 지원 기능:
 * - GET /timeline: 타임라인 조회 시 인플루언서 트윗 병합
 * - Fan-out on Read 전략 (Celebrity Problem 해결)
 */
@Repository
public interface CelebrityTweetRepository extends CassandraRepository<CelebrityTweet, CelebrityTweetKey> {

    /**
     * 특정 인플루언서의 최신 트윗 조회
     * Fan-out on Read 시 사용
     * @param authorId 인플루언서 ID
     * @param limit 조회할 트윗 수
     * @return 시간순 정렬된 인플루언서 트윗 목록
     */
    @Query("SELECT * FROM celebrity_tweets WHERE author_id = ?0 ORDER BY created_at DESC LIMIT ?1")
    List<CelebrityTweet> findByKeyAuthorIdOrderByKeyCreatedAtDesc(UUID authorId, int limit);

    /**
     * 커서 기반 페이지네이션 - 특정 시간 이전 트윗 조회
     * 타임라인 병합 시 사용
     * @param authorId 인플루언서 ID
     * @param cursor 기준 시간
     * @param limit 조회할 트윗 수
     * @return 인플루언서 트윗 목록
     */
    @Query("SELECT * FROM celebrity_tweets WHERE author_id = ?0 AND created_at < ?1 ORDER BY created_at DESC LIMIT ?2")
    List<CelebrityTweet> findByKeyAuthorIdAndKeyCreatedAtBefore(UUID authorId, LocalDateTime cursor, int limit);

    /**
     * 여러 인플루언서의 최신 트윗 배치 조회
     * 타임라인 병합 시 성능 최적화
     * @param authorIds 인플루언서 ID 목록
     * @param limit 각 인플루언서당 조회할 트윗 수
     * @return 모든 인플루언서의 트윗 목록
     */
    @Query("SELECT * FROM celebrity_tweets WHERE author_id IN ?0 ORDER BY author_id, created_at DESC LIMIT ?1")
    List<CelebrityTweet> findByKeyAuthorIdInOrderByKeyCreatedAtDesc(List<UUID> authorIds, int limit);

    /**
     * 특정 인플루언서 트윗 삭제
     * 트윗 삭제 시 사용
     * @param authorId 인플루언서 ID
     * @param createdAt 생성 시간
     * @param tweetId 트윗 ID
     */
    @Query("DELETE FROM celebrity_tweets WHERE author_id = ?0 AND created_at = ?1 AND tweet_id = ?2")
    void deleteByKeyAuthorIdAndKeyCreatedAtAndKeyTweetId(UUID authorId, LocalDateTime createdAt, UUID tweetId);
} 