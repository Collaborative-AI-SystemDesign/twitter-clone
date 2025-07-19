package com.example.demo.domain.tweet.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 트윗 생성 요청 DTO
 * 
 * API 명세 대응: POST /tweets
 * 요청 본문 예시:
 * {
 *   "content": "더워 죽겠네"
 * }
 */
@Getter
@Setter
@NoArgsConstructor
public class CreateTweetRequest {
    
    /**
     * 트윗 내용
     * - 필수 입력
     * - 최대 280자 제한 (Twitter 규격)
     */
    @NotBlank(message = "트윗 내용은 필수입니다")
    @Size(max = 280, message = "트윗은 최대 280자까지 입력 가능합니다")
    private String content;
} 