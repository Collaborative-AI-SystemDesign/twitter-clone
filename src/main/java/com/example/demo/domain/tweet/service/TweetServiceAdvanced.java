package com.example.demo.domain.tweet.service;

import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchType;
import com.example.demo.domain.follow.FollowRepository;
import com.example.demo.domain.follow.FollowersByUser;
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
import org.springframework.data.cassandra.core.query.Criteria;
import org.springframework.data.cassandra.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.ArrayList;

import static org.springframework.data.cassandra.core.query.Query.query;
import static org.springframework.data.cassandra.core.query.Criteria.where;

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
        
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        
        log.info("최적화된 Fan-out 완료 - authorId: {}, 팔로워 수: {}, 소요시간: {}ms, 배치 수: {}", 
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
}
