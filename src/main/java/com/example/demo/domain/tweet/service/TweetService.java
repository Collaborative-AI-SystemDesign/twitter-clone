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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 트윗 비즈니스 로직 서비스
 * 
 * 핵심 기능:
 * - 트윗 생성 + Fan-out-on-write 전략
 * - 사용자별 트윗 조회 (커서 기반 페이지네이션)
 * - 재시도 메커니즘으로 안정성 보장
 * - 300M DAU 대응 성능 최적화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TweetService {

    private final TweetRepository tweetRepository;
    private final TweetByUserRepository tweetByUserRepository;
    private final FollowRepository followRepository;
    private final UserTimelineRepository userTimelineRepository;
    private final RabbitMqService rabbitMqService;

    /**
     * 새 트윗 생성 + Fan-out-on-write
     * 
     * 전략: 원본 트윗은 반드시 저장, Fan-out 실패 시에만 큐에서 재시도
     */
    @Transactional
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

        // 3. Fan-out 시도 (실패해도 트윗 생성은 성공)
        try {
            fanOutToFollowers(userId, tweetId, request.getContent(), now);
        } catch (Exception e) {
            log.warn("Fan-out 실패, 재시도 큐로 전송 - userId: {}, tweetId: {}, error: {}", 
                    userId, tweetId, e.getMessage());
            sendToRetryQueue(userId, tweetId, request.getContent(), now, 0);
        }

        log.info("트윗 생성 완료 - userId: {}, tweetId: {}", userId, tweetId);
        
        return TweetResponse.of(tweet);
    }

    /**
     * 팔로워들의 타임라인에 새 트윗 복사 (Fan-out-on-write)
     */
    private void fanOutToFollowers(UUID authorId, UUID tweetId, String tweetText, LocalDateTime createdAt) {
        // 1. 팔로워 목록 조회
        List<UUID> followerIds = followRepository.findByKeyFollowedUserId(authorId)
                .stream()
                .map(follower -> follower.getKey().getFollowerId())
                .toList();

        if (followerIds.isEmpty()) {
            log.debug("팔로워 없음 - authorId: {}", authorId);
            return;
        }

        // 2. Celebrity 사용자 체크 (팔로워 1000명 이상)
//        if (followerIds.size() > 1000) {
//            log.info("Celebrity 사용자 Fan-out - authorId: {}, 팔로워 수: {}", authorId, followerIds.size());
//            // 향후 Hybrid Fan-out 전략 적용 예정
//        }

        // 3. 각 팔로워의 타임라인에 트윗 추가
        List<UserTimeline> timelineEntries = followerIds.stream()
                .map(followerId -> UserTimeline.builder()
                        .followerId(followerId)
                        .tweetId(tweetId)
                        .authorId(authorId)
                        .tweetText(tweetText)
                        .createdAt(createdAt)  // 원본 시간 사용 (중복 방지)
                        .build())
                .collect(Collectors.toList());

        // 4. 배치 저장 (성능 최적화)
        userTimelineRepository.saveAll(timelineEntries);
        
        log.info("Fan-out 완료 - authorId: {}, 팔로워 수: {}", authorId, followerIds.size());
    }

    /**
     * Fan-out 재시도 실행 (큐에서 호출)
     */
    public void retryFanout(FanoutRetryMessage message) {
        log.info("Fan-out 재시도 실행 - authorId: {}, tweetId: {}, retryCount: {}", 
                message.getAuthorId(), message.getTweetId(), message.getRetryCount());
        
        fanOutToFollowers(
            message.getAuthorId(),
            message.getTweetId(),
            message.getTweetText(),
            message.getCreatedAt()  // 원본 시간 그대로 사용 (중복 방지)
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
 * 🔥 사용자의 타임라인 조회 (Cassandra 직접 조회 버전)
 * 
 * 특징:
 * - Redis SortedSet을 사용하지 않는 버전
 * - Cassandra UserTimeline 테이블에서 직접 조회
 * - Fan-out-on-write 전략으로 미리 저장된 데이터 활용
 * - 커서 기반 페이지네이션 지원
 * 
 * @param userId 타임라인을 조회할 사용자 ID
 * @param lastTimestamp 커서 (이전 페이지의 마지막 시간)
 * @param size 조회할 트윗 개수 (최대 50개)
 * @return 타임라인 트윗 목록
 */
public TweetListResponse getUserTimeline(UUID userId, LocalDateTime lastTimestamp, int size) {
    if (userId == null) {
        throw new IllegalArgumentException("사용자 ID는 필수입니다");
    }
    
    size = Math.min(size, 50);
    
    List<UserTimeline> timelineEntries;
    
    if (lastTimestamp == null) {
        // 최신 타임라인 조회
        timelineEntries = userTimelineRepository.findLatestTimeline(userId)
                .stream()
                .limit(size)
                .collect(Collectors.toList());
    } else {
        // 커서 기반 페이지네이션
        timelineEntries = userTimelineRepository.findTimelineWithCursor(userId, lastTimestamp)
                .stream()
                .limit(size)
                .collect(Collectors.toList());
    }
    
    // UserTimeline → TweetResponse 변환
    List<TweetResponse> tweetResponses = timelineEntries.stream()
        .map(timeline -> TweetResponse.builder()
            .tweetId(timeline.getKey().getTweetId())
            .userId(timeline.getAuthorId())    // 작성자 ID
            .content(timeline.getTweetText())
            .createdAt(timeline.getKey().getCreatedAt())
            .build())
        .collect(Collectors.toList());
    
    // 다음 커서 설정
    LocalDateTime nextCursor = timelineEntries.isEmpty() ? null 
        : timelineEntries.get(timelineEntries.size() - 1).getKey().getCreatedAt();
    
    log.info("Cassandra 타임라인 조회 완료 - userId: {}, 조회된 트윗 수: {}", userId, tweetResponses.size());
    
    return new TweetListResponse(tweetResponses, nextCursor, timelineEntries.size() == size);
}
}
