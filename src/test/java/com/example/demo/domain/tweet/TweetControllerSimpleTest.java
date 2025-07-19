package com.example.demo.domain.tweet;

import com.example.demo.common.ApiResponse;
import com.example.demo.domain.tweet.controller.TweetController;
import com.example.demo.domain.tweet.request.CreateTweetRequest;
import com.example.demo.domain.tweet.response.TweetResponse;
import com.example.demo.domain.tweet.response.TweetListResponse;
import com.example.demo.domain.tweet.service.TweetService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * TweetController 간단한 단위 테스트
 * 
 * 5년차 시니어 접근법:
 * - ApplicationContext 로딩 없이 Controller 로직만 테스트
 * - MockMvc 대신 직접 메서드 호출로 단순화
 * - 핵심 비즈니스 로직에 집중
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TweetController 간단한 단위 테스트")
class TweetControllerSimpleTest {

    @Mock
    private TweetService tweetService;

    @InjectMocks
    private TweetController tweetController;

    private UUID userId;
    private CreateTweetRequest createRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        createRequest = new CreateTweetRequest();
        createRequest.setContent("테스트 트윗 내용입니다!");
    }

    @Test
    @DisplayName("POST /tweets - 트윗 생성 성공")
    void createTweet_Success() {
        // Given
        TweetResponse mockResponse = TweetResponse.builder()
                .tweetId(UUID.randomUUID())
                .userId(userId)
                .content(createRequest.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        given(tweetService.createTweet(eq(userId), any(CreateTweetRequest.class)))
                .willReturn(mockResponse);

        // When
        ApiResponse<TweetResponse> response = tweetController.createTweet(userId, createRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("트윗이 성공적으로 생성되었습니다");
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getContent()).isEqualTo(createRequest.getContent());

        verify(tweetService).createTweet(userId, createRequest);
    }

    @Test
    @DisplayName("GET /tweets/{userId} - 사용자 트윗 조회 성공")
    void getUserTweets_Success() {
        // Given
        TweetResponse tweet1 = TweetResponse.builder()
                .tweetId(UUID.randomUUID())
                .userId(userId)
                .content("첫 번째 트윗")
                .createdAt(LocalDateTime.now())
                .build();
        
        TweetResponse tweet2 = TweetResponse.builder()
                .tweetId(UUID.randomUUID())
                .userId(userId)
                .content("두 번째 트윗")
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        TweetListResponse mockResponse = new TweetListResponse();
        mockResponse.setTweets(Arrays.asList(tweet1, tweet2));
        mockResponse.setHasMore(false);
        mockResponse.setNextCursor(null);

        given(tweetService.getUserTweets(eq(userId), isNull(), eq(10)))
                .willReturn(mockResponse);

        // When
        ApiResponse<TweetListResponse> response = tweetController.getUserTweets(userId, null, 10);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("사용자 트윗 조회가 완료되었습니다");
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getTweets()).hasSize(2);
        assertThat(response.getData().isHasMore()).isFalse();

        verify(tweetService).getUserTweets(userId, null, 10);
    }

    @Test
    @DisplayName("GET /tweets/{userId} - 커서와 함께 조회 성공")
    void getUserTweets_WithCursor_Success() {
        // Given
        LocalDateTime cursor = LocalDateTime.of(2025, 1, 19, 10, 0, 0);
        
        TweetResponse tweet = TweetResponse.builder()
                .tweetId(UUID.randomUUID())
                .userId(userId)
                .content("과거 트윗")
                .createdAt(cursor.minusHours(1))
                .build();

        TweetListResponse mockResponse = new TweetListResponse();
        mockResponse.setTweets(Arrays.asList(tweet));
        mockResponse.setHasMore(true);
        mockResponse.setNextCursor(cursor.minusHours(2));

        given(tweetService.getUserTweets(eq(userId), eq(cursor), eq(5)))
                .willReturn(mockResponse);

        // When
        ApiResponse<TweetListResponse> response = tweetController.getUserTweets(userId, cursor, 5);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("사용자 트윗 조회가 완료되었습니다");
        assertThat(response.getData().getTweets()).hasSize(1);
        assertThat(response.getData().isHasMore()).isTrue();

        verify(tweetService).getUserTweets(userId, cursor, 5);
    }

    @Test
    @DisplayName("GET /tweets/{userId} - 트윗이 없는 경우")
    void getUserTweets_NoTweets_Success() {
        // Given
        TweetListResponse mockResponse = new TweetListResponse();
        mockResponse.setTweets(Collections.emptyList());
        mockResponse.setHasMore(false);
        mockResponse.setNextCursor(null);

        given(tweetService.getUserTweets(eq(userId), isNull(), eq(10)))
                .willReturn(mockResponse);

        // When
        ApiResponse<TweetListResponse> response = tweetController.getUserTweets(userId, null, 10);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("사용자 트윗 조회가 완료되었습니다");
        assertThat(response.getData().getTweets()).isEmpty();
        assertThat(response.getData().isHasMore()).isFalse();

        verify(tweetService).getUserTweets(userId, null, 10);
    }

    @Test
    @DisplayName("Controller 메서드 직접 호출 - 예외 처리 확인")
    void createTweet_ServiceThrowsException_HandledGracefully() {
        // Given
        given(tweetService.createTweet(eq(userId), any(CreateTweetRequest.class)))
                .willThrow(new RuntimeException("Service layer error"));

        // When & Then
        assertThatThrownBy(() -> tweetController.createTweet(userId, createRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Service layer error");

        verify(tweetService).createTweet(userId, createRequest);
    }
} 