package com.example.demo.domain.tweet;

import com.example.demo.domain.tweet.dto.FanoutRetryMessage;
import com.example.demo.domain.tweet.service.TweetService;
import com.example.demo.rabbitmq.RabbitMqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Fan-out 재시도 큐 처리기
 * 
 * RabbitMQ에서 재시도 메시지를 받아 Fan-out을 다시 시도
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FanoutRetryProcessor {

    private final TweetService tweetService;
    private final RabbitMqService rabbitMqService;
    
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * Fan-out 재시도 메시지 처리
     * 
     * 큐에서 재시도 메시지를 받아 Fan-out을 다시 실행
     */
    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void processFanoutRetry(FanoutRetryMessage message) {
        try {
            log.info("Fan-out 재시도 처리 시작 - authorId: {}, tweetId: {}, retryCount: {}", 
                    message.getAuthorId(), message.getTweetId(), message.getRetryCount());
            
            // Fan-out 재시도 실행
            tweetService.retryFanout(message);
            
            log.info("Fan-out 재시도 성공 - authorId: {}, tweetId: {}", 
                    message.getAuthorId(), message.getTweetId());
                    
        } catch (Exception e) {
            handleRetryFailure(message, e);
        }
    }

    /**
     * 재시도 실패 처리
     */
    private void handleRetryFailure(FanoutRetryMessage message, Exception e) {
        int nextRetryCount = message.getRetryCount() + 1;
        
        if (nextRetryCount <= MAX_RETRY_COUNT) {
            // 재시도 카운트 증가 후 다시 큐에 전송
            log.warn("Fan-out 재시도 실패, 재전송 - authorId: {}, retryCount: {}", 
                    message.getAuthorId(), nextRetryCount);
            
            FanoutRetryMessage retryMessage = new FanoutRetryMessage(
                message.getAuthorId(),
                message.getTweetId(), 
                message.getTweetText(),
                message.getCreatedAt(),
                nextRetryCount
            );
            
            // 다시 큐에 전송
            rabbitMqService.sendMessage(retryMessage);
            
        } else {
            // 최대 재시도 초과 - Dead Letter Queue 처리
            log.error("Fan-out 최대 재시도 초과 - authorId: {}, tweetId: {}, maxRetry: {}", 
                    message.getAuthorId(), message.getTweetId(), MAX_RETRY_COUNT, e);
            
            // TODO: 모니터링 알림 발송, Dead Letter Queue 저장 등
            // alertService.sendAlert("Fan-out 실패", message);
        }
    }
} 