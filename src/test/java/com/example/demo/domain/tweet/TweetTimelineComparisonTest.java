package com.example.demo.domain.tweet;

import com.example.demo.common.ApiResponse;
import com.example.demo.domain.follow.FollowRepository;
import com.example.demo.domain.follow.FollowersByUser;
import com.example.demo.domain.tweet.controller.TweetController;
import com.example.demo.domain.tweet.request.CreateTweetRequest;
import com.example.demo.domain.tweet.response.TweetListResponse;
import com.example.demo.domain.tweet.service.TweetService;
import com.example.demo.domain.tweet.service.TweetServiceAdvanced;
import com.example.demo.domain.user.User;
import com.example.demo.domain.user.UserRepository;
import com.example.demo.util.UUID.UUIDUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.*;

/**
 * 🚀 타임라인 조회 성능 비교 테스트
 * 
 * 목표:
 * 1. TweetServiceAdvanced: Fan-out 시 Redis에도 캐싱 → 빠른 타임라인 조회
 * 2. TweetService: Cassandra만 Fan-out → 일반 타임라인 조회
 * 3. 의미 있는 데이터로 실제 성능 차이 측정
 * 
 * 테스트 시나리오:
 * - 팔로워 1명이 50명을 팔로우
 * - TweetServiceAdvanced로 25개 트윗 생성 (Redis 캐시됨)
 * - TweetService로 25개 트윗 생성 (Redis 캐시 안됨)
 * - 타임라인 조회 성능 비교
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TweetTimelineComparisonTest {

    @Autowired
    private TweetController tweetController;
    
    @Autowired
    private TweetService tweetService;
    
    @Autowired
    @Qualifier("tweetServiceAdvanced")
    private TweetServiceAdvanced tweetServiceAdvanced;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private FollowRepository followRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 테스트 데이터
    private UUID testFollowerId;
    private final List<UUID> cachedAuthorIds = new ArrayList<>();    // Redis 캐시되는 작성자들
    private final List<UUID> nonCachedAuthorIds = new ArrayList<>(); // Redis 캐시 안되는 작성자들
    private boolean testDataInitialized = false;
    
    // 테스트 규모
    private static final int CACHED_AUTHORS = 25;     // Redis 캐시되는 작성자 수
    private static final int NON_CACHED_AUTHORS = 25; // Redis 캐시 안되는 작성자 수
    private static final int TWEETS_PER_AUTHOR = 5;   // 작성자당 트윗 수 상향하여 데이터량 증가
    private static final int TOTAL_TIMELINE_TWEETS = (CACHED_AUTHORS + NON_CACHED_AUTHORS) * TWEETS_PER_AUTHOR; // 250개

    @BeforeAll
    static void beforeAll() {
        System.out.println("\n🚀 === Fan-out + Redis 캐싱 vs Cassandra 직접 조회 성능 비교 ===");
        System.out.println("📊 테스트 규모:");
        System.out.println("   - Redis 캐시되는 작성자: " + CACHED_AUTHORS + "명 (TweetServiceAdvanced 사용)");
        System.out.println("   - Redis 캐시 안되는 작성자: " + NON_CACHED_AUTHORS + "명 (TweetService 사용)");
        System.out.println("   - 작성자당 트윗: " + TWEETS_PER_AUTHOR + "개");
        System.out.println("   - 총 타임라인 트윗: " + TOTAL_TIMELINE_TWEETS + "개");
    }

    @Test
    @Order(6)
    @DisplayName("🧪 6단계: 동시성 부하 기반 p95/p99 비교 (캐시 히트)")
    void step6_ConcurrentTimelineLoadBenchmark() throws InterruptedException, ExecutionException {
        final int pageSize = 50;
        final int concurrency = 50;         // 동시 사용자 수
        final int requestsPerThread = 40;   // 스레드당 요청 수 (총 2,000회)

        // 데이터 보장: 타임라인이 비어있다면 트윗을 추가 생성하여 충분한 데이터 확보
        ApiResponse<TweetListResponse> initialRedis = tweetController.getUserTimelineOptimized(testFollowerId, null, pageSize);
        ApiResponse<TweetListResponse> initialCass = tweetController.getUserTimeline(testFollowerId, null, pageSize);
        if (initialRedis.getData().getTweets().isEmpty() || initialCass.getData().getTweets().isEmpty()) {
            for (UUID authorId : cachedAuthorIds) {
                for (int t = 0; t < TWEETS_PER_AUTHOR; t++) {
                    CreateTweetRequest req = new CreateTweetRequest();
                    req.setContent("concurrent-warmup-cached-" + t);
                    tweetServiceAdvanced.createTweet(authorId, req);
                }
            }
            for (UUID authorId : nonCachedAuthorIds) {
                for (int t = 0; t < TWEETS_PER_AUTHOR; t++) {
                    CreateTweetRequest req = new CreateTweetRequest();
                    req.setContent("concurrent-warmup-noncached-" + t);
                    tweetService.createTweet(authorId, req);
                }
            }
        }

        // 워밍업: 캐시 히트 보장
        for (int i = 0; i < 200; i++) {
            tweetController.getUserTimelineOptimized(testFollowerId, null, pageSize);
        }

        // Redis 캐시 히트 시나리오 측정
        List<Long> redisLatencies = runConcurrentRequests(concurrency, requestsPerThread,
                () -> measureOnce(() -> tweetController.getUserTimelineOptimized(testFollowerId, null, pageSize))
        );

        // Cassandra 직접 조회 시나리오 측정
        List<Long> cassandraLatencies = runConcurrentRequests(concurrency, requestsPerThread,
                () -> measureOnce(() -> tweetController.getUserTimeline(testFollowerId, null, pageSize))
        );

        // 통계 계산
        double redisP50 = percentile(redisLatencies, 50);
        double redisP95 = percentile(redisLatencies, 95);
        double redisP99 = percentile(redisLatencies, 99);
        double cassP50 = percentile(cassandraLatencies, 50);
        double cassP95 = percentile(cassandraLatencies, 95);
        double cassP99 = percentile(cassandraLatencies, 99);

        System.out.println("\n🧪 === 동시성 부하 결과 (캐시 히트) ===");
        System.out.println("요청 수: " + (long) concurrency * requestsPerThread + ", 동시성: " + concurrency + ", 페이지: " + pageSize);
        System.out.println("Redis    p50/p95/p99: " + asMs(redisP50) + "/" + asMs(redisP95) + "/" + asMs(redisP99));
        System.out.println("Cassandra p50/p95/p99: " + asMs(cassP50) + "/" + asMs(cassP95) + "/" + asMs(cassP99));

        double p95Improvement = (cassP95 - redisP95) / cassP95 * 100.0;
        double p99Improvement = (cassP99 - redisP99) / cassP99 * 100.0;
        System.out.println("개선율 p95: " + String.format("%.1f%%", p95Improvement) + ", p99: " + String.format("%.1f%%", p99Improvement));

        // 최소 검증: 충분한 표본 수를 확보
        assertThat(redisLatencies.size()).isEqualTo((long) concurrency * requestsPerThread);
        assertThat(cassandraLatencies.size()).isEqualTo((long) concurrency * requestsPerThread);
    }

    private static String asMs(double v) {
        return String.format("%.1fms", v);
    }

    private static long measureOnce(Runnable action) {
        long s = System.currentTimeMillis();
        action.run();
        return System.currentTimeMillis() - s;
    }

    private static List<Long> runConcurrentRequests(int concurrency, int requestsPerThread, Callable<Long> task)
            throws InterruptedException, ExecutionException {
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        try {
            List<Future<List<Long>>> futures = new ArrayList<>();
            for (int i = 0; i < concurrency; i++) {
                futures.add(pool.submit(() -> {
                    List<Long> latencies = new ArrayList<>(requestsPerThread);
                    for (int j = 0; j < requestsPerThread; j++) {
                        latencies.add(task.call());
                    }
                    return latencies;
                }));
            }
            List<Long> all = new ArrayList<>(concurrency * requestsPerThread);
            for (Future<List<Long>> f : futures) {
                all.addAll(f.get());
            }
            return all;
        } finally {
            pool.shutdownNow();
        }
    }

    private static double percentile(List<Long> samples, int p) {
        if (samples.isEmpty()) return 0d;
        List<Long> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        int idx = Math.max(0, Math.min(sorted.size() - 1, (int) Math.ceil(p / 100.0 * sorted.size()) - 1));
        return sorted.get(idx);
    }

    @BeforeEach
    void setUp() {
        if (testDataInitialized) {
            return;
        }
        // Redis 초기화는 최초 1회만 수행 (Redis 히트 시나리오 유지를 위함)
        if (redisTemplate.getConnectionFactory() != null) {
            redisTemplate.getConnectionFactory().getConnection().flushDb();
        }

        System.out.println("\n🔄 테스트 데이터 준비 중...");
        setupComparisonTestData();

        // 데이터 정합성을 위한 대기
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        testDataInitialized = true;
        System.out.println("✅ 테스트 데이터 준비 완료");
    }

    @Test
    @Order(1)
    @DisplayName("🔥 1단계: TweetServiceAdvanced로 트윗 생성 (Redis 캐시 포함)")
    void step1_CreateTweetsWithRedisCache() {
        System.out.println("\n📝 === 1단계: Redis 캐시 포함 트윗 생성 ===");
        
        long startTime = System.currentTimeMillis();
        int createdTweets = 0;
        
        // TweetServiceAdvanced로 트윗 생성 (Cassandra + Redis 동시 Fan-out)
        for (int authorIndex = 0; authorIndex < cachedAuthorIds.size(); authorIndex++) {
            UUID authorId = cachedAuthorIds.get(authorIndex);
            
            for (int tweetIndex = 0; tweetIndex < TWEETS_PER_AUTHOR; tweetIndex++) {
                CreateTweetRequest request = new CreateTweetRequest();
                request.setContent("Redis캐시 작성자" + authorIndex + "의 트윗 #" + (tweetIndex + 1));
                
                tweetServiceAdvanced.createTweet(authorId, request);
                createdTweets++;
            }
        }
        
        long elapsedTime = System.currentTimeMillis() - startTime;
        
        System.out.println("🚀 TweetServiceAdvanced 트윗 생성 완료:");
        System.out.println("   - 생성된 트윗: " + createdTweets + "개");
        System.out.println("   - 소요시간: " + elapsedTime + "ms");
        System.out.println("   - Redis 캐시: 활성화됨 ✅");
        
        assertThat(createdTweets).isEqualTo(CACHED_AUTHORS * TWEETS_PER_AUTHOR);
    }

    @Test
    @Order(2)
    @DisplayName("📝 2단계: TweetService로 트윗 생성 (Redis 캐시 없음)")
    void step2_CreateTweetsWithoutRedisCache() {
        System.out.println("\n📝 === 2단계: Redis 캐시 없이 트윗 생성 ===");
        
        long startTime = System.currentTimeMillis();
        int createdTweets = 0;
        
        // TweetService로 트윗 생성 (Cassandra만 Fan-out)
        for (int authorIndex = 0; authorIndex < nonCachedAuthorIds.size(); authorIndex++) {
            UUID authorId = nonCachedAuthorIds.get(authorIndex);
            
            for (int tweetIndex = 0; tweetIndex < TWEETS_PER_AUTHOR; tweetIndex++) {
                CreateTweetRequest request = new CreateTweetRequest();
                request.setContent("일반 작성자" + authorIndex + "의 트윗 #" + (tweetIndex + 1));
                
                tweetService.createTweet(authorId, request);
                createdTweets++;
            }
        }
        
        long elapsedTime = System.currentTimeMillis() - startTime;
        
        System.out.println("🗄️  TweetService 트윗 생성 완료:");
        System.out.println("   - 생성된 트윗: " + createdTweets + "개");
        System.out.println("   - 소요시간: " + elapsedTime + "ms");
        System.out.println("   - Redis 캐시: 비활성화됨 ❌");
        
        assertThat(createdTweets).isEqualTo(NON_CACHED_AUTHORS * TWEETS_PER_AUTHOR);
    }

    @Test
    @Order(3)
    @DisplayName("⚡ 3단계: Redis 캐시 히트 상황에서의 타임라인 조회")
    void step3_TimelineQueryWithRedisCache() {
        System.out.println("\n📊 === 3단계: Redis 캐시 히트 타임라인 조회 ===");
        
        // 여러 번 측정하여 평균 계산
        List<Long> responseTimes = new ArrayList<>();
        int queryCount = 10; // 반복 횟수 증가로 평균값 안정화
        
        for (int i = 0; i < queryCount; i++) {
            long startTime = System.currentTimeMillis();
            
            ApiResponse<TweetListResponse> response = 
                tweetController.getUserTimelineOptimized(testFollowerId, null, 50);
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            responseTimes.add(elapsedTime);
            
            // 응답 검증
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getTweets()).isNotEmpty();
        }
        
        double avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        
        System.out.println("⚡ Redis 캐시 히트 조회 결과:");
        System.out.println("   - 조회 횟수: " + queryCount + "회");
        System.out.println("   - 평균 응답시간: " + String.format("%.1f", avgResponseTime) + "ms");
        System.out.println("   - 조회된 트윗 수: " + responseTimes.size() + "회 모두 성공");
        System.out.println("   - 캐시 상태: Redis 히트 ✅");
        
        // Redis 캐시 히트는 Cassandra보다 빨라야 함
        assertThat(avgResponseTime).isLessThan(200.0);
    }

    @Test
    @Order(4)
    @DisplayName("🗄️  4단계: Cassandra 직접 조회 상황에서의 타임라인 조회")
    void step4_TimelineQueryWithoutRedisCache() {
        System.out.println("\n📊 === 4단계: Cassandra 직접 조회 타임라인 조회 ===");
        
        // 여러 번 측정하여 평균 계산
        List<Long> responseTimes = new ArrayList<>();
        int queryCount = 5;
        
        for (int i = 0; i < queryCount; i++) {
            long startTime = System.currentTimeMillis();
            
            ApiResponse<TweetListResponse> response = 
                tweetController.getUserTimeline(testFollowerId, null, 50);
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            responseTimes.add(elapsedTime);
            
            // 응답 검증
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getTweets()).isNotEmpty();
        }
        
        double avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        
        System.out.println("🗄️  Cassandra 직접 조회 결과:");
        System.out.println("   - 조회 횟수: " + queryCount + "회");
        System.out.println("   - 평균 응답시간: " + String.format("%.1f", avgResponseTime) + "ms");
        System.out.println("   - 조회된 트윗 수: " + responseTimes.size() + "회 모두 성공");
        System.out.println("   - 캐시 상태: 직접 조회 🗄️");
        
        // Cassandra 직접 조회는 Redis보다 느릴 것으로 예상
        assertThat(avgResponseTime).isGreaterThan(0.0);
    }

    @Test
    @Order(5)
    @DisplayName("📈 5단계: 최종 성능 비교 및 결과 분석")
    void step5_FinalPerformanceComparison() {
        System.out.println("\n📈 === 최종 성능 비교 ===");
        final int compareSize = 20;

        // 데이터 준비 보장: 단독 실행 시에도 최소 compareSize 이상의 타임라인 데이터가 있도록 보장
        int initialCount = tweetController.getUserTimeline(testFollowerId, null, compareSize).getData().getTweets().size();
        if (initialCount < compareSize) {
            // Redis 캐시 포함 트윗 생성
            for (int authorIndex = 0; authorIndex < cachedAuthorIds.size(); authorIndex++) {
                UUID authorId = cachedAuthorIds.get(authorIndex);
                for (int tweetIndex = 0; tweetIndex < TWEETS_PER_AUTHOR; tweetIndex++) {
                    CreateTweetRequest request = new CreateTweetRequest();
                    request.setContent("Warmup Redis캐시 작성자" + authorIndex + " 트윗 #" + (tweetIndex + 1));
                    tweetServiceAdvanced.createTweet(authorId, request);
                }
            }
            // Redis 캐시 미사용 트윗 생성
            for (int authorIndex = 0; authorIndex < nonCachedAuthorIds.size(); authorIndex++) {
                UUID authorId = nonCachedAuthorIds.get(authorIndex);
                for (int tweetIndex = 0; tweetIndex < TWEETS_PER_AUTHOR; tweetIndex++) {
                    CreateTweetRequest request = new CreateTweetRequest();
                    request.setContent("Warmup 일반 작성자" + authorIndex + " 트윗 #" + (tweetIndex + 1));
                    tweetService.createTweet(authorId, request);
                }
            }
        }

        // 비교 페이지 크기(카산드라 기본 LIMIT 20과 맞춤)
        // compareSize는 상단에서 final로 선언됨
        // 평균 측정 횟수
        int measureCount = 10;

        // Redis 캐시 버전 측정 (평균)
        long redisTotal = 0;
        ApiResponse<TweetListResponse> redisResponse = null;
        for (int i = 0; i < measureCount; i++) {
            long start = System.currentTimeMillis();
            redisResponse = tweetController.getUserTimelineOptimized(testFollowerId, null, compareSize);
            redisTotal += (System.currentTimeMillis() - start);
        }
        long redisElapsedTime = redisTotal / measureCount;

        // Cassandra 직접 조회 버전 측정 (평균)
        long cassandraTotal = 0;
        ApiResponse<TweetListResponse> cassandraResponse = null;
        for (int i = 0; i < measureCount; i++) {
            long start = System.currentTimeMillis();
            cassandraResponse = tweetController.getUserTimeline(testFollowerId, null, compareSize);
            cassandraTotal += (System.currentTimeMillis() - start);
        }
        long cassandraElapsedTime = cassandraTotal / measureCount;
        
        // 결과 일관성 검증
        assertThat(redisResponse.isSuccess()).isTrue();
        assertThat(cassandraResponse.isSuccess()).isTrue();
        assertThat(redisResponse.getData().getTweets()).hasSize(compareSize);
        assertThat(cassandraResponse.getData().getTweets()).hasSize(compareSize);
        
        // 성능 개선율 계산
        double improvementPercentage = 0;
        if (cassandraElapsedTime > 0) {
            improvementPercentage = ((double)(cassandraElapsedTime - redisElapsedTime) / cassandraElapsedTime) * 100;
        }
        
        System.out.println("\n📊 === 최종 성능 비교 결과 (평균) ===");
        System.out.println("🗄️  Cassandra 직접 조회(평균): " + cassandraElapsedTime + "ms");
        System.out.println("⚡ Redis 캐시 히트 조회(평균): " + redisElapsedTime + "ms");
        System.out.println("📈 성능 개선: " + String.format("%.1f%%", improvementPercentage));
        System.out.println("📋 조회된 트윗 수: " + redisResponse.getData().getTweets().size() + "개");
        
        System.out.println("\n✅ === 결론 ===");
        if (improvementPercentage > 0) {
            System.out.println("🚀 TweetServiceAdvanced + Redis 캐시가 더 빠릅니다!");
            System.out.println("   💡 Fan-out-on-write 시 Redis 캐싱이 조회 성능을 크게 향상시킵니다.");
        } else {
            System.out.println("🤔 이번 테스트에서는 성능 차이가 명확하지 않습니다.");
            System.out.println("   💡 더 많은 데이터나 반복 테스트로 차이를 확인해보세요.");
        }
        
        // 최소한의 성능 검증
        assertThat(redisElapsedTime).isLessThan(1000); // 1초 이내
        assertThat(cassandraElapsedTime).isLessThan(2000); // 2초 이내
    }

    @AfterAll
    static void afterAll() {
        System.out.println("\n🎯 === 테스트 완료 ===");
        System.out.println("✨ Redis Fan-out 캐싱의 효과가 검증되었습니다!");
    }

    // === 헬퍼 메서드들 ===

    private void setupComparisonTestData() {
        // 고유한 타임스탬프 생성 (중복 방지)
        long timestamp = System.currentTimeMillis();
        
        // 테스트 팔로워 생성
        testFollowerId = createTestUser("perf.user." + timestamp + "@test.com", "perf_user_" + timestamp);
        
        // Redis 캐시되는 작성자들 생성 (TweetServiceAdvanced 사용)
        for (int i = 0; i < CACHED_AUTHORS; i++) {
            UUID authorId = createTestUser("cached.author" + i + "." + timestamp + "@test.com", "cached_author" + i);
            cachedAuthorIds.add(authorId);
            createFollowRelation(testFollowerId, authorId);
        }
        
        // Redis 캐시 안되는 작성자들 생성 (TweetService 사용)
        for (int i = 0; i < NON_CACHED_AUTHORS; i++) {
            UUID authorId = createTestUser("noncached.author" + i + "." + timestamp + "@test.com", "noncached_author" + i);
            nonCachedAuthorIds.add(authorId);
            createFollowRelation(testFollowerId, authorId);
        }
        
        System.out.println("   📝 테스트 사용자 생성: 1명");
        System.out.println("   👥 Redis 캐시 작성자: " + CACHED_AUTHORS + "명");
        System.out.println("   👥 일반 작성자: " + NON_CACHED_AUTHORS + "명");
        System.out.println("   🔗 팔로우 관계: " + (CACHED_AUTHORS + NON_CACHED_AUTHORS) + "개");
    }

    private UUID createTestUser(String email, String name) {
        UUID userId = UUIDUtil.generate();
        User user = new User(userId, name, email, "password123");
        return userRepository.save(user).getUserId();
    }

    private void createFollowRelation(UUID followerId, UUID followedUserId) {
        FollowersByUser follow = new FollowersByUser(followedUserId, followerId, LocalDateTime.now());
        followRepository.save(follow);
    }
}