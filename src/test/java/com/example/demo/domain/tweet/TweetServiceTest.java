package com.example.demo.domain.tweet;

import com.example.demo.domain.follow.FollowRepository;
import com.example.demo.domain.timeline.UserTimelineRepository;
import com.example.demo.domain.tweet.entity.Tweet;
import com.example.demo.domain.tweet.entity.TweetByUser;
import com.example.demo.domain.tweet.repository.TweetByUserRepository;
import com.example.demo.domain.tweet.repository.TweetRepository;
import com.example.demo.domain.tweet.request.CreateTweetRequest;
import com.example.demo.domain.tweet.response.TweetResponse;
import com.example.demo.domain.tweet.response.TweetListResponse;
import com.example.demo.domain.tweet.service.TweetService;
import com.example.demo.rabbitmq.RabbitMqService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * TweetService 단위 테스트
 * 
 * 테스트 범위:
 * - 트윗 생성 로직 (핵심 비즈니스 로직만)
 * - 사용자별 트윗 조회
 * - Fan-out 실패 시 재시도 처리
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TweetService 단위 테스트")
class TweetServiceTest {

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

    @InjectMocks
    private TweetService tweetService;

    private UUID userId;
    private CreateTweetRequest createRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        createRequest = new CreateTweetRequest();
        createRequest.setContent("테스트 트윗 내용입니다 🐦");
    }

    @Test
    @DisplayName("트윗 생성 성공 - 팔로워가 없는 경우")
    void createTweet_WithNoFollowers_Success() {
        // Given
        Tweet mockTweet = createMockTweet();
        TweetByUser mockTweetByUser = createMockTweetByUser();
        
        given(tweetRepository.save(any(Tweet.class))).willReturn(mockTweet);
        given(tweetByUserRepository.save(any(TweetByUser.class))).willReturn(mockTweetByUser);
        given(followRepository.findFollowerIds(userId)).willReturn(Collections.emptyList());

        // When
        TweetResponse response = tweetService.createTweet(userId, createRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo(createRequest.getContent());
        
        verify(tweetRepository).save(any(Tweet.class));
        verify(tweetByUserRepository).save(any(TweetByUser.class));
        verify(followRepository).findFollowerIds(userId);
        verifyNoInteractions(userTimelineRepository);
        verifyNoInteractions(rabbitMqService);
    }

    @Test
    @DisplayName("트윗 생성 성공 - 팔로워가 있는 경우")
    void createTweet_WithFollowers_Success() {
        // Given
        Tweet mockTweet = createMockTweet();
        TweetByUser mockTweetByUser = createMockTweetByUser();
        List<UUID> followerIds = Arrays.asList(
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        given(tweetRepository.save(any(Tweet.class))).willReturn(mockTweet);
        given(tweetByUserRepository.save(any(TweetByUser.class))).willReturn(mockTweetByUser);
        given(followRepository.findFollowerIds(userId)).willReturn(followerIds);
        given(userTimelineRepository.saveAll(anyList())).willReturn(Collections.emptyList());

        // When
        TweetResponse response = tweetService.createTweet(userId, createRequest);

        // Then
        assertThat(response).isNotNull();
        
        verify(tweetRepository).save(any(Tweet.class));
        verify(tweetByUserRepository).save(any(TweetByUser.class));
        verify(followRepository).findFollowerIds(userId);
        verify(userTimelineRepository).saveAll(any());
        verifyNoInteractions(rabbitMqService);
    }

    @Test
    @DisplayName("트윗 생성 - Fan-out 실패 시 재시도 큐 전송")
    void createTweet_FanOutFails_SendToRetryQueue() {
        // Given
        Tweet mockTweet = createMockTweet();
        TweetByUser mockTweetByUser = createMockTweetByUser();
        
        given(tweetRepository.save(any(Tweet.class))).willReturn(mockTweet);
        given(tweetByUserRepository.save(any(TweetByUser.class))).willReturn(mockTweetByUser);
        given(followRepository.findFollowerIds(userId)).willThrow(new RuntimeException("Database connection failed"));

        // When
        TweetResponse response = tweetService.createTweet(userId, createRequest);

        // Then
        assertThat(response).isNotNull();
        
        verify(tweetRepository).save(any(Tweet.class));
        verify(tweetByUserRepository).save(any(TweetByUser.class));
        verify(followRepository).findFollowerIds(userId);
        verify(rabbitMqService).sendMessage(any());
        verifyNoInteractions(userTimelineRepository);
    }

    @Test
    @DisplayName("사용자 트윗 조회 성공")
    void getUserTweets_Success() {
        // Given
        List<TweetByUser> tweets = Arrays.asList(
            createMockTweetByUser(),
            createMockTweetByUser()
        );
        
        given(tweetByUserRepository.findLatestTweets(userId)).willReturn(tweets);

        // When
        TweetListResponse response = tweetService.getUserTweets(userId, null, 10);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTweets()).hasSize(2);
        
        verify(tweetByUserRepository).findLatestTweets(userId);
    }

    @Test
    @DisplayName("사용자 트윗 조회 - 커서 있는 경우")
    void getUserTweets_WithCursor_Success() {
        // Given
        LocalDateTime cursor = LocalDateTime.now().minusHours(1);
        List<TweetByUser> tweets = Arrays.asList(createMockTweetByUser());
        
        given(tweetByUserRepository.findTweetsWithCursor(userId, cursor)).willReturn(tweets);

        // When
        TweetListResponse response = tweetService.getUserTweets(userId, cursor, 10);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTweets()).hasSize(1);
        
        verify(tweetByUserRepository).findTweetsWithCursor(userId, cursor);
    }

    @Test
    @DisplayName("사용자 트윗 조회 - 트윗이 없는 경우")
    void getUserTweets_NoTweets_ReturnsEmpty() {
        // Given
        given(tweetByUserRepository.findLatestTweets(userId)).willReturn(Collections.emptyList());

        // When
        TweetListResponse response = tweetService.getUserTweets(userId, null, 10);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTweets()).isEmpty();
        assertThat(response.isHasMore()).isFalse();
    }

    /**
     * 테스트용 Mock Tweet 객체 생성
     */
    private Tweet createMockTweet() {
        LocalDateTime now = LocalDateTime.now();
        UUID tweetId = UUID.randomUUID();
        
        return Tweet.builder()
                .tweetId(tweetId)
                .userId(userId)
                .tweetText(createRequest.getContent())
                .build();
    }

    /**
     * 테스트용 Mock TweetByUser 객체 생성
     */
    private TweetByUser createMockTweetByUser() {
        LocalDateTime now = LocalDateTime.now();
        UUID tweetId = UUID.randomUUID();
        
        return TweetByUser.builder()
                .userId(userId)
                .tweetId(tweetId)
                .tweetText(createRequest.getContent())
                .createdAt(now)
                .build();
    }
} 