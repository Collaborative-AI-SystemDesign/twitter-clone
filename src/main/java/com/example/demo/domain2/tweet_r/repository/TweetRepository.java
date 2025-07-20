package com.example.demo.domain2.tweet_r.repository;

import com.example.demo.domain2.tweet_r.entity.Tweet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * RDB 버전 트윗 리포지토리
 */
@Repository
public interface TweetRepository extends JpaRepository<Tweet, UUID> {
} 