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
 * 주요 기능:
 * - 트윗 원본 저장/조회/삭제
 * - UUID 기반 직접 접근 (O(1) 성능)
 * - 트윗 수정 및 메타데이터 업데이트
 */
@Repository
public interface TweetRepository extends CassandraRepository<Tweet, UUID> {

    /**
     * 트윗 ID로 트윗 조회
     * @param tweetId 트윗 고유 ID
     * @return 트윗 정보 (Optional)
     */
    Optional<Tweet> findByTweetId(UUID tweetId);

    /**
     * 특정 사용자가 작성한 트윗 존재 여부 확인
     * (권한 검증 시 사용)
     * @param tweetId 트윗 ID
     * @param userId 사용자 ID
     * @return 존재 여부
     */
    @Query("SELECT COUNT(*) FROM tweets WHERE tweet_id = ?0 AND user_id = ?1")
    boolean existsByTweetIdAndUserId(UUID tweetId, UUID userId);

    /**
     * 여러 트윗을 배치로 조회
     * (타임라인 상세 정보 조회 시 사용)
     * @param tweetIds 트윗 ID 목록
     * @return 트윗 목록
     */
    @Query("SELECT * FROM tweets WHERE tweet_id IN ?0")
    List<Tweet> findByTweetIdIn(List<UUID> tweetIds);

    /**
     * 트윗 텍스트 업데이트
     * @param tweetId 트윗 ID
     * @param tweetText 새로운 트윗 내용
     * @param modifiedAt 수정 시간
     */
    @Query("UPDATE tweets SET tweet_text = ?1, modified_at = ?2 WHERE tweet_id = ?0")
    void updateTweetText(UUID tweetId, String tweetText, LocalDateTime modifiedAt);

    /**
     * 트윗 삭제
     * @param tweetId 삭제할 트윗 ID
     */
    void deleteByTweetId(UUID tweetId);
} 