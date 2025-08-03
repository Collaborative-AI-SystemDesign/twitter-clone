package com.example.demo.domain.tweet;

import com.example.demo.domain.follow.FollowRepository;
import com.example.demo.domain.follow.FollowersByUser;
import com.example.demo.domain.follow.FollowersByUserKey;

import com.example.demo.domain.timeline.UserTimelineRepository;
import com.example.demo.domain.tweet.entity.Tweet;
import com.example.demo.domain.tweet.entity.TweetByUser;
import com.example.demo.domain.tweet.entity.TweetByUserKey;
import com.example.demo.domain.tweet.repository.TweetByUserRepository;
import com.example.demo.domain.tweet.repository.TweetRepository;
import com.example.demo.domain.tweet.request.CreateTweetRequest;
import com.example.demo.domain.tweet.response.TweetResponse;
import com.example.demo.domain.tweet.response.TweetListResponse;
import com.example.demo.domain.tweet.service.TweetServiceAdvanced;
import com.example.demo.domain.tweet.dto.FanoutRetryMessage;
import com.example.demo.rabbitmq.RabbitMqService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.CassandraBatchOperations;
import org.springframework.data.cassandra.core.cql.WriteOptions;
import org.springframework.dao.DataAccessException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

/**
 * TweetServiceAdvanced 포괄적 단위 테스트
 * 
 * 테스트 범위:
 * 1. 정상 케이스 테스트
 * 2. 예외 상황 테스트 (모든 가능한 예외)
 * 3. 성능 및 최적화 테스트
 * 4. 동시성 및 배치 처리 테스트
 * 5. 재시도 메커니즘 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TweetServiceAdvanced 포괄적 테스트")
class TweetServiceAdvancedTest {

    @Mock
    private TweetRepository tweetRepository;
    
    @Mock
    private TweetByUserRepository tweetByUserRepository;
    
    @Mock
    private FollowRepository followRepository;
    
    @Mock
    private UserTimelineRepository userTimelineRepository;
    
    @Mock
    private RabbitMqService rabbitMqService;
    
    @Mock
    private CassandraTemplate cassandraTemplate;
    
    @Mock
    private WriteOptions timelineWriteOptions;
    
    @Mock
    private CassandraBatchOperations batchOperations;

    @InjectMocks
    private TweetServiceAdvanced tweetServiceAdvanced;

    private UUID userId;
    private UUID tweetId;
    private CreateTweetRequest createRequest;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        tweetId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        testTime = LocalDateTime.now();
        
        createRequest = new CreateTweetRequest();
        createRequest.setContent("최적화된 트윗 테스트 내용입니다 🚀");
    }

    @Nested
    @DisplayName("트윗 생성 테스트")
    class CreateTweetTest {

        @Test
        @DisplayName("정상 - 팔로워가 없는 경우")
        void createTweet_WithNoFollowers_Success() {
            // Given
            Tweet mockTweet = createMockTweet();
            TweetByUser mockTweetByUser = createMockTweetByUser();
            
            given(tweetRepository.save(any(Tweet.class))).willReturn(mockTweet);
            given(tweetByUserRepository.save(any(TweetByUser.class))).willReturn(mockTweetByUser);
            given(followRepository.findByKeyFollowedUserId(userId)).willReturn(Collections.emptyList());

            // When
            TweetResponse response = tweetServiceAdvanced.createTweet(userId, createRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEqualTo(createRequest.getContent());
            assertThat(response.getUserId()).isEqualTo(userId);
            
            verify(tweetRepository).save(any(Tweet.class));
            verify(tweetByUserRepository).save(any(TweetByUser.class));
            verify(followRepository).findByKeyFollowedUserId(userId);
            verifyNoInteractions(cassandraTemplate);
            verifyNoInteractions(rabbitMqService);
        }

        @Test
        @DisplayName("정상 - 팔로워가 있는 경우 (소규모)")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void createTweet_WithSmallFollowers_Success() {
            // Given
            Tweet mockTweet = createMockTweet();
            TweetByUser mockTweetByUser = createMockTweetByUser();
            List<FollowersByUser> followers = createMockFollowers(50); // 50명의 팔로워
            
            given(tweetRepository.save(any(Tweet.class))).willReturn(mockTweet);
            given(tweetByUserRepository.save(any(TweetByUser.class))).willReturn(mockTweetByUser);
            given(followRepository.findByKeyFollowedUserId(userId)).willReturn(followers);
            given(cassandraTemplate.batchOps()).willReturn(batchOperations);
            given(batchOperations.insert(anyList(), eq(timelineWriteOptions))).willReturn(batchOperations);

            // When
            TweetResponse response = tweetServiceAdvanced.createTweet(userId, createRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEqualTo(createRequest.getContent());
            
            verify(tweetRepository).save(any(Tweet.class));
            verify(tweetByUserRepository).save(any(TweetByUser.class));
            verify(followRepository).findByKeyFollowedUserId(userId);
            verify(cassandraTemplate).batchOps();
            verify(batchOperations).insert(anyList(), eq(timelineWriteOptions));
            verify(batchOperations).execute();
        }

        @Test
        @DisplayName("정상 - 대규모 팔로워 배치 처리")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void createTweet_WithLargeFollowers_BatchProcessing() {
            // Given
            Tweet mockTweet = createMockTweet();
            TweetByUser mockTweetByUser = createMockTweetByUser();
            List<FollowersByUser> followers = createMockFollowers(1500); // 1500명의 팔로워 (배치 15개)
            
            given(tweetRepository.save(any(Tweet.class))).willReturn(mockTweet);
            given(tweetByUserRepository.save(any(TweetByUser.class))).willReturn(mockTweetByUser);
            given(followRepository.findByKeyFollowedUserId(userId)).willReturn(followers);
            given(cassandraTemplate.batchOps()).willReturn(batchOperations);
            given(batchOperations.insert(anyList(), eq(timelineWriteOptions))).willReturn(batchOperations);

            // When
            TweetResponse response = tweetServiceAdvanced.createTweet(userId, createRequest);

            // Then
            assertThat(response).isNotNull();
            
            verify(tweetRepository).save(any(Tweet.class));
            verify(tweetByUserRepository).save(any(TweetByUser.class));
            verify(followRepository).findByKeyFollowedUserId(userId);
            
            // 배치 개수 확인 (1500 / 100 = 15개 배치)
            verify(cassandraTemplate, times(15)).batchOps();
            verify(batchOperations, times(15)).insert(anyList(), eq(timelineWriteOptions));
            verify(batchOperations, times(15)).execute();
        }

        @Test
        @DisplayName("예외 - userId가 null인 경우")
        void createTweet_WithNullUserId_ThrowsException() {
            // Given
            UUID nullUserId = null;

            // When & Then
            assertThatThrownBy(() -> tweetServiceAdvanced.createTweet(nullUserId, createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 ID는 필수입니다");
            
            verifyNoInteractions(tweetRepository);
            verifyNoInteractions(tweetByUserRepository);
            verifyNoInteractions(followRepository);
        }

        @Test
        @DisplayName("예외 - 트윗 저장 실패")
        void createTweet_TweetSaveFailure_ThrowsException() {
            // Given
            given(tweetRepository.save(any(Tweet.class)))
                .willThrow(new DataAccessException("Cassandra 연결 실패") {});

            // When & Then
            assertThatThrownBy(() -> tweetServiceAdvanced.createTweet(userId, createRequest))
                .isInstanceOf(DataAccessException.class)
                .hasMessage("Cassandra 연결 실패");
            
            verify(tweetRepository).save(any(Tweet.class));
            verifyNoInteractions(tweetByUserRepository);
            verifyNoInteractions(followRepository);
        }

        @Test
        @DisplayName("예외 - TweetByUser 저장 실패")
        void createTweet_TweetByUserSaveFailure_ThrowsException() {
            // Given
            Tweet mockTweet = createMockTweet();
            given(tweetRepository.save(any(Tweet.class))).willReturn(mockTweet);
            given(tweetByUserRepository.save(any(TweetByUser.class)))
                .willThrow(new DataAccessException("TweetByUser 저장 실패") {});

            // When & Then
            assertThatThrownBy(() -> tweetServiceAdvanced.createTweet(userId, createRequest))
                .isInstanceOf(DataAccessException.class)
                .hasMessage("TweetByUser 저장 실패");
            
            verify(tweetRepository).save(any(Tweet.class));
            verify(tweetByUserRepository).save(any(TweetByUser.class));
            verifyNoInteractions(followRepository);
        }

        @Test
        @DisplayName("예외 - Fan-out 실패 시 재시도 큐 전송")
        void createTweet_FanoutFailure_SendToRetryQueue() {
            // Given
            Tweet mockTweet = createMockTweet();
            TweetByUser mockTweetByUser = createMockTweetByUser();
            List<FollowersByUser> followers = createMockFollowers(100);
            
            given(tweetRepository.save(any(Tweet.class))).willReturn(mockTweet);
            given(tweetByUserRepository.save(any(TweetByUser.class))).willReturn(mockTweetByUser);
            given(followRepository.findByKeyFollowedUserId(userId)).willReturn(followers);
            given(cassandraTemplate.batchOps()).willReturn(batchOperations);
            given(batchOperations.insert(anyList(), eq(timelineWriteOptions))).willReturn(batchOperations);
            doThrow(new RuntimeException("배치 처리 실패")).when(batchOperations).execute();

            // When
            TweetResponse response = tweetServiceAdvanced.createTweet(userId, createRequest);

            // Then
            assertThat(response).isNotNull(); // 트윗 생성은 성공해야 함
            
            verify(tweetRepository).save(any(Tweet.class));
            verify(tweetByUserRepository).save(any(TweetByUser.class));
            verify(followRepository).findByKeyFollowedUserId(userId);
            verify(rabbitMqService).sendMessage(any(FanoutRetryMessage.class));
        }

        @Test
        @DisplayName("예외 - Fan-out 실패 + 재시도 큐 전송도 실패")
        void createTweet_FanoutAndRetryQueueFailure_StillSucceeds() {
            // Given
            Tweet mockTweet = createMockTweet();
            TweetByUser mockTweetByUser = createMockTweetByUser();
            List<FollowersByUser> followers = createMockFollowers(100);
            
            given(tweetRepository.save(any(Tweet.class))).willReturn(mockTweet);
            given(tweetByUserRepository.save(any(TweetByUser.class))).willReturn(mockTweetByUser);
            given(followRepository.findByKeyFollowedUserId(userId)).willReturn(followers);
            given(cassandraTemplate.batchOps()).willReturn(batchOperations);
            given(batchOperations.insert(anyList(), eq(timelineWriteOptions))).willReturn(batchOperations);
            doThrow(new RuntimeException("배치 처리 실패")).when(batchOperations).execute();
            doThrow(new RuntimeException("RabbitMQ 전송 실패")).when(rabbitMqService).sendMessage(any());

            // When
            TweetResponse response = tweetServiceAdvanced.createTweet(userId, createRequest);

            // Then
            assertThat(response).isNotNull(); // 트윗 생성은 여전히 성공해야 함
            
            verify(tweetRepository).save(any(Tweet.class));
            verify(tweetByUserRepository).save(any(TweetByUser.class));
            verify(followRepository).findByKeyFollowedUserId(userId);
            verify(rabbitMqService).sendMessage(any(FanoutRetryMessage.class));
        }
    }

    @Nested
    @DisplayName("팔로워 Fan-out 재시도 테스트")
    class RetryFanoutTest {

        @Test
        @DisplayName("정상 - 재시도 Fan-out 성공")
        void retryFanout_Success() {
            // Given
            List<FollowersByUser> followers = createMockFollowers(200);
            FanoutRetryMessage retryMessage = new FanoutRetryMessage(
                userId, tweetId, "재시도 트윗", testTime, 1
            );
            
            given(followRepository.findByKeyFollowedUserId(userId)).willReturn(followers);
            given(cassandraTemplate.batchOps()).willReturn(batchOperations);
            given(batchOperations.insert(anyList(), eq(timelineWriteOptions))).willReturn(batchOperations);

            // When
            assertThatNoException().isThrownBy(() -> 
                tweetServiceAdvanced.retryFanout(retryMessage)
            );

            // Then
            verify(followRepository).findByKeyFollowedUserId(userId);
            verify(cassandraTemplate, times(2)).batchOps(); // 200명 / 100 = 2배치
            verify(batchOperations, times(2)).insert(anyList(), eq(timelineWriteOptions));
            verify(batchOperations, times(2)).execute();
        }

        @Test
        @DisplayName("예외 - 재시도 Fan-out 실패")
        void retryFanout_Failure() {
            // Given
            List<FollowersByUser> followers = createMockFollowers(100);
            FanoutRetryMessage retryMessage = new FanoutRetryMessage(
                userId, tweetId, "재시도 트윗", testTime, 1
            );
            
            given(followRepository.findByKeyFollowedUserId(userId)).willReturn(followers);
            given(cassandraTemplate.batchOps()).willReturn(batchOperations);
            given(batchOperations.insert(anyList(), eq(timelineWriteOptions))).willReturn(batchOperations);
            doThrow(new RuntimeException("재시도 실패")).when(batchOperations).execute();

            // When & Then - CompletableFuture로 인해 CompletionException으로 래핑될 수 있음
            assertThatThrownBy(() -> tweetServiceAdvanced.retryFanout(retryMessage))
                .satisfiesAnyOf(
                    throwable -> assertThat(throwable).isInstanceOf(RuntimeException.class),
                    throwable -> assertThat(throwable).isInstanceOf(java.util.concurrent.CompletionException.class)
                );
        }
    }

    @Nested
    @DisplayName("사용자 트윗 조회 테스트")
    class GetUserTweetsTest {

        @Test
        @DisplayName("정상 - 첫 페이지 조회")
        void getUserTweets_FirstPage_Success() {
            // Given
            List<TweetByUser> mockTweets = createMockTweetsByUser(20);
            given(tweetByUserRepository.findLatestTweets(userId)).willReturn(mockTweets);

            // When
            TweetListResponse response = tweetServiceAdvanced.getUserTweets(userId, null, 10);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTweets()).hasSize(10);
            assertThat(response.isHasMore()).isTrue();
            assertThat(response.getNextCursor()).isNotNull();
            
            verify(tweetByUserRepository).findLatestTweets(userId);
            verify(tweetByUserRepository, never()).findTweetsWithCursor(any(), any());
        }

        @Test
        @DisplayName("정상 - 커서 기반 페이지 조회")
        void getUserTweets_WithCursor_Success() {
            // Given
            LocalDateTime cursor = testTime.minusDays(1);
            List<TweetByUser> mockTweets = createMockTweetsByUser(5);
            given(tweetByUserRepository.findTweetsWithCursor(userId, cursor)).willReturn(mockTweets);

            // When
            TweetListResponse response = tweetServiceAdvanced.getUserTweets(userId, cursor, 10);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTweets()).hasSize(5);
            assertThat(response.isHasMore()).isFalse();
            
            verify(tweetByUserRepository).findTweetsWithCursor(userId, cursor);
            verify(tweetByUserRepository, never()).findLatestTweets(any());
        }

        @Test
        @DisplayName("정상 - 크기 제한 적용")
        void getUserTweets_SizeLimitApplied() {
            // Given
            List<TweetByUser> mockTweets = createMockTweetsByUser(100);
            given(tweetByUserRepository.findLatestTweets(userId)).willReturn(mockTweets);

            // When
            TweetListResponse response = tweetServiceAdvanced.getUserTweets(userId, null, 200); // 200 요청

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTweets()).hasSize(50); // 최대 50개로 제한
            assertThat(response.isHasMore()).isTrue();
        }

        @Test
        @DisplayName("정상 - 빈 결과")
        void getUserTweets_EmptyResult() {
            // Given
            given(tweetByUserRepository.findLatestTweets(userId)).willReturn(Collections.emptyList());

            // When
            TweetListResponse response = tweetServiceAdvanced.getUserTweets(userId, null, 10);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTweets()).isEmpty();
            assertThat(response.isHasMore()).isFalse();
            assertThat(response.getNextCursor()).isNull();
        }

        @Test
        @DisplayName("예외 - Repository 조회 실패")
        void getUserTweets_RepositoryFailure() {
            // Given
            given(tweetByUserRepository.findLatestTweets(userId))
                .willThrow(new DataAccessException("DB 조회 실패") {});

            // When & Then
            assertThatThrownBy(() -> tweetServiceAdvanced.getUserTweets(userId, null, 10))
                .isInstanceOf(DataAccessException.class)
                .hasMessage("DB 조회 실패");
        }
    }

    @Nested
    @DisplayName("성능 및 최적화 테스트")
    class PerformanceTest {

        @Test
        @DisplayName("메모리 효율성 - 대량 팔로워 처리")
        @Timeout(value = 15, unit = TimeUnit.SECONDS)
        void createTweet_LargeFollowersMemoryEfficient() {
            // Given - 10,000명의 팔로워 (극한 상황)
            Tweet mockTweet = createMockTweet();
            TweetByUser mockTweetByUser = createMockTweetByUser();
            List<FollowersByUser> followers = createMockFollowers(10000);
            
            given(tweetRepository.save(any(Tweet.class))).willReturn(mockTweet);
            given(tweetByUserRepository.save(any(TweetByUser.class))).willReturn(mockTweetByUser);
            given(followRepository.findByKeyFollowedUserId(userId)).willReturn(followers);
            given(cassandraTemplate.batchOps()).willReturn(batchOperations);
            given(batchOperations.insert(anyList(), eq(timelineWriteOptions))).willReturn(batchOperations);

            // When
            TweetResponse response = tweetServiceAdvanced.createTweet(userId, createRequest);

            // Then
            assertThat(response).isNotNull();
            
            // 배치 개수 확인 (10,000 / 100 = 100개 배치)
            verify(cassandraTemplate, times(100)).batchOps();
            verify(batchOperations, times(100)).insert(anyList(), eq(timelineWriteOptions));
            verify(batchOperations, times(100)).execute();
        }

        @Test
        @DisplayName("타임아웃 - 배치 처리 시간 초과")
        @Timeout(value = 3, unit = TimeUnit.SECONDS)
        void createTweet_BatchProcessingTimeout() {
            // Given
            Tweet mockTweet = createMockTweet();
            TweetByUser mockTweetByUser = createMockTweetByUser();
            List<FollowersByUser> followers = createMockFollowers(1000);
            
            given(tweetRepository.save(any(Tweet.class))).willReturn(mockTweet);
            given(tweetByUserRepository.save(any(TweetByUser.class))).willReturn(mockTweetByUser);
            given(followRepository.findByKeyFollowedUserId(userId)).willReturn(followers);
            given(cassandraTemplate.batchOps()).willReturn(batchOperations);
            given(batchOperations.insert(anyList(), eq(timelineWriteOptions))).willReturn(batchOperations);
            
            // 처리 지연 시뮬레이션
            willAnswer(invocation -> {
                Thread.sleep(500); // 각 배치마다 500ms 지연
                return null;
            }).given(batchOperations).execute();

            // When & Then - 타임아웃 내에 완료되어야 함 (비동기 처리로 인해)
            assertThatNoException().isThrownBy(() -> 
                tweetServiceAdvanced.createTweet(userId, createRequest)
            );
        }
    }

    @Nested
    @DisplayName("동시성 및 스레드 안전성 테스트")
    class ConcurrencyTest {

        @Test
        @DisplayName("동시성 - 여러 스레드에서 동시 트윗 생성")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void createTweet_ConcurrentExecution() {
            // Given
            Tweet mockTweet = createMockTweet();
            TweetByUser mockTweetByUser = createMockTweetByUser();
            List<FollowersByUser> followers = createMockFollowers(500);
            
            given(tweetRepository.save(any(Tweet.class))).willReturn(mockTweet);
            given(tweetByUserRepository.save(any(TweetByUser.class))).willReturn(mockTweetByUser);
            given(followRepository.findByKeyFollowedUserId(any())).willReturn(followers);
            given(cassandraTemplate.batchOps()).willReturn(batchOperations);
            given(batchOperations.insert(anyList(), eq(timelineWriteOptions))).willReturn(batchOperations);

            // When - 5개 스레드에서 동시 실행
            List<UUID> userIds = IntStream.range(0, 5)
                .mapToObj(i -> UUID.randomUUID())
                .collect(Collectors.toList());

            // Then - 모든 트윗이 성공적으로 생성되어야 함
            assertThatNoException().isThrownBy(() -> {
                userIds.parallelStream().forEach(id -> {
                    tweetServiceAdvanced.createTweet(id, createRequest);
                });
            });

            verify(tweetRepository, times(5)).save(any(Tweet.class));
            verify(tweetByUserRepository, times(5)).save(any(TweetByUser.class));
        }
    }

    // Helper Methods
    private Tweet createMockTweet() {
        return Tweet.builder()
            .tweetId(tweetId)
            .userId(userId)
            .tweetText(createRequest.getContent())
            .createdAt(testTime)
            .build();
    }

    private TweetByUser createMockTweetByUser() {
        TweetByUserKey key = new TweetByUserKey();
        key.setUserId(userId);
        key.setTweetId(tweetId);
        key.setCreatedAt(testTime);
        
        TweetByUser tweetByUser = new TweetByUser();
        tweetByUser.setKey(key);
        tweetByUser.setTweetText(createRequest.getContent());
        return tweetByUser;
    }

    private List<FollowersByUser> createMockFollowers(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> {
                FollowersByUserKey key = new FollowersByUserKey();
                key.setFollowedUserId(userId);
                key.setFollowerId(UUID.randomUUID());
                
                FollowersByUser follower = new FollowersByUser();
                follower.setKey(key);
                return follower;
            })
            .collect(Collectors.toList());
    }

    private List<TweetByUser> createMockTweetsByUser(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> {
                TweetByUserKey key = new TweetByUserKey();
                key.setUserId(userId);
                key.setTweetId(UUID.randomUUID());
                key.setCreatedAt(testTime.minusMinutes(i));
                
                TweetByUser tweet = new TweetByUser();
                tweet.setKey(key);
                tweet.setTweetText("테스트 트윗 " + i);
                return tweet;
            })
            .collect(Collectors.toList());
    }
}