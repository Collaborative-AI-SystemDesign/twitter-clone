package com.example.demo.config;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.cql.WriteOptions;

/**
 * 카산드라 성능 최적화 설정
 * 
 * 목적:
 * - 타임라인 전용 WriteOptions 최적화
 * - 배치 처리 성능 개선
 * - ConsistencyLevel ONE 적용
 */
@Configuration
public class CassandraOptimizationConfig {

    /**
     * 타임라인 전용 WriteOptions (성능 최적화)
     * - ConsistencyLevel.ONE: 빠른 쓰기 성능
     * - 타임라인은 Eventually Consistent 허용
     */
    @Bean("timelineWriteOptions")
    public WriteOptions timelineWriteOptions() {
        return WriteOptions.builder()
                .consistencyLevel(ConsistencyLevel.ONE)
                .build();
    }

    /**
     * 고성능 배치 WriteOptions
     */
    @Bean("batchWriteOptions")
    public WriteOptions batchWriteOptions() {
        return WriteOptions.builder()
                .consistencyLevel(ConsistencyLevel.ONE)
                .build();
    }
} 