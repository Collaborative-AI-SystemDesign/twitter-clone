package com.example.demo.ratelimiter.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정 클래스
 * 
 * RedisTemplate Bean을 정의하여 Redis 기반 Rate Limiter들이 
 * 의존성 주입을 받을 수 있도록 설정
 * 
 * Redis Cluster와 단일 Redis 모두 지원:
 * - application.yaml에서 spring.data.redis.cluster.nodes 설정 시 Cluster 모드
 * - application.yaml에서 spring.data.redis.host/port 설정 시 단일 모드
 */
@Slf4j
@Configuration
public class RedisConfig {
    
    /**
     * RedisTemplate<String, String> Bean 등록
     * 
     * Rate Limiter들이 사용할 RedisTemplate을 설정합니다.
     * Key와 Value 모두 String으로 직렬화하여 Redis에 저장합니다.
     * 
     * Spring Boot가 application.yaml 설정에 따라 자동으로 생성하는 연결 팩토리를 사용:
     * - Redis Cluster: RedisClusterConnectionFactory
     * - 단일 Redis: LettuceConnectionFactory
     * 
     * @param connectionFactory Spring Boot가 자동으로 생성하는 Redis 연결 팩토리
     * @return 설정된 RedisTemplate 인스턴스
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        
        // Redis 연결 팩토리 설정
        template.setConnectionFactory(connectionFactory);
        
        // Key 직렬화 설정 (String으로 저장)
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Value 직렬화 설정 (String으로 저장)
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        
        // 설정 적용
        template.afterPropertiesSet();
        
        // Redis 연결 정보 로깅
        log.info("Redis Template configured with connection factory: {}", 
                connectionFactory.getClass().getSimpleName());
        
        return template;
    }
}
