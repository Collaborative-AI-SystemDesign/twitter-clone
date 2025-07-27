package com.example.demo.domain2.follow_r.entity;

import com.example.demo.domain.MySqlBaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RDB 버전 팔로우 엔티티
 * 사용자 간의 팔로우 관계를 저장
 */
@Entity
@Table(name = "follows", indexes = {
    @Index(name = "idx_follow_user_id", columnList = "user_id"),
    @Index(name = "idx_follow_follower_id", columnList = "follower_id"),
    @Index(name = "idx_follow_user_follower", columnList = "user_id, follower_id", unique = true)
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Follow extends MySqlBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 팔로우 당하는 사용자 ID (게시물 작성자)
     */
    @Column(name = "user_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID userId;

    /**
     * 팔로우 하는 사용자 ID (팔로워)
     */
    @Column(name = "follower_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID followerId;


} 