package com.example.demo.domain.tweet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fan-out 재시도를 위한 메시지
 * 
 * RabbitMQ를 통해 전송되는 재시도 정보
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FanoutRetryMessage {
    
    /**
     * 트윗 작성자 ID
     */
    private UUID authorId;
    
    /**
     * 트윗 ID
     */
    private UUID tweetId;
    
    /**
     * 트윗 내용
     */
    private String tweetText;
    
    /**
     * 트윗 생성 시간 (원본 시간, 중복 방지용)
     */
    private LocalDateTime createdAt;
    
    /**
     * 재시도 횟수
     */
    private int retryCount;
} 