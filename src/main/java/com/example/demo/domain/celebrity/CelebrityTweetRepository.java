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
 * 주요 기능:
 * - Fan-out on Read 전략용 인플루언서 트윗 저장/조회
 * - 인플루언서별 시간순 트윗 조회
 * - 커서 기반 페이지네이션 지원
 * - 타임라인 병합 시 사용
 */
@Repository
public interface CelebrityTweetRepository extends CassandraRepository<CelebrityTweet, CelebrityTweetKey> {

    /**
     * 특정 인플루언서의 최신 트윗 조회
     * 시간순 내림차순으로 정렬되어 있음 (Cassandra 클러스터링 키 활용)
     * @param authorId 인플루언서 ID
     * @param limit 조회할 트윗 수
     * @return 인플루언서 트윗 목록
     */
    @Query("SELECT * FROM celebrity_tweets WHERE author_id = ?0 ORDER BY created_at DESC LIMIT ?1")
    List<CelebrityTweet> findByKeyAuthorIdOrderByKeyCreatedAtDesc(UUID authorId, int limit);

    /**
     * 커서 기반 페이지네이션 - 특정 시간 이전 트윗 조회
     * @param authorId 인플루언서 ID
     * @param cursor 기준 시간 (이 시간 이전 트윗들 조회)
     * @param limit 조회할 트윗 수
     * @return 인플루언서 트윗 목록
     */
    @Query("SELECT * FROM celebrity_tweets WHERE author_id = ?0 AND created_at < ?1 ORDER BY created_at DESC LIMIT ?2")
    List<CelebrityTweet> findByKeyAuthorIdAndKeyCreatedAtBefore(UUID authorId, LocalDateTime cursor, int limit);

    /**
     * 특정 시간 이후의 인플루언서 트윗 조회
     * (실시간 업데이트 시 사용)
     * @param authorId 인플루언서 ID
     * @param cursor 기준 시간 (이 시간 이후 트윗들 조회)
     * @param limit 조회할 트윗 수
     * @return 인플루언서 트윗 목록
     */
    @Query("SELECT * FROM celebrity_tweets WHERE author_id = ?0 AND created_at > ?1 ORDER BY created_at DESC LIMIT ?2")
    List<CelebrityTweet> findByKeyAuthorIdAndKeyCreatedAtAfter(UUID authorId, LocalDateTime cursor, int limit);

    /**
     * 여러 인플루언서의 최신 트윗 조회 (배치)
     * 타임라인 병합 시 사용
     * @param authorIds 인플루언서 ID 목록
     * @param limit 각 인플루언서당 조회할 트윗 수
     * @return 모든 인플루언서의 트윗 목록
     */
    @Query("SELECT * FROM celebrity_tweets WHERE author_id IN ?0 ORDER BY author_id, created_at DESC LIMIT ?1")
    List<CelebrityTweet> findByKeyAuthorIdInOrderByKeyCreatedAtDesc(List<UUID> authorIds, int limit);

    /**
     * 특정 트윗 ID로 인플루언서 트윗 조회
     * @param authorId 인플루언서 ID
     * @param tweetId 트윗 ID
     * @return 인플루언서 트윗 (선택적)
     */
    @Query("SELECT * FROM celebrity_tweets WHERE author_id = ?0 AND tweet_id = ?1 ALLOW FILTERING")
    List<CelebrityTweet> findByKeyAuthorIdAndKeyTweetId(UUID authorId, UUID tweetId);

    /**
     * 특정 인플루언서 트윗 삭제
     * @param authorId 인플루언서 ID
     * @param createdAt 생성 시간
     * @param tweetId 트윗 ID
     */
    @Query("DELETE FROM celebrity_tweets WHERE author_id = ?0 AND created_at = ?1 AND tweet_id = ?2")
    void deleteByKeyAuthorIdAndKeyCreatedAtAndKeyTweetId(UUID authorId, LocalDateTime createdAt, UUID tweetId);

    /**
     * 특정 인플루언서의 모든 트윗 삭제
     * (계정 삭제 또는 인플루언서 해제 시 사용)
     * @param authorId 인플루언서 ID
     */
    @Query("DELETE FROM celebrity_tweets WHERE author_id = ?0")
    void deleteByKeyAuthorId(UUID authorId);

    /**
     * 특정 인플루언서의 트윗 수 조회
     * @param authorId 인플루언서 ID
     * @return 트윗 수
     */
    @Query("SELECT COUNT(*) FROM celebrity_tweets WHERE author_id = ?0")
    long countByKeyAuthorId(UUID authorId);

    /**
     * 특정 기간 동안의 인플루언서 트윗 조회
     * @param authorId 인플루언서 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 해당 기간의 트윗 목록
     */
    @Query("SELECT * FROM celebrity_tweets WHERE author_id = ?0 AND created_at >= ?1 AND created_at <= ?2 ORDER BY created_at DESC")
    List<CelebrityTweet> findByKeyAuthorIdAndKeyCreatedAtBetween(UUID authorId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 모든 인플루언서의 최신 트윗 조회 (관리자용)
     * @param limit 조회할 트윗 수
     * @return 모든 인플루언서의 최신 트윗
     */
    @Query("SELECT * FROM celebrity_tweets ORDER BY author_id, created_at DESC LIMIT ?0")
    List<CelebrityTweet> findAllOrderByKeyCreatedAtDesc(int limit);

    /**
     * 인플루언서별 트윗 통계 조회
     * (인기도 분석용)
     * @return 인플루언서 ID와 트윗 수 매핑
     */
    @Query("SELECT author_id, COUNT(*) as tweet_count FROM celebrity_tweets GROUP BY author_id")
    List<Object[]> getCelebrityTweetStats();

    /**
     * 특정 시간 이후 모든 인플루언서 트윗 조회
     * (시스템 복구 시 사용)
     * @param timestamp 기준 시간
     * @return 해당 시간 이후의 모든 인플루언서 트윗
     */
    @Query("SELECT * FROM celebrity_tweets WHERE created_at > ?0 ALLOW FILTERING")
    List<CelebrityTweet> findAllByKeyCreatedAtAfter(LocalDateTime timestamp);
} 