package com.example.demo.domain.tweet;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 사용자별 트윗 조회를 위한 Cassandra Repository
 */
@Repository
public interface TweetByUserRepository extends CassandraRepository<TweetByUser, TweetByUserKey> {

    /**
     * 특정 사용자의 최신 트윗 조회
     * @param userId 사용자 ID
     * @return 최신순 트윗 목록 (최대 20개)
     */
    @Query("SELECT * FROM tweets_by_user WHERE user_id = ?0 LIMIT 20")
    List<TweetByUser> findLatestTweets(UUID userId);

    /**
     * 커서 기반 페이지네이션
     * @param userId 사용자 ID
     * @param cursor 기준 시간
     * @return 트윗 목록
     */
    @Query("SELECT * FROM tweets_by_user WHERE user_id = ?0 AND created_at < ?1 LIMIT 20")
    List<TweetByUser> findTweetsWithCursor(UUID userId, LocalDateTime cursor);
}
