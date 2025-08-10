package com.example.demo.domain.tweet.service;

import com.example.demo.domain.follow.FollowRepository;
import com.example.demo.domain.timeline.UserTimeline;
import com.example.demo.domain.timeline.UserTimelineRepository;
import com.example.demo.domain.tweet.entity.Tweet;
import com.example.demo.domain.tweet.entity.TweetByUser;
import com.example.demo.domain.tweet.repository.TweetByUserRepository;
import com.example.demo.domain.tweet.repository.TweetRepository;
import com.example.demo.domain.tweet.request.CreateTweetRequest;
import com.example.demo.domain.tweet.response.TweetResponse;
import com.example.demo.domain.tweet.response.TweetListResponse;
import com.example.demo.domain.tweet.dto.FanoutRetryMessage;
import com.example.demo.rabbitmq.RabbitMqService;
import com.example.demo.util.UUID.UUIDUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.cql.WriteOptions;

import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.ArrayList;



/**
 * 카산드라 최적화된 트윗 서비스
 * 
 * 핵심 최적화:
 * 1. 배치 처리 (1000개씩 분할)
 * 2. 비동기 병렬 처리 (CompletableFuture)
 * 3. ConsistencyLevel ONE 적용
 * 4. CassandraTemplate batchOps() 사용
 * 
 * 예상 성능: 10,000명 팬아웃 16초 → 1-2초
 */
@Slf4j
@Service("tweetServiceAdvanced")
@RequiredArgsConstructor
public class TweetServiceAdvanced {

    private final TweetRepository tweetRepository;
    private final TweetByUserRepository tweetByUserRepository;
    private final FollowRepository followRepository;
    private final UserTimelineRepository userTimelineRepository;
    private final RabbitMqService rabbitMqService;
    private final CassandraTemplate cassandraTemplate;
    private final ZSetOperations<String, Object> zSetOperations;
    
    @Qualifier("timelineWriteOptions")
    private final WriteOptions timelineWriteOptions;
    
    @Qualifier("batchWriteOptions")
    private final WriteOptions batchWriteOptions;

    // 배치 크기 (카산드라 실제 제한 고려: 50-100개가 안전)
    private static final int BATCH_SIZE = 100;
    
    // 고정 ThreadPool로 병렬도 제어 (CPU 코어 수의 2배 권장)
    private final ExecutorService batchExecutor = Executors.newFixedThreadPool(8);

    /**
     * 새 트윗 생성 + 최적화된 Fan-out-on-write
     * 
     * @Transactional 제거: Cassandra는 트랜잭션 DB가 아님
     */
    public TweetResponse createTweet(UUID userId, CreateTweetRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }
        
        UUID tweetId = UUIDUtil.generate();
        LocalDateTime now = LocalDateTime.now();

        // 1. 원본 트윗 저장 (핵심 데이터, 반드시 성공해야 함)
        Tweet tweet = Tweet.builder()
                .tweetId(tweetId)
                .userId(userId)
                .tweetText(request.getContent())
                .createdAt(now)
                .build();
        tweetRepository.save(tweet);

        // 2. 사용자별 트윗 저장 (작성자 본인의 트윗 목록)
        TweetByUser tweetByUser = TweetByUser.builder()
                .userId(userId)
                .tweetId(tweetId)
                .tweetText(request.getContent())
                .createdAt(now)
                .build();
        tweetByUserRepository.save(tweetByUser);

        // 3. 최적화된 Fan-out 시도
        try {
            optimizedFanOutToFollowers(userId, tweetId, request.getContent(), now);
        } catch (Exception e) {
            log.warn("Fan-out 실패, 재시도 큐로 전송 - userId: {}, tweetId: {}, error: {}", 
                    userId, tweetId, e.getMessage());
            sendToRetryQueue(userId, tweetId, request.getContent(), now, 0);
        }

        log.info("트윗 생성 완료 - userId: {}, tweetId: {}", userId, tweetId);
        
        return TweetResponse.of(tweet);
    }

    /**
     * 🚀 최적화된 팔로워 타임라인 Fan-out
     * 
     * 최적화 포인트:
     * 1. 배치 처리: 1000개씩 분할
     * 2. 비동기 병렬 처리: CompletableFuture
     * 3. 진짜 배치 Statement 사용
     * 4. ConsistencyLevel ONE 적용
     */
    private void optimizedFanOutToFollowers(UUID authorId, UUID tweetId, String tweetText, LocalDateTime createdAt) {
        long startTime = System.currentTimeMillis();
        
        // 1. 팔로워 목록 조회
        List<UUID> followerIds = followRepository.findByKeyFollowedUserId(authorId)
                .stream()
                .map(follower -> follower.getKey().getFollowerId())
                .toList();

        if (followerIds.isEmpty()) {
            log.debug("팔로워 없음 - authorId: {}", authorId);
            return;
        }

        log.info("최적화된 Fan-out 시작 - authorId: {}, 팔로워 수: {}", authorId, followerIds.size());

        // 2. UserTimeline 엔티티 생성
        List<UserTimeline> timelineEntries = followerIds.stream()
                .map(followerId -> UserTimeline.builder()
                        .followerId(followerId)
                        .tweetId(tweetId)
                        .authorId(authorId)
                        .tweetText(tweetText)
                        .createdAt(createdAt)
                        .build())
                .collect(Collectors.toList());

        // 3. 배치 처리 + 비동기 병렬 실행
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < timelineEntries.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, timelineEntries.size());
            List<UserTimeline> batch = timelineEntries.subList(i, end);
            int batchNumber = (i / BATCH_SIZE) + 1;
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                processBatch(batch, batchNumber);
            }, batchExecutor);
            futures.add(future);
        }
        
        // 4. 모든 배치 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // 5. 🚀 Redis 캐시에도 동시에 업데이트 (각 팔로워의 타임라인 캐시)
        TweetResponse tweetResponse = TweetResponse.builder()
                .tweetId(tweetId)
                .userId(authorId)
                .content(tweetText)
                .createdAt(createdAt)
                .build();
        updateRedisTimelineCaches(followerIds, tweetResponse);
        
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        
        log.info("최적화된 Fan-out + Redis 캐싱 완료 - authorId: {}, 팔로워 수: {}, 소요시간: {}ms, 배치 수: {}", 
                authorId, followerIds.size(), elapsedTime, futures.size());
    }

    /**
     * 개별 배치 처리 (CassandraTemplate.batchOps() 네이티브 배치 사용)
     * 
     * 🚀 최적화 포인트:
     * 1. Spring Data saveAll() → CassandraTemplate.batchOps().insert()
     * 2. 진짜 Cassandra BatchStatement 생성 (순차 insert X)
     * 3. ConsistencyLevel ONE 적용으로 빠른 쓰기
     * 4. 고정 ThreadPool로 병렬도 제어
     */
    private void processBatch(List<UserTimeline> batch, int batchNumber) {
        long batchStartTime = System.currentTimeMillis();
        
        try {
            // 🚀 CassandraTemplate 네이티브 배치 처리
            cassandraTemplate.batchOps()
                    .insert(batch, batchWriteOptions)
                    .execute();
            
            long batchEndTime = System.currentTimeMillis();
            log.debug("🚀 네이티브 배치 #{} 완료 - 크기: {}, 소요시간: {}ms", 
                    batchNumber, batch.size(), (batchEndTime - batchStartTime));
                    
        } catch (Exception e) {
            log.error("❌ 배치 #{} 실패 - 크기: {}, error: {}", batchNumber, batch.size(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Fan-out 재시도 실행 (큐에서 호출)
     */
    public void retryFanout(FanoutRetryMessage message) {
        log.info("Fan-out 재시도 실행 - authorId: {}, tweetId: {}, retryCount: {}", 
                message.getAuthorId(), message.getTweetId(), message.getRetryCount());
        
        optimizedFanOutToFollowers(
            message.getAuthorId(),
            message.getTweetId(),
            message.getTweetText(),
            message.getCreatedAt()
        );
    }

    /**
     * Fan-out 재시도 큐로 메시지 전송
     */
    private void sendToRetryQueue(UUID authorId, UUID tweetId, String tweetText, LocalDateTime createdAt, int retryCount) {
        try {
            FanoutRetryMessage retryMessage = new FanoutRetryMessage(
                authorId, tweetId, tweetText, createdAt, retryCount
            );
            rabbitMqService.sendMessage(retryMessage);
            log.info("Fan-out 재시도 큐 전송 완료 - authorId: {}, tweetId: {}, retryCount: {}", 
                    authorId, tweetId, retryCount);
        } catch (Exception e) {
            log.error("Fan-out 재시도 큐 전송 실패 - authorId: {}, tweetId: {}", authorId, tweetId, e);
        }
    }

    /**
     * 사용자의 트윗 목록 조회 (기존 로직과 동일)
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
            .map(tweet -> TweetResponse.builder()
                .tweetId(tweet.getKey().getTweetId())
                .userId(tweet.getKey().getUserId())
                .content(tweet.getTweetText())
                .createdAt(tweet.getKey().getCreatedAt())
                .build())
            .collect(Collectors.toList());
        
        LocalDateTime nextCursor = tweets.isEmpty() ? null 
            : tweets.get(tweets.size() - 1).getKey().getCreatedAt();
            
        return new TweetListResponse(tweetResponses, nextCursor, tweets.size() == size);
    }

    /**
     * 🚀 사용자의 타임라인 조회 (Redis SortedSet 최적화 버전)
     * 
     * 핵심 최적화:
     * 1. Redis SortedSet 활용: O(log N) 성능으로 빠른 조회
     * 2. Score 기반 시간 정렬: timestamp를 score로 사용
     * 3. 캐시 미스 시 Cassandra 폴백: 안정성 보장
     * 4. TTL 관리: 메모리 효율성
     * 
     * Redis Key 구조: "timeline:user:{userId}"
     * SortedSet Score: 트윗 생성시간의 timestamp (milliseconds)
     * SortedSet Value: 트윗 정보 JSON
     * 
     * @param userId 타임라인을 조회할 사용자 ID
     * @param lastTimestamp 커서 (이전 페이지의 마지막 시간)
     * @param size 조회할 트윗 개수 (최대 50개)
     * @return 타임라인 트윗 목록
     */
    public TweetListResponse getUserTimelineOptimized(UUID userId, LocalDateTime lastTimestamp, int size) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }
        
        size = Math.min(size, 50);
        
        String redisKey = "timeline:user:" + userId.toString();
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. Redis SortedSet에서 타임라인 조회
            List<TweetResponse> tweetResponses = getTimelineFromRedis(redisKey, lastTimestamp, size);
            
            if (!tweetResponses.isEmpty()) {
                long endTime = System.currentTimeMillis();
                log.info("🚀 Redis 타임라인 조회 성공 - userId: {}, 조회된 트윗 수: {}, 소요시간: {}ms", 
                        userId, tweetResponses.size(), (endTime - startTime));
                
                LocalDateTime nextCursor = tweetResponses.isEmpty() ? null 
                    : tweetResponses.get(tweetResponses.size() - 1).getCreatedAt();
                
                return new TweetListResponse(tweetResponses, nextCursor, tweetResponses.size() == size);
            }
            
            // 2. Redis 캐시 미스 - Cassandra에서 조회 후 Redis에 캐싱
            log.info("Redis 캐시 미스 - Cassandra 폴백 실행: userId={}", userId);
            return getUserTimelineFromCassandraAndCache(userId, lastTimestamp, size, redisKey);
            
        } catch (Exception e) {
            log.warn("Redis 타임라인 조회 실패 - Cassandra 폴백: userId={}, error={}", userId, e.getMessage());
            return getUserTimelineFromCassandraAndCache(userId, lastTimestamp, size, redisKey);
        }
    }

    /**
     * Redis SortedSet에서 타임라인 조회
     */
    private List<TweetResponse> getTimelineFromRedis(String redisKey, LocalDateTime lastTimestamp, int size) {
        try {
            // SortedSet에서 최신순으로 조회 (ZREVRANGEBYSCORE)
            double maxScore = lastTimestamp != null 
                ? lastTimestamp.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                : System.currentTimeMillis();
                
            // Redis SortedSet: 높은 score부터 조회 (최신순)
            java.util.Set<Object> rawResults = zSetOperations.reverseRangeByScore(
                redisKey, 
                0, 
                lastTimestamp != null ? maxScore - 1 : maxScore, // 커서 다음부터
                0, 
                size
            );
            
            if (rawResults == null || rawResults.isEmpty()) {
                return new ArrayList<>();
            }
            
            // JSON → TweetResponse 변환
            return rawResults.stream()
                .map(this::convertToTweetResponse)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Redis SortedSet 조회 실패: key={}, error={}", redisKey, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Cassandra에서 타임라인 조회 후 Redis에 캐싱
     */
    private TweetListResponse getUserTimelineFromCassandraAndCache(UUID userId, LocalDateTime lastTimestamp, int size, String redisKey) {
        long startTime = System.currentTimeMillis();
        
        // Cassandra에서 조회
        List<UserTimeline> timelineEntries;
        
        if (lastTimestamp == null) {
            timelineEntries = userTimelineRepository.findLatestTimeline(userId)
                    .stream()
                    .limit(size)
                    .collect(Collectors.toList());
        } else {
            timelineEntries = userTimelineRepository.findTimelineWithCursor(userId, lastTimestamp)
                    .stream()
                    .limit(size)
                    .collect(Collectors.toList());
        }
        
        // UserTimeline → TweetResponse 변환
        List<TweetResponse> tweetResponses = timelineEntries.stream()
            .map(timeline -> TweetResponse.builder()
                .tweetId(timeline.getKey().getTweetId())
                .userId(timeline.getAuthorId())
                .content(timeline.getTweetText())
                .createdAt(timeline.getKey().getCreatedAt())
                .build())
            .collect(Collectors.toList());
        
        // Redis에 캐싱 (비동기)
        CompletableFuture.runAsync(() -> {
            cacheTimelineToRedis(redisKey, tweetResponses);
        }, batchExecutor);
        
        LocalDateTime nextCursor = timelineEntries.isEmpty() ? null 
            : timelineEntries.get(timelineEntries.size() - 1).getKey().getCreatedAt();
        
        long endTime = System.currentTimeMillis();
        log.info("Cassandra 타임라인 조회 + Redis 캐싱 완료 - userId: {}, 조회된 트윗 수: {}, 소요시간: {}ms", 
                userId, tweetResponses.size(), (endTime - startTime));
        
        return new TweetListResponse(tweetResponses, nextCursor, timelineEntries.size() == size);
    }

    /**
     * Redis SortedSet에 타임라인 캐싱
     */
    private void cacheTimelineToRedis(String redisKey, List<TweetResponse> tweets) {
        try {
            if (tweets.isEmpty()) {
                return;
            }
            
            // SortedSet에 트윗들 추가
            for (TweetResponse tweet : tweets) {
                double score = tweet.getCreatedAt()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
                    
                zSetOperations.add(redisKey, tweet, score);
            }
            
            // TTL 설정: 1시간 (타임라인은 자주 변경되므로 짧게)
            zSetOperations.getOperations().expire(redisKey, java.time.Duration.ofHours(1));
            
            log.debug("Redis 타임라인 캐싱 완료: key={}, 트윗 수={}", redisKey, tweets.size());
            
        } catch (Exception e) {
            log.error("Redis 타임라인 캐싱 실패: key={}, error={}", redisKey, e.getMessage());
        }
    }

    /**
     * 🚀 Fan-out 시 각 팔로워의 Redis 타임라인 캐시 동시 업데이트
     * 
     * 새로운 트윗이 Fan-out될 때 각 팔로워의 Redis 캐시에도 추가
     * - 각 팔로워의 타임라인 캐시 키: "timeline:user:{followerId}"
     * - SortedSet에 새 트윗 추가 (Score: timestamp)
     * - 비동기 처리로 Fan-out 성능에 영향 최소화
     */
    private void updateRedisTimelineCaches(List<UUID> followerIds, TweetResponse newTweet) {
        CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            double score = newTweet.getCreatedAt()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
            
            int successCount = 0;
            for (UUID followerId : followerIds) {
                try {
                    String redisKey = "timeline:user:" + followerId.toString();
                    
                    // SortedSet에 새 트윗 추가
                    zSetOperations.add(redisKey, newTweet, score);
                    
                    // TTL 갱신 (1시간)
                    zSetOperations.getOperations().expire(redisKey, java.time.Duration.ofHours(1));
                    
                    successCount++;
                    
                } catch (Exception e) {
                    log.warn("팔로워 {} Redis 캐시 업데이트 실패: {}", followerId, e.getMessage());
                }
            }
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("🚀 Redis 타임라인 캐시 Fan-out 완료 - 대상: {}명, 성공: {}명, 소요시간: {}ms", 
                    followerIds.size(), successCount, elapsedTime);
                    
        }, batchExecutor).exceptionally(throwable -> {
            log.error("Redis 타임라인 캐시 Fan-out 실패", throwable);
            return null;
        });
    }

    /**
     * Object → TweetResponse 변환
     */
    private TweetResponse convertToTweetResponse(Object obj) {
        try {
            if (obj instanceof TweetResponse) {
                return (TweetResponse) obj;
            }
            if (obj instanceof java.util.Map<?, ?> mapObj) {
                Object tweetIdObj = mapObj.get("tweetId");
                Object userIdObj = mapObj.get("userId");
                Object contentObj = mapObj.get("content");
                Object createdAtObj = mapObj.get("createdAt");

                java.util.UUID tweetId = tweetIdObj != null ? java.util.UUID.fromString(tweetIdObj.toString()) : null;
                java.util.UUID userId = userIdObj != null ? java.util.UUID.fromString(userIdObj.toString()) : null;
                java.time.LocalDateTime createdAt;
                if (createdAtObj instanceof java.time.LocalDateTime ldt) {
                    createdAt = ldt;
                } else if (createdAtObj != null) {
                    createdAt = java.time.LocalDateTime.parse(createdAtObj.toString());
                } else {
                    createdAt = null;
                }

                return TweetResponse.builder()
                        .tweetId(tweetId)
                        .userId(userId)
                        .content(contentObj != null ? contentObj.toString() : null)
                        .createdAt(createdAt)
                        .build();
            }

            log.warn("Redis에서 예상치 못한 객체 타입: {}", obj.getClass());
            return null;
        } catch (Exception e) {
            log.error("TweetResponse 변환 실패: {}", e.getMessage());
            return null;
        }
    }
}
