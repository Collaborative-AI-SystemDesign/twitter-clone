package com.example.demo.domain2.tweet_r.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RDB 버전 트윗 생성 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTweetRequest {

    @NotBlank(message = "트윗 내용은 필수입니다")
    @Size(max = 280, message = "트윗은 280자를 초과할 수 없습니다")
    private String content;
} 