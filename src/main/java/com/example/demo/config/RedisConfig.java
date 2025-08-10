package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Redis 설정 클래스
 * 
 * 핵심 기능:
 * - RedisTemplate 설정 (JSON 직렬화)
 * - ZSetOperations Bean 제공 (타임라인용 SortedSet)
 * - 타임라인 캐싱을 위한 Redis 연동
 */
@Configuration
public class RedisConfig {

    /**
     * LocalDateTime 지원 ObjectMapper
     * 
     * Java 8 시간 타입(LocalDateTime) 직렬화를 위한 설정
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * RedisTemplate 설정
     * 
     * 설정 내용:
     * - Key: String 직렬화
     * - Value: JSON 직렬화 (LocalDateTime 지원)
     * - Hash Key/Value: String/JSON 직렬화
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory, ObjectMapper redisObjectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Key 직렬화: String
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Value 직렬화: JSON (LocalDateTime 지원)
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * ZSetOperations Bean
     * 
     * 타임라인용 Redis SortedSet 연산을 위한 Bean
     * Score: 트윗 생성 시간 (timestamp)
     * Value: 트윗 정보 (JSON)
     */
    @Bean
    public ZSetOperations<String, Object> zSetOperations(RedisTemplate<String, Object> redisTemplate) {
        return redisTemplate.opsForZSet();
    }
}