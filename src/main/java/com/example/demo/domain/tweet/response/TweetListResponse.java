package com.example.demo.domain.tweet.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 트윗 목록 응답 DTO
 * 
 * API 명세 대응: GET /tweets/{userId}?last={timestamp}&size={size}
 * 응답 예시:
 * {
 *   "tweets": [
 *     {
 *       "tweetId": 12345,
 *       "userId": 20,
 *       "content": "더워 죽겠네",
 *       "createdAt": "2025-07-10T20:14:30"
 *     }
 *   ],
 *   "nextCursor": "2025-07-10T20:14:30",
 *   "hasMore": true
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TweetListResponse {
    
    /**
     * 트윗 목록
     */
    private List<TweetResponse> tweets;
    
    /**
     * 다음 페이지 커서 (마지막 트윗의 생성 시간)
     */
    private LocalDateTime nextCursor;
    
    /**
     * 더 많은 데이터 존재 여부
     */
    private boolean hasMore;
} 