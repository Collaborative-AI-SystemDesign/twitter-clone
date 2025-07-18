package com.example.demo.domain.celebrity;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * CelebrityTweet 엔티티를 위한 Cassandra Repository
 * Celebrity Problem 해결을 위한 Fan-out on Read 전략
 */
@Repository
public interface CelebrityTweetRepository extends CassandraRepository<CelebrityTweet, CelebrityTweetKey> {

    /**
     * 특정 인플루언서의 최신 트윗 조회
     * @param authorId 인플루언서 ID
     * @return 최신순 트윗 목록 (최대 20개)
     */
    @Query("SELECT * FROM celebrity_tweets WHERE author_id = ?0 LIMIT 20")
    List<CelebrityTweet> findLatestTweets(UUID authorId);

    /**
     * 커서 기반 페이지네이션
     * @param authorId 인플루언서 ID
     * @param cursor 기준 시간
     * @return 트윗 목록
     */
    @Query("SELECT * FROM celebrity_tweets WHERE author_id = ?0 AND created_at < ?1 LIMIT 20")
    List<CelebrityTweet> findTweetsWithCursor(UUID authorId, LocalDateTime cursor);
} 