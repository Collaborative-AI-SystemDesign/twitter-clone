package com.example.demo.domain;

import lombok.Getter;
import org.springframework.data.annotation.Transient;

import java.time.LocalDateTime;

@Getter
public abstract class CassandraBaseEntity {

    /**
     * 생성 시간
     * - 컴포지트 키에서 이미 created_at을 정의하므로 Cassandra 매핑에서 제외
     * - 도메인 로직에서는 키에서 가져온 값을 사용
     */
    @Transient
    protected LocalDateTime createdAt;

    /**
     * 생성 시간 설정
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
