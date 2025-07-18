package com.example.demo.domain;

import lombok.Getter;
import org.springframework.data.cassandra.core.mapping.Column;

import java.time.LocalDateTime;

@Getter
public abstract class CassandraBaseEntity {

    @Column("created_at")
    protected LocalDateTime createdAt;

    /**
     * 생성 시간 설정
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
