package com.example.demo.domain2.tweet_r.service;

import com.example.demo.config.DataSourceConfig;
import com.example.demo.domain2.follow_r.repository.RdbFollowRepository;
import com.example.demo.domain2.timeline_r.entity.UserTimeline;
import com.example.demo.domain2.timeline_r.repository.RdbUserTimelineRepository;
import com.example.demo.domain2.tweet_r.entity.Tweet;
import com.example.demo.domain2.tweet_r.entity.TweetByUser;
import com.example.demo.domain2.tweet_r.entity.TweetByUserKey;
import com.example.demo.domain2.tweet_r.repository.RdbTweetByUserRepository;
import com.example.demo.domain2.tweet_r.repository.RdbTweetRepository;
import com.example.demo.domain2.tweet_r.request.CreateTweetRequest;
import com.example.demo.domain2.tweet_r.response.TweetResponse;
import com.example.demo.domain2.tweet_r.response.TweetListResponse;
import com.example.demo.util.ShardUtil;
import com.example.demo.util.UUID.UUIDUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RDB 전용 트윗 비즈니스 로직 서비스 (샤딩 + Fan-out-on-write 적용)
 * 
 * 핵심 기능:
 * - 트윗 생성 + Fan-out-on-write 전략
 * - 사용자별 트윗 조회 (커서 기반 페이지네이션)
 * - 샤딩 적용 (원본 트윗: authorID 기준, 타임라인: followerID 기준)
 * - 순수 RDB 환경에서 동작 (Cassandra 의존성 없음)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RdbTweetService {

    private final RdbTweetRepository tweetRepository;
    private final RdbTweetByUserRepository tweetByUserRepository;
    private final RdbFollowRepository followRepository;
    private final RdbUserTimelineRepository userTimelineRepository;

    /**
     * 새 트윗 생성 + Fan-out-on-write
     * 
     * 전략: 
     * 1. 원본 트윗은 authorID 기준 샤딩
     * 2. 팔로워들의 타임라인은 followerID 기준 샤딩
     */
    @Transactional
    public TweetResponse createTweet(UUID userId, CreateTweetRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }
        
        UUID tweetId = UUIDUtil.generate();
        LocalDateTime now = LocalDateTime.now();

        // 1. 원본 트윗 저장 (Controller에서 이미 샤드 설정됨)
        saveTweetWithoutSharding(userId, tweetId, request.getContent(), now);

        // 2. 사용자별 트윗 저장 (Controller에서 이미 샤드 설정됨)
        saveTweetByUserWithoutSharding(userId, tweetId, request.getContent(), now);

        // 3. Fan-out 시도 (팔로워 타임라인에 복사)
        try {
            fanOutToFollowers(userId, tweetId, request.getContent(), now);
        } catch (Exception e) {
            log.error("Fan-out 실패 - userId: {}, tweetId: {}, error: {}", 
                    userId, tweetId, e.getMessage(), e);
            // 원본 트윗은 저장되었으므로 실패해도 계속 진행
        }

        log.info("RDB 트윗 생성 완료 (샤딩 적용) - userId: {}, tweetId: {}", userId, tweetId);
        
        // 응답용 Tweet 객체 생성
        Tweet tweet = Tweet.builder()
                .tweetId(tweetId)
                .userId(userId)
                .tweetText(request.getContent())
                .createdAt(now)
                .build();
        
        return TweetResponse.of(tweet);
    }

    /**
     * 원본 트윗 저장 (Controller에서 이미 샤드 설정됨)
     */
    private void saveTweetWithoutSharding(UUID userId, UUID tweetId, String content, LocalDateTime createdAt) {
        Tweet tweet = Tweet.builder()
                .tweetId(tweetId)
                .userId(userId)
                .tweetText(content)
                .createdAt(createdAt)
                .build();
        
        tweetRepository.save(tweet);
        
        log.debug("원본 트윗 저장 완료 - userId: {}, tweetId: {}", userId, tweetId);
    }

    /**
     * 사용자별 트윗 저장 (Controller에서 이미 샤드 설정됨)
     */
    private void saveTweetByUserWithoutSharding(UUID userId, UUID tweetId, String content, LocalDateTime createdAt) {
        TweetByUserKey key = TweetByUserKey.builder()
                .userId(userId)
                .tweetId(tweetId)
                .createdAt(createdAt)
                .build();
        
        TweetByUser tweetByUser = TweetByUser.builder()
                .key(key)
                .tweetText(content)
                .build();
        
        tweetByUserRepository.save(tweetByUser);
        
        log.debug("사용자별 트윗 저장 완료 - userId: {}, tweetId: {}", userId, tweetId);
    }

    /**
     * 팔로워들의 타임라인에 새 트윗 복사 (Fan-out-on-write)
     */
    private void fanOutToFollowers(UUID authorId, UUID tweetId, String tweetText, LocalDateTime createdAt) {
        try {
            // 1. 팔로워 목록 조회 (authorID 기준 샤딩 - shard0)
            List<UUID> followerIds = getFollowersWithSharding(authorId);

            if (followerIds.isEmpty()) {
                log.debug("팔로워 없음 - authorId: {}", authorId);
                return;
            }

            log.info("Fan-out 시작 - authorId: {}, 팔로워 수: {}", authorId, followerIds.size());

            // 2. 각 팔로워의 타임라인에 트윗 추가 (followerID 기준 샤딩)
            int successCount = 0;
            for (UUID followerId : followerIds) {
                try {
                    saveTimelineWithSharding(followerId, tweetId, authorId, tweetText, createdAt);
                    successCount++;
                } catch (Exception e) {
                    log.error("개별 팔로워 Fan-out 실패 - followerId: {}, tweetId: {}, error: {}", 
                            followerId, tweetId, e.getMessage());
                    // 개별 실패해도 다른 팔로워들은 계속 처리
                }
            }
            
            log.info("Fan-out 완료 - authorId: {}, 전체: {}, 성공: {}, 실패: {}", 
                    authorId, followerIds.size(), successCount, followerIds.size() - successCount);
                    
        } catch (Exception e) {
            log.error("Fan-out 전체 실패 - authorId: {}, tweetId: {}, error: {}", 
                    authorId, tweetId, e.getMessage());
            // Fan-out 실패해도 원본 트윗은 유지
        }
    }

    /**
     * 팔로워 목록 조회 (사용자 데이터는 항상 shard0)
     */
    private List<UUID> getFollowersWithSharding(UUID authorId) {
        String shardKey = ShardUtil.selectUserDataShardKey(); // 항상 shard0
        String currentShard = DataSourceConfig.getShard();
        
        try {
            DataSourceConfig.setShard(shardKey);
            List<UUID> followers = followRepository.findFollowerIds(authorId);
            log.debug("팔로워 조회 완료 - authorId: {}, 팔로워 수: {}", authorId, followers.size());
            return followers;
        } catch (Exception e) {
            log.error("팔로워 조회 실패 - authorId: {}, error: {}", authorId, e.getMessage());
            return new ArrayList<>(); // 빈 리스트 반환으로 안전하게 처리
        } finally {
            // 이전 샤드로 복원
            if (currentShard != null) {
                DataSourceConfig.setShard(currentShard);
            } else {
                DataSourceConfig.clearShard();
            }
        }
    }

    /**
     * 타임라인에 트윗 저장 (followerID 기준 트윗 데이터용 샤딩 - shard1,2,3)
     */
    private void saveTimelineWithSharding(UUID followerId, UUID tweetId, UUID authorId, 
                                        String tweetText, LocalDateTime createdAt) {
        String shardKey = ShardUtil.selectTweetDataShardKeyByUserId(followerId);
        String currentShard = DataSourceConfig.getShard();
        
        try {
            DataSourceConfig.setShard(shardKey);
            
            UserTimeline timeline = UserTimeline.builder()
                    .followerId(followerId)
                    .tweetId(tweetId)
                    .authorId(authorId)
                    .tweetText(tweetText)
                    .createdAt(createdAt)
                    .build();
            
            userTimelineRepository.save(timeline);
            
            log.debug("타임라인 저장 완료 - shard: {}, followerId: {}, tweetId: {}", 
                    shardKey, followerId, tweetId);
        } catch (Exception e) {
            log.error("타임라인 저장 실패 - shard: {}, followerId: {}, tweetId: {}, error: {}", 
                    shardKey, followerId, tweetId, e.getMessage());
            // Fan-out 실패는 원본 트윗에 영향주지 않음
        } finally {
            // 이전 샤드로 복원
            if (currentShard != null) {
                DataSourceConfig.setShard(currentShard);
            } else {
                DataSourceConfig.clearShard();
            }
        }
    }

    /**
     * 사용자의 트윗 목록 조회 (커서 기반 페이지네이션)
     */
    public TweetListResponse getUserTweets(UUID userId, LocalDateTime lastTimestamp, int size) {
        // 크기 제한 (DoS 방지)
        size = Math.min(size, 50);
        
        List<TweetByUser> tweets;
            
            if (lastTimestamp == null) {
                tweets = tweetByUserRepository.findLatestTweets(userId)
                    .stream()
                    .limit(size)
                    .collect(Collectors.toList());
            } else {
                tweets = tweetByUserRepository.findTweetsWithCursor(userId, lastTimestamp)
                    .stream()
                    .limit(size)
                    .collect(Collectors.toList());
            }
            
            List<TweetResponse> tweetResponses = tweets.stream()
                .map(tweet -> new TweetResponse(
                    tweet.getKey().getTweetId(),
                    tweet.getKey().getUserId(),
                    tweet.getTweetText(),
                    tweet.getKey().getCreatedAt()
                ))
                .collect(Collectors.toList());
            
        LocalDateTime nextCursor = tweets.isEmpty() ? null 
            : tweets.get(tweets.size() - 1).getKey().getCreatedAt();
            
        return new TweetListResponse(tweetResponses, nextCursor, tweets.size() == size);
    }

    /**
     * 사용자의 타임라인 조회 (팔로우한 사용자들의 트윗)
     */
    public TweetListResponse getUserTimeline(UUID followerId, LocalDateTime lastTimestamp, int size) {
        // 크기 제한 (DoS 방지)
        size = Math.min(size, 50);
        
        List<UserTimeline> timeline;
            
            if (lastTimestamp == null) {
                timeline = userTimelineRepository.findLatestTimeline(followerId)
                    .stream()
                    .limit(size)
                    .collect(Collectors.toList());
            } else {
                timeline = userTimelineRepository.findTimelineWithCursor(followerId, lastTimestamp)
                    .stream()
                    .limit(size)
                    .collect(Collectors.toList());
            }
            
            List<TweetResponse> tweetResponses = timeline.stream()
                .map(t -> new TweetResponse(
                    t.getTweetId(),
                    t.getAuthorId(),
                    t.getTweetText(),
                    t.getCreatedAt()
                ))
                .collect(Collectors.toList());
            
        LocalDateTime nextCursor = timeline.isEmpty() ? null 
            : timeline.get(timeline.size() - 1).getCreatedAt();
            
        return new TweetListResponse(tweetResponses, nextCursor, timeline.size() == size);
    }
} 