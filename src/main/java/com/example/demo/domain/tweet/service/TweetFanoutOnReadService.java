package com.example.demo.domain.tweet.service;

import com.example.demo.domain.follow.FollowRepository;
import com.example.demo.domain.follow.FollowingsByUserRepository;
import com.example.demo.domain.tweet.entity.Tweet;
import com.example.demo.domain.tweet.entity.TweetByUser;
import com.example.demo.domain.tweet.entity.TweetByUserKey;
import com.example.demo.domain.tweet.repository.TweetByUserRepository;
import com.example.demo.domain.tweet.repository.TweetRepository;
import com.example.demo.domain.tweet.request.CreateTweetRequest;
import com.example.demo.domain.tweet.response.TweetListResponse;
import com.example.demo.domain.tweet.response.TweetResponse;

import com.example.demo.util.UUID.UUIDUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TweetFanoutOnReadService {

  private final TweetRepository tweetRepository;
  private final TweetByUserRepository tweetByUserRepository;
  private final FollowRepository followRepository;
  private final FollowingsByUserRepository followingsByUserRepository;

  /**
   * 새 트윗 생성 (Fan-out on Read 방식)
   *
   * 특징: 본인 피드에만 저장하므로 매우 빠른 쓰기 성능
   */
  @Transactional
  public TweetResponse createTweet(UUID userId, CreateTweetRequest request) {
    if (userId == null) {
      throw new IllegalArgumentException("사용자 ID는 필수입니다");
    }

    UUID tweetId = UUIDUtil.generate();
    LocalDateTime now = LocalDateTime.now();

    // 1. 원본 트윗 저장
    Tweet tweet = Tweet.builder()
        .tweetId(tweetId)
        .userId(userId)
        .tweetText(request.getContent())
        .createdAt(now)
        .build();
    tweetRepository.save(tweet);

    // 2. 사용자별 트윗 저장 (작성자 본인만)
    TweetByUser tweetByUser = TweetByUser.builder()
        .userId(userId)
        .tweetId(tweetId)
        .tweetText(request.getContent())
        .createdAt(now)
        .build();
    tweetByUserRepository.save(tweetByUser);

    log.info("트윗 생성 완료 (Fan-out on Read) - userId: {}, tweetId: {}", userId, tweetId);

    // 3. 팔로워 수에 따라 분기
//    int followerCount = getFollowerCount(userId);
//
//    if (followerCount < CELEBRITY_THRESHOLD) {  // 예: 10만명
//      // 일반 사용자 → Fan-out on Write
//      log.info("일반 사용자 Fan-out on Write - userId: {}, 팔로워: {}명", userId, followerCount);
//      fanOutToFollowers(userId, tweetId, request.getContent(), now);
//    } else {
//      // 셀럽 → Fan-out on Read (Push 대신 Pull)
//      log.info("셀럽 사용자 Fan-out on Read - userId: {}, 팔로워: {}명", userId, followerCount);
//      // 아무것도 하지 않음 (실시간 조회에 의존)
//    }

    return TweetResponse.of(tweet);
  }

  /**
   * 사용자 타임라인 조회 (Fan-out on Read 방식)
   *
   * 실시간으로 팔로잉하는 모든 사용자의 트윗을 조회해서 시간순 병합
   */
  public TweetListResponse getTimeline(UUID userId, LocalDateTime lastTimestamp, int size) {
    log.info("타임라인 조회 시작 (Fan-out on Read) - userId: {}", userId);

    // 크기 제한
//    size = Math.min(size, 1000);

    // 1. 팔로잉하는 사용자들 조회 (실제로는 내가 팔로우하는 사람들의 반대 관계를 찾아야 함)
    List<UUID> followingIds = new ArrayList<>(getFollowingUserIds(userId));

    // 2. 본인도 포함 (본인 트윗도 타임라인에 표시)
    followingIds.add(userId);

    log.debug("팔로잉 사용자 수: {} (본인 포함)", followingIds.size());

    // 비동기 병렬 조회
    List<TweetByUser> allTweets = fetchTweetsFromMultipleUsersAsync(followingIds, lastTimestamp, size);

    // 4. 응답 변환
    List<TweetResponse> tweetResponses = allTweets.stream()
        .map(tweet -> new TweetResponse(
            tweet.getKey().getTweetId(),
            tweet.getKey().getUserId(),
            tweet.getTweetText(),
            tweet.getKey().getCreatedAt()
        ))
        .collect(Collectors.toList());

    LocalDateTime nextCursor = allTweets.isEmpty() ? null
        : allTweets.get(allTweets.size() - 1).getKey().getCreatedAt();

    log.info("타임라인 조회 완료 - userId: {}, 반환 트윗 수: {}", userId, tweetResponses.size());

    return new TweetListResponse(tweetResponses, nextCursor, allTweets.size() == size);
  }

  /**
   * 팔로잉하는 사용자 ID 목록 조회
   * 현재 Repository는 팔로워 조회용이므로, 실제로는 following 관계를 위한 별도 테이블/쿼리 필요
   */
  private List<UUID> getFollowingUserIds(UUID userId) {
    return followingsByUserRepository.findByKeyFollowerId(userId)
        .stream()
        .map(follow -> follow.getKey().getFollowedUserId())
        .toList();
  }

  /**
   * 여러 사용자의 트윗을 비동기로 병렬 조회 (성능 최적화)
   */
  private List<TweetByUser> fetchTweetsFromMultipleUsersAsync(List<UUID> userIds, LocalDateTime lastTimestamp, int size) {
    // 사용자가 너무 많으면 상위 50명만 제한 (성능 고려)
//    List<UUID> limitedUserIds = userIds.stream()
//        .limit(50)
//        .toList();
    System.out.println(size);
    List<UUID> limitedUserIds = userIds.stream()
        .limit(size)
        .toList();

    // 각 사용자별로 비동기 조회
    List<CompletableFuture<List<TweetByUser>>> futures = limitedUserIds.stream()
        .map(userId -> CompletableFuture.supplyAsync(() -> fetchUserTweets(userId, lastTimestamp, 10))) // 사용자당 10개씩
        .toList();

    // 모든 비동기 작업 완료 대기 및 결과 병합
    List<TweetByUser> allTweets = new ArrayList<>();
    for (CompletableFuture<List<TweetByUser>> future : futures) {
      try {
        allTweets.addAll(future.get());
      } catch (InterruptedException | ExecutionException e) {
        log.error("트윗 조회 중 오류 발생", e);
      }
    }

    // 시간순 정렬 (최신순) 후 제한된 개수만 반환
    return allTweets.stream()
        .sorted((t1, t2) -> t2.getKey().getCreatedAt().compareTo(t1.getKey().getCreatedAt()))
        .limit(size)
        .collect(Collectors.toList());
  }

  /**
   * 개별 사용자의 트윗 조회
   */
  private List<TweetByUser> fetchUserTweets(UUID userId, LocalDateTime lastTimestamp, int limit) {
    try {
      if (lastTimestamp == null) {
        return tweetByUserRepository.findLatestTweets(userId)
            .stream()
            .limit(limit)
            .collect(Collectors.toList());
      } else {
        return tweetByUserRepository.findTweetsWithCursor(userId, lastTimestamp)
            .stream()
            .limit(limit)
            .collect(Collectors.toList());
      }
    } catch (Exception e) {
      log.error("사용자 트윗 조회 실패 - userId: {}", userId, e);
      return new ArrayList<>();
    }
  }

  /**
   * 사용자의 개인 트윗 목록 조회 (기존과 동일)
   */
  public TweetListResponse getUserTweets(UUID userId, LocalDateTime lastTimestamp, int size) {
    size = Math.min(size, 50);

    List<com.example.demo.domain.tweet.entity.TweetByUser> tweets;

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
   * 특정 트윗 단건 조회
   */
  public TweetResponse getTweet(UUID tweetId) {
    Tweet tweet = tweetRepository.findById(tweetId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 트윗입니다"));

    return TweetResponse.of(tweet);
  }
}