package com.example.demo.domain.tweet.service;

import static org.junit.jupiter.api.Assertions.*;

import com.example.demo.domain.follow.FollowRepository;
import com.example.demo.domain.follow.FollowersByUser;
import com.example.demo.domain.follow.FollowingsByUser;
import com.example.demo.domain.follow.FollowingsByUserRepository;
import com.example.demo.domain.timeline.UserTimeline;
import com.example.demo.domain.timeline.UserTimelineRepository;
import com.example.demo.domain.timeline.service.TimelineService;
import com.example.demo.domain.tweet.entity.Tweet;
import com.example.demo.domain.tweet.entity.TweetByUser;
import com.example.demo.domain.tweet.repository.TweetByUserRepository;
import com.example.demo.domain.tweet.repository.TweetRepository;
import com.example.demo.domain.tweet.response.TweetListResponse;
import com.example.demo.domain.user.User;
import com.example.demo.domain.user.UserRepository;
import com.example.demo.util.UUID.UUIDUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class TweetFanoutOnReadServiceTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TweetRepository tweetRepository;

  @Autowired
  private TweetByUserRepository tweetByUserRepository;

  @Autowired
  private FollowRepository followersByUserRepository;

  @Autowired
  private FollowingsByUserRepository followingsByUserRepository;

  @Autowired
  private UserTimelineRepository userTimelineRepository;

  // 실제 서비스 클래스들 주입
  @Autowired
  private TweetFanoutOnReadService fanoutOnReadService;

  @Autowired
  private TimelineService timelineService;


  private static final UUID USER_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"); // 1명 팔로우
  private static final UUID USER_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"); // 10명 팔로우
  private static final UUID USER_C = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"); // 100명 팔로우
  private static final UUID USER_D = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"); // 1000명 팔로우

  @Test
  public void generateAllDummyData() {
    log.info("=== 더미 데이터 생성 시작 ===");

    // 1. 사용자 생성
    List<UUID> celebrityIds = createUsers();

    // 2. 팔로우 관계 설정
    createFollowRelationships(celebrityIds);

    // 3. 트윗 생성
    createTweets(celebrityIds);

    // 4. Fan-out on Write용 타임라인 생성
    createUserTimelines(celebrityIds);

    log.info("=== 더미 데이터 생성 완료 ===");
  }

  /**
   * 1. 사용자 생성 (셀럽 1000명 + 테스트 사용자 4명)
   */
  private List<UUID> createUsers() {
    log.info("사용자 생성 시작...");

    List<UUID> celebrityIds = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();

    // 셀럽 1000명 생성
    IntStream.range(1, 1001).forEach(i -> {
      UUID celebrityId = UUIDUtil.generate();
      celebrityIds.add(celebrityId);

      User celebrity = new User(
          celebrityId,
          String.format("Celebrity %03d", i),
          String.format("celebrity%03d@test.com", i),
          "password"
      );
//      celebrity.setCreatedAt(now);

      userRepository.save(celebrity);

      if (i % 100 == 0) {
        log.info("셀럽 {}명 생성 완료", i);
      }
    });

    // 테스트 사용자 4명 생성
    List<User> testUsers = Arrays.asList(
        new User(USER_A, "User A (1 following)", "userA@test.com", "password"),
        new User(USER_B, "User B (10 following)", "userB@test.com", "password"),
        new User(USER_C, "User C (100 following)", "userC@test.com", "password"),
        new User(USER_D, "User D (1000 following)", "userD@test.com", "password")
    );

    testUsers.forEach(user -> {
//      user.setCreatedAt(now);
      userRepository.save(user);
    });

    log.info("사용자 생성 완료 - 셀럽: {}명, 테스트 사용자: {}명", celebrityIds.size(), testUsers.size());
    return celebrityIds;
  }

  /**
   * 2. 팔로우 관계 설정
   */
  private void createFollowRelationships(List<UUID> celebrityIds) {
    log.info("팔로우 관계 설정 시작...");
    LocalDateTime now = LocalDateTime.now();

    // User A: 1명 팔로우
    createFollowRelation(USER_A, celebrityIds.get(0), now);

    // User B: 10명 팔로우
    IntStream.range(0, 10).forEach(i ->
        createFollowRelation(USER_B, celebrityIds.get(i), now)
    );

    // User C: 100명 팔로우
    IntStream.range(0, 100).forEach(i ->
        createFollowRelation(USER_C, celebrityIds.get(i), now)
    );

    // User D: 1000명 팔로우
    IntStream.range(0, 1000).forEach(i ->
        createFollowRelation(USER_D, celebrityIds.get(i), now)
    );

    log.info("팔로우 관계 설정 완료");
  }

  /**
   * 개별 팔로우 관계 생성
   */
  private void createFollowRelation(UUID followerId, UUID followedUserId, LocalDateTime createdAt) {
    // followings_by_user 테이블 (팔로워가 팔로우하는 사람들)
    FollowingsByUser following = new FollowingsByUser(followerId, followedUserId, createdAt);
    followingsByUserRepository.save(following);

    // followers_by_user 테이블 (팔로우 받는 사람의 팔로워들)
    FollowersByUser follower = new FollowersByUser(followedUserId, followerId, createdAt);
    followersByUserRepository.save(follower);
  }

  /**
   * 3. 트윗 생성 (각 셀럽당 20개씩)
   */
  private void createTweets(List<UUID> celebrityIds) {
    log.info("트윗 생성 시작...");

    for (int i = 0; i < celebrityIds.size(); i++) {
      UUID celebrityId = celebrityIds.get(i);

      // 각 셀럽당 20개의 트윗 생성
      int finalI = i;
      IntStream.range(1, 21).forEach(tweetNum -> {
        UUID tweetId = UUIDUtil.generate();
        // 시간을 분산시켜서 최신 순서가 섞이도록 설정
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(tweetNum + (finalI * 20));
        String tweetText = String.format("Celebrity %03d - Tweet %02d", finalI + 1, tweetNum);

        // tweets 테이블에 저장
        Tweet tweet = Tweet.builder()
            .tweetId(tweetId)
            .userId(celebrityId)
            .tweetText(tweetText)
            .createdAt(createdAt)
            .build();
        tweetRepository.save(tweet);

        // tweets_by_user 테이블에 저장
        TweetByUser tweetByUser = TweetByUser.builder()
            .userId(celebrityId)
            .tweetId(tweetId)
            .tweetText(tweetText)
            .createdAt(createdAt)
            .build();
        tweetByUserRepository.save(tweetByUser);
      });

      if ((i + 1) % 100 == 0) {
        log.info("{}명의 셀럽 트윗 생성 완료 (총 {}개 트윗)", i + 1, (i + 1) * 20);
      }
    }

    log.info("트윗 생성 완료 - 총 {}개 트윗", celebrityIds.size() * 20);
  }

  /**
   * 4. Fan-out on Write용 사용자 타임라인 생성
   */
  private void createUserTimelines(List<UUID> celebrityIds) {
    log.info("사용자 타임라인 생성 시작...");

    // User A 타임라인 (1명 팔로우 - 20개 트윗)
    createUserTimeline(USER_A, celebrityIds.subList(0, 1));

    // User B 타임라인 (10명 팔로우 - 각자의 최신 트윗 2개씩 = 20개)
    createUserTimeline(USER_B, celebrityIds.subList(0, 10));

    // User C 타임라인 (100명 팔로우 - 최신 20개만)
    createUserTimeline(USER_C, celebrityIds.subList(0, 100));

    // User D 타임라인 (1000명 팔로우 - 최신 20개만)
    createUserTimeline(USER_D, celebrityIds.subList(0, 1000));

    log.info("사용자 타임라인 생성 완료");
  }

  /**
   * 개별 사용자의 타임라인 생성
   */
  private void createUserTimeline(UUID followerId, List<UUID> followedUserIds) {
    List<UserTimeline> timelineEntries = new ArrayList<>();

    // 각 팔로우하는 사용자의 트윗들을 수집
    for (UUID followedUserId : followedUserIds) {
      // 해당 사용자의 최신 트윗 몇 개씩 가져오기
      int tweetsPerUser = Math.min(20 / followedUserIds.size() + 1, 5); // 최대 5개씩

      IntStream.range(1, tweetsPerUser + 1).forEach(tweetNum -> {
        UUID tweetId = UUIDUtil.generate();
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(tweetNum + timelineEntries.size());
        String tweetText = String.format("Timeline tweet from %s - %d", followedUserId.toString().substring(0, 8), tweetNum);

        UserTimeline timeline = UserTimeline.builder()
            .followerId(followerId)
            .tweetId(tweetId)
            .authorId(followedUserId)
            .tweetText(tweetText)
            .createdAt(createdAt)
            .build();

        timelineEntries.add(timeline);
      });
    }

    // 시간순 정렬 후 상위 20개만 저장
    timelineEntries.stream()
        .sorted((t1, t2) -> t2.getKey().getCreatedAt().compareTo(t1.getKey().getCreatedAt()))
        .limit(20)
        .forEach(userTimelineRepository::save);

    log.info("사용자 {} 타임라인 생성 완료 - {}개 엔트리",
        followerId.toString().substring(0, 8), Math.min(timelineEntries.size(), 20));
  }

  /**
   * 생성된 데이터 검증용 테스트
   */
  @Test
  public void verifyDummyData() {
    log.info("=== 생성된 더미 데이터 검증 ===");

    // User A 타임라인 확인 (Fan-out on Write)
    List<UserTimeline> userATimeline = userTimelineRepository.findLatestTimeline(USER_A);
    log.info("User A 타임라인 개수: {}", userATimeline.size());

    // User A 팔로잉 확인 (Fan-out on Read 용)
    List<FollowingsByUser> userAFollowings = followingsByUserRepository.findByKeyFollowerId(USER_A);
    log.info("User A 팔로잉 개수: {}", userAFollowings.size());

    // User B 팔로잉 확인
    List<FollowingsByUser> userBFollowings = followingsByUserRepository.findByKeyFollowerId(USER_B);
    log.info("User B 팔로잉 개수: {}", userBFollowings.size());

    // User C 팔로잉 확인
    List<FollowingsByUser> userCFollowings = followingsByUserRepository.findByKeyFollowerId(USER_C);
    log.info("User C 팔로잉 개수: {}", userCFollowings.size());

    // User D 팔로잉 확인
    List<FollowingsByUser> userDFollowings = followingsByUserRepository.findByKeyFollowerId(USER_D);
    log.info("User D 팔로잉 개수: {}", userDFollowings.size());

    log.info("=== 데이터 검증 완료 ===");
  }

  /**
   * 모든 사용자별 Fan-out 성능 비교 테스트
   */
  @Test
  public void compareAllFanOutPerformance() {
    log.info("\n=== Fan-out 성능 비교 테스트 시작 ===");

    Map<String, UUID> testUsers = Map.of(
        "1명 팔로우", USER_A,
        "10명 팔로우", USER_B,
        "100명 팔로우", USER_C,
        "1000명 팔로우", USER_D
    );

    // 각 사용자별로 성능 테스트 실행
    testUsers.forEach((description, userId) -> {
      log.info("\n--- {} 사용자 성능 테스트 ---", description);

      // Fan-out on Write 테스트
      long writeTime = testFanOutOnWrite(userId);

      // Fan-out on Read 테스트
      long readTime = testFanOutOnRead(userId);

      // 결과 비교
      log.info("{} 결과:", description);
      log.info("  Fan-out on Write: {}ms", writeTime);
      log.info("  Fan-out on Read: {}ms", readTime);
      log.info("  성능 차이: {}배 (Read가 Write보다 {})",
          String.format("%.2f", (double)readTime / writeTime),
          readTime > writeTime ? "느림" : "빠름");
    });

    log.info("\n=== 성능 비교 테스트 완료 ===");
  }

  /**
   * 개별 사용자의 Fan-out on Write 성능 테스트
   */
  @Test
  public void testFanOutOnWriteIndividualTimeline() {
    testFanOutOnWrite(USER_B);
  }

  /**
   * 개별 사용자의 Fan-out on Read 성능 테스트
   */
  @Test
  public void testFanOutOnRead1Following() {
    testFanOutOnRead(USER_A);
  }

  @Test
  public void testFanOutOnRead10Following() {
    testFanOutOnRead(USER_B);
  }

  @Test
  public void testFanOutOnRead100Following() {
    testFanOutOnRead(USER_C);
  }

  @Test
  public void testFanOutOnRead1000Following() {
    testFanOutOnRead(USER_D);
  }

  /**
   * Fan-out on Write 성능 측정 (기존 타임라인 조회)
   */
  private long testFanOutOnWrite(UUID userId) {
    long startTime = System.nanoTime();

    // 실제 TimelineService 사용
    List<UserTimeline> timeline = timelineService.getLatestTimeline(userId);

    long endTime = System.nanoTime();
    long elapsedMs = (endTime - startTime) / 1_000_000;

    log.info("Fan-out on Write - 조회된 타임라인 수: {}, 소요 시간: {}ms", timeline.size(), elapsedMs);
    return elapsedMs;
  }

  /**
   * Fan-out on Read 성능 측정 (실제 서비스 사용)
   */
  private long testFanOutOnRead(UUID userId) {
    long startTime = System.nanoTime();

    // 실제 TweetFanoutOnReadService.getTimeline() 사용
    TweetListResponse response = fanoutOnReadService.getTimeline(userId, null, 20);

    long endTime = System.nanoTime();
    long elapsedMs = (endTime - startTime) / 1_000_000;

    log.info("Fan-out on Read - 조회된 트윗 수: {}, 소요 시간: {}ms",
        response.getTweets().size(), elapsedMs);
    return elapsedMs;
  }

  /**
   * 연속 성능 테스트 (여러 번 실행하여 평균 측정)
   */
  @Test
  public void performanceBenchmark() {
    log.info("\n=== 벤치마크 테스트 (각 10회 실행) ===");

    Map<String, UUID> testUsers = Map.of(
        "1명", USER_A,
        "10명", USER_B,
        "100명", USER_C,
        "1000명", USER_D
    );

    int iterations = 10;

    testUsers.forEach((description, userId) -> {
      log.info("\n--- {} 팔로우 사용자 벤치마크 ---", description);

      // Fan-out on Write 평균 측정
      long totalWriteTime = 0;
      for (int i = 0; i < iterations; i++) {
        totalWriteTime += testFanOutOnWrite(userId);
        try { Thread.sleep(100); } catch (InterruptedException e) {}
      }
      long avgWriteTime = totalWriteTime / iterations;

      // Fan-out on Read 평균 측정
      long totalReadTime = 0;
      for (int i = 0; i < iterations; i++) {
        totalReadTime += testFanOutOnRead(userId);
        try { Thread.sleep(100); } catch (InterruptedException e) {}
      }
      long avgReadTime = totalReadTime / iterations;

      log.info("{} 팔로우 평균 결과 ({}회 실행):", description, iterations);
      log.info("  Fan-out on Write 평균: {}ms", avgWriteTime);
      log.info("  Fan-out on Read 평균: {}ms", avgReadTime);
      log.info("  성능 차이: {}배", String.format("%.2f", (double)avgReadTime / avgWriteTime));
    });

    log.info("\n=== 벤치마크 테스트 완료 ===");
  }
}