//package com.example.demo.domain.tweet;
//
//import com.example.demo.common.ApiResponse;
//import com.example.demo.DemoApplication;
//import com.example.demo.domain.tweet.service.TweetService;
//import com.example.demo.domain.tweet.service.TweetServiceAdvanced;
//import com.example.demo.domain.tweet.response.TweetListResponse;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.junit.jupiter.api.*;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Import;
//import org.springframework.context.annotation.EnableAspectJAutoProxy;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.lang.management.ManagementFactory;
//import java.util.UUID;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicLong;
//
//@SpringBootTest(classes = DemoApplication.class)
//@ActiveProfiles("test")
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@Import(TimelineDbLoadAndResourceComparisonTest.MetricsAspectConfig.class)
//class TimelineDbLoadAndResourceComparisonTest {
//
//    private static final int PAGE_SIZE = 20;
//    private static final int WARMUP_CALLS = 5;
//    private static final int MEASURE_CALLS = 200; // 충분한 표본으로 차이를 명확화
//
//    // 전역 카운터 (Aspect에서 접근)
//    static final AtomicLong CASSANDRA_QUERY_COUNT = new AtomicLong(0);
//    static final AtomicLong REDIS_ZRANGE_COUNT = new AtomicLong(0);
//
//    // ZSetOperations 주입 없이 AOP 계측만 사용
//
//    @org.springframework.beans.factory.annotation.Autowired
//    private TweetService tweetService;
//
//    @org.springframework.beans.factory.annotation.Autowired
//    private TweetServiceAdvanced tweetServiceAdvanced;
//
//    private UUID followerId;
//
//
//    @BeforeAll
//    void setUpData() {
//        followerId = UUID.randomUUID();
//    }
//
//    @BeforeEach
//    void resetCounters() {
//        CASSANDRA_QUERY_COUNT.set(0);
//        REDIS_ZRANGE_COUNT.set(0);
//    }
//
//    @Test
//    @Order(1)
//    @DisplayName("Cassandra vs Redis: DB 부하/리소스 비교 (단일 프로세스 관측치)")
//    void compareDbLoadAndResources() throws Exception {
//        // 1) 워밍업 (Redis 캐시 히트 보장)
//        for (int i = 0; i < WARMUP_CALLS; i++) {
//            tweetServiceAdvanced.getUserTimelineOptimized(followerId, null, PAGE_SIZE);
//        }
//
//        // 2) Redis 캐시 히트 측정
//        CASSANDRA_QUERY_COUNT.set(0);
//        REDIS_ZRANGE_COUNT.set(0);
//        long redisCpuStartNanos = getProcessCpuTimeNanos();
//        long redisMemStart = usedHeapBytes();
//        long redisStartMs = System.currentTimeMillis();
//
//        for (int i = 0; i < MEASURE_CALLS; i++) {
//            ApiResponse<TweetListResponse> res = new ApiResponse<>(true, null,
//                    tweetServiceAdvanced.getUserTimelineOptimized(followerId, null, PAGE_SIZE));
//            Assertions.assertNotNull(res.getData());
//        }
//        long redisElapsedMs = System.currentTimeMillis() - redisStartMs;
//        long redisCpuNanos = getProcessCpuTimeNanos() - redisCpuStartNanos;
//        long redisMemDelta = usedHeapBytes() - redisMemStart;
//        long redisZops = REDIS_ZRANGE_COUNT.get();
//        long redisCassQueries = CASSANDRA_QUERY_COUNT.get(); // 캐시 히트면 0에 가까워야 함
//
//        // 3) Cassandra 직접 조회 측정
//        CASSANDRA_QUERY_COUNT.set(0);
//        REDIS_ZRANGE_COUNT.set(0);
//        long cassCpuStartNanos = getProcessCpuTimeNanos();
//        long cassMemStart = usedHeapBytes();
//        long cassStartMs = System.currentTimeMillis();
//
//        for (int i = 0; i < MEASURE_CALLS; i++) {
//            ApiResponse<TweetListResponse> res = new ApiResponse<>(true, null,
//                    tweetService.getUserTimeline(followerId, null, PAGE_SIZE));
//            Assertions.assertNotNull(res.getData());
//        }
//        long cassElapsedMs = System.currentTimeMillis() - cassStartMs;
//        long cassCpuNanos = getProcessCpuTimeNanos() - cassCpuStartNanos;
//        long cassMemDelta = usedHeapBytes() - cassMemStart;
//        long cassQueries = CASSANDRA_QUERY_COUNT.get();
//        long cassZops = REDIS_ZRANGE_COUNT.get();
//
//        // 4) 결과 출력
//        System.out.println("\n========== DB 부하/리소스 비교 (MEASURE_CALLS=" + MEASURE_CALLS + ") ==========");
//        System.out.println("[Redis 캐시 히트]");
//        System.out.println(" - 총 응답시간(ms): " + redisElapsedMs);
//        System.out.println(" - 평균 응답(ms): " + String.format("%.2f", (double) redisElapsedMs / MEASURE_CALLS));
//        System.out.println(" - ZSET 조회 횟수(reverseRangeByScore): " + redisZops);
//        System.out.println(" - Cassandra 폴백 호출 수: " + redisCassQueries);
//        System.out.println(" - CPU 사용(ns): " + redisCpuNanos);
//        System.out.println(" - Heap 증감(bytes): " + redisMemDelta);
//
//        System.out.println("\n[Cassandra 직접 조회]");
//        System.out.println(" - 총 응답시간(ms): " + cassElapsedMs);
//        System.out.println(" - 평균 응답(ms): " + String.format("%.2f", (double) cassElapsedMs / MEASURE_CALLS));
//        System.out.println(" - Repository 호출 수(find*): " + cassQueries);
//        System.out.println(" - ZSET 조회 횟수: " + cassZops);
//        System.out.println(" - CPU 사용(ns): " + cassCpuNanos);
//        System.out.println(" - Heap 증감(bytes): " + cassMemDelta);
//
//        // 5) 간단 유효성: 캐시 히트 시 Cassandra 호출이 없어야 함
//        Assertions.assertTrue(redisCassQueries == 0,
//                "캐시 히트 경로에서 Cassandra 폴백이 과도합니다");
//    }
//
//    @Configuration
//    @EnableAspectJAutoProxy(proxyTargetClass = true)
//    static class MetricsAspectConfig {
//        @Bean
//        MetricsAspect metricsAspect() { return new MetricsAspect(); }
//    }
//
//    @Aspect
//    static class MetricsAspect {
//        // Cassandra Repository 호출 카운트 (find*)
//        @Around("execution(* com.example.demo.domain.timeline.UserTimelineRepository.find*(..))")
//        public Object aroundCassandraRepo(ProceedingJoinPoint pjp) throws Throwable {
//            CASSANDRA_QUERY_COUNT.incrementAndGet();
//            return pjp.proceed();
//        }
//
//        // Redis ZSET reverseRangeByScore 호출 카운트
//        @Around("execution(* org.springframework.data.redis.core.ZSetOperations.reverseRangeByScore(..))")
//        public Object aroundRedisZRange(ProceedingJoinPoint pjp) throws Throwable {
//            REDIS_ZRANGE_COUNT.incrementAndGet();
//            return pjp.proceed();
//        }
//    }
//
//    private static long usedHeapBytes() {
//        Runtime rt = Runtime.getRuntime();
//        return rt.totalMemory() - rt.freeMemory();
//    }
//
//    private static long getProcessCpuTimeNanos() throws Exception {
//        var osBean = ManagementFactory.getOperatingSystemMXBean();
//        // com.sun.management.OperatingSystemMXBean 사용 가능 시 CPU 시간 제공
//        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOs) {
//            long nanos = sunOs.getProcessCpuTime();
//            if (nanos >= 0) return nanos;
//        }
//        // 대안: 짧은 슬립으로 time 기반 근사 (정확도 낮음, fallback)
//        long before = System.nanoTime();
//        TimeUnit.MILLISECONDS.sleep(1);
//        return System.nanoTime() - before;
//    }
//}
//
//
