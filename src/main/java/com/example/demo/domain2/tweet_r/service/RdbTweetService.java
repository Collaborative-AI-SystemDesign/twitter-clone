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
    public TweetResponse createTweet(UUID userId, CreateTweetRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }
        
        UUID tweetId = UUIDUtil.generate();
        LocalDateTime now = LocalDateTime.now();

        try {
            // 1. 원본 트윗 저장 (트랜잭션 적용, 샤드 분리)
            saveTweetWithProperSharding(userId, tweetId, request.getContent(), now);

            // 2. Fan-out 시도 (팔로워 타임라인에 복사)
            fanOutToFollowers(userId, tweetId, request.getContent(), now);
            
        } catch (Exception e) {
            log.error("트윗 생성 실패 - userId: {}, tweetId: {}, error: {}", 
                    userId, tweetId, e.getMessage(), e);
            throw new RuntimeException("트윗 생성에 실패했습니다: " + e.getMessage(), e);
        } finally {
            // 샤드 정리
            DataSourceConfig.clearShard();
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
     * 원본 트윗과 사용자별 트윗을 적절한 샤드에 트랜잭션으로 저장
     */
    private void saveTweetWithProperSharding(UUID userId, UUID tweetId, String content, LocalDateTime createdAt) {
        // Controller에서 설정된 샤드를 무시하고 새로 설정
        String tweetDataShardKey = ShardUtil.selectTweetDataShardKeyByUserId(userId);
        log.info("=== 트윗 저장 샤드 설정 === userId: {}, 샤드: {}", userId, tweetDataShardKey);
        
        DataSourceConfig.setShard(tweetDataShardKey);
        
        try {
            saveTweetWithTransaction(userId, tweetId, content, createdAt);
            log.info("트윗 저장 완료 - userId: {}, tweetId: {}, 샤드: {}", userId, tweetId, tweetDataShardKey);
        } catch (Exception e) {
            log.error("트윗 저장 실패 - userId: {}, 샤드: {}, error: {}", userId, tweetDataShardKey, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 원본 트윗과 사용자별 트윗을 트랜잭션으로 저장 (샤드는 이미 설정됨)
     */
    @Transactional
    private void saveTweetWithTransaction(UUID userId, UUID tweetId, String content, LocalDateTime createdAt) {
        // 원본 트윗 저장
        saveTweetWithoutSharding(userId, tweetId, content, createdAt);
        
        // 사용자별 트윗 저장
        saveTweetByUserWithoutSharding(userId, tweetId, content, createdAt);
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
        log.info("=== Fan-out 디버깅 시작 === authorId: {}, tweetId: {}", authorId, tweetId);
        
        try {
            // 1. 팔로워 목록 조회 (authorID 기준 샤딩 - shard0)
            log.info("팔로워 조회 시작 - authorId: {}", authorId);
            List<UUID> followerIds = getFollowersWithSharding(authorId);
            log.info("팔로워 조회 결과 - authorId: {}, 팔로워 수: {}, 팔로워 목록: {}", 
                    authorId, followerIds.size(), followerIds);

            if (followerIds.isEmpty()) {
                log.warn("팔로워 없음 - authorId: {}", authorId);
                return;
            }

            log.info("Fan-out 시작 - authorId: {}, 팔로워 수: {}", authorId, followerIds.size());

            // 2. 각 팔로워의 타임라인에 트윗 추가 (followerID 기준 샤딩)
            int successCount = 0;
            int failureCount = 0;
            
            for (UUID followerId : followerIds) {
                try {
                    log.debug("개별 팔로워 Fan-out 시작 - followerId: {}, tweetId: {}", followerId, tweetId);
                    
                    // 재시도 로직 추가 (최대 3회)
                    boolean saved = false;
                    int retryCount = 0;
                    Exception lastException = null;
                    
                    while (!saved && retryCount < 3) {
                        try {
                            saveTimelineWithSharding(followerId, tweetId, authorId, tweetText, createdAt);
                            saved = true;
                            successCount++;
                            log.debug("개별 팔로워 Fan-out 성공 - followerId: {}, tweetId: {}, 시도: {}", 
                                    followerId, tweetId, retryCount + 1);
                        } catch (Exception e) {
                            lastException = e;
                            retryCount++;
                            log.warn("개별 팔로워 Fan-out 재시도 - followerId: {}, 시도: {}, 오류: {}", 
                                    followerId, retryCount, e.getMessage());
                            
                            if (retryCount < 3) {
                                Thread.sleep(100); // 100ms 대기 후 재시도
                            }
                        }
                    }
                    
                    if (!saved) {
                        failureCount++;
                        log.error("개별 팔로워 Fan-out 최종 실패 - followerId: {}, tweetId: {}, 최종 오류: {}", 
                                followerId, tweetId, lastException.getMessage(), lastException);
                    }
                    
                } catch (Exception e) {
                    failureCount++;
                    log.error("개별 팔로워 Fan-out 처리 중 예외 - followerId: {}, tweetId: {}, error: {}", 
                            followerId, tweetId, e.getMessage(), e);
                }
            }
            
            log.info("Fan-out 완료 - authorId: {}, 전체: {}, 성공: {}, 실패: {}, 성공률: {:.2f}%", 
                    authorId, followerIds.size(), successCount, failureCount, 
                    (double) successCount / followerIds.size() * 100);
                    
        } catch (Exception e) {
            log.error("Fan-out 전체 실패 - authorId: {}, tweetId: {}, error: {}", 
                    authorId, tweetId, e.getMessage(), e);
            // Fan-out 실패해도 원본 트윗은 유지
        }
        
        log.info("=== Fan-out 디버깅 종료 === authorId: {}, tweetId: {}", authorId, tweetId);
    }

    /**
     * 팔로워 목록 조회 (사용자 데이터는 항상 shard0)
     */
    private List<UUID> getFollowersWithSharding(UUID authorId) {
        String shardKey = ShardUtil.selectUserDataShardKey(); // 항상 shard0
        String currentShard = DataSourceConfig.getShard();
        
        log.info("=== 팔로워 조회 샤드 변경 === currentShard: {}, targetShard: {}", currentShard, shardKey);
        
        try {
            DataSourceConfig.setShard(shardKey);
            String actualShard = DataSourceConfig.getShard();
            log.info("샤드 설정 후 확인 - actualShard: {}", actualShard);
            
            // UUID VARCHAR(36) 변경으로 수정된 네이티브 쿼리 사용
            List<String> followerIdStrings = followRepository.findFollowerIdsAsString(authorId.toString());
            List<UUID> followers = followerIdStrings.stream()
                    .map(UUID::fromString)
                    .toList();
            
            log.info("팔로워 조회 완료 - authorId: {}, 팔로워 수: {}, 실제 사용된 샤드: {}", 
                    authorId, followers.size(), actualShard);
            
            // 실제 SQL이 어떤 샤드로 갔는지 확인하기 위해 로그 추가
            if (followers.isEmpty()) {
                log.warn("!!!주의!!! 팔로워가 0명으로 조회됨 - 샤드 문제일 가능성 있음");
                
                // 디버깅을 위해 JPQL 메서드도 시도해보기
                List<UUID> jpqlFollowers = followRepository.findFollowerIds(authorId);
                log.info("JPQL 방식 팔로워 조회 결과: {}", jpqlFollowers.size());
            }
            
            return followers;
        } catch (Exception e) {
            log.error("팔로워 조회 실패 - authorId: {}, error: {}", authorId, e.getMessage(), e);
            return new ArrayList<>(); // 빈 리스트 반환으로 안전하게 처리
        } finally {
            // 이전 샤드로 복원
            if (currentShard != null) {
                DataSourceConfig.setShard(currentShard);
                log.info("샤드 복원 완료 - restored to: {}", currentShard);
            } else {
                DataSourceConfig.clearShard();
                log.info("샤드 정리 완료");
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
        
        log.info("타임라인 저장 시작 - followerId: {}, shardKey: {}, currentShard: {}", 
                followerId, shardKey, currentShard);
        
        try {
            DataSourceConfig.setShard(shardKey);
            log.info("샤드 설정 완료 - new shard: {}", shardKey);
            
            UserTimeline timeline = UserTimeline.builder()
                    .followerId(followerId)
                    .tweetId(tweetId)
                    .authorId(authorId)
                    .tweetText(tweetText)
                    .createdAt(createdAt)
                    .build();
            
            log.info("UserTimeline 엔티티 생성 완료 - {}", timeline);
            
            UserTimeline savedTimeline = userTimelineRepository.save(timeline);
            log.info("타임라인 저장 성공 - savedId: {}, shard: {}, followerId: {}, tweetId: {}", 
                    savedTimeline.getId(), shardKey, followerId, tweetId);
            
            // 저장 검증
            try {
                long count = userTimelineRepository.count();
                log.info("현재 shard {} 의 user_timelines 총 개수: {}", shardKey, count);
            } catch (Exception countException) {
                log.warn("저장 후 개수 확인 실패: {}", countException.getMessage());
            }
            
        } catch (Exception e) {
            log.error("타임라인 저장 실패 - shard: {}, followerId: {}, tweetId: {}, error: {}", 
                    shardKey, followerId, tweetId, e.getMessage(), e);
            // Fan-out 실패는 원본 트윗에 영향주지 않음
        } finally {
            // 이전 샤드로 복원
            if (currentShard != null) {
                DataSourceConfig.setShard(currentShard);
                log.info("샤드 복원 완료 - restored to: {}", currentShard);
            } else {
                DataSourceConfig.clearShard();
                log.info("샤드 정리 완료");
            }
        }
    }

    /**
     * 사용자의 트윗 목록 조회 (커서 기반 페이지네이션)
     */
    public TweetListResponse getUserTweets(UUID userId, LocalDateTime lastTimestamp, int size) {
        // 크기 제한 (DoS 방지)
        size = Math.min(size, 50);
        
        // 트윗 데이터 조회용 샤드 설정
        String tweetDataShardKey = ShardUtil.selectTweetDataShardKeyByUserId(userId);
        log.info("=== 사용자 트윗 조회 샤드 설정 === userId: {}, 샤드: {}", userId, tweetDataShardKey);
        DataSourceConfig.setShard(tweetDataShardKey);
        
        try {
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
        } finally {
            DataSourceConfig.clearShard();
        }
    }

    /**
     * 사용자의 타임라인 조회 (팔로우한 사용자들의 트윗)
     */
    public TweetListResponse getUserTimeline(UUID followerId, LocalDateTime lastTimestamp, int size) {
        // 크기 제한 (DoS 방지)
        size = Math.min(size, 50);
        
        // 타임라인 데이터 조회용 샤드 설정 
        String tweetDataShardKey = ShardUtil.selectTweetDataShardKeyByUserId(followerId);
        log.info("=== 타임라인 조회 샤드 설정 === followerId: {}, 샤드: {}", followerId, tweetDataShardKey);
        DataSourceConfig.setShard(tweetDataShardKey);
        
        try {
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
        } finally {
            DataSourceConfig.clearShard();
        }
    }
} 