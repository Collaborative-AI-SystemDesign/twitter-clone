package com.example.demo.domain.tweet.repository;

import com.example.demo.domain.tweet.Tweet;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Tweet 엔티티를 위한 Cassandra Repository
 * 트윗 CRUD 기본 기능 제공
 */
@Repository
public interface TweetRepository extends CassandraRepository<Tweet, UUID> {

    /**
     * 트윗 작성자 검증 (권한 확인용)
     * @param tweetId 트윗 ID
     * @param userId 사용자 ID
     * @return 존재 여부
     */
    boolean existsByTweetIdAndUserId(UUID tweetId, UUID userId);
} 