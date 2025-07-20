package com.example.demo.domain2.tweet_r.repository;

import com.example.demo.domain2.tweet_r.entity.TweetByUser;
import com.example.demo.domain2.tweet_r.entity.TweetByUserKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * RDB 버전 사용자별 트윗 리포지토리
 * 커서 기반 페이지네이션 지원
 */
@Repository
public interface TweetByUserRepository extends JpaRepository<TweetByUser, TweetByUserKey> {

    /**
     * 사용자의 최신 트윗 목록 조회 (첫 페이지)
     */
    @Query("SELECT t FROM TweetByUser t WHERE t.key.userId = :userId ORDER BY t.key.createdAt DESC")
    List<TweetByUser> findLatestTweets(@Param("userId") UUID userId);

    /**
     * 커서 기반 트윗 목록 조회 (다음 페이지)
     */
    @Query("SELECT t FROM TweetByUser t WHERE t.key.userId = :userId AND t.key.createdAt < :cursor ORDER BY t.key.createdAt DESC")
    List<TweetByUser> findTweetsWithCursor(@Param("userId") UUID userId, @Param("cursor") LocalDateTime cursor);
} 