package com.example.demo.util.cassandra;

import com.example.demo.domain.follow.FollowRepository;
import com.example.demo.domain.follow.FollowersByUser;
import com.example.demo.domain.follow.FollowersByUserKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 카산드라 팔로우 관계 테스트 데이터 생성기
 * 
 * 목적: 대용량 팔로우 관계 데이터를 효율적으로 생성
 * 
 * 데이터 구조:
 * - 총 1,000,001명의 유저 (user_1 ~ user_1000001)
 * - 7명의 Celebrity 유저가 각각 다른 수의 팔로워 보유:
 *   user_10: 1명, user_20: 10명, user_30: 100명
 *   user_40: 1,000명, user_50: 10,000명
 *   user_60: 100,000명, user_70: 1,000,000명
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CassandraDataGenerator {

    private final FollowRepository followRepository;

    /**
     * Celebrity 유저들의 팔로우 관계 생성
     * 
     * @param totalUserCount 총 유저 수 (1,000,001 권장)
     */
    public void generateFollowRelations(int totalUserCount) {
        log.info("=== 카산드라 팔로우 관계 생성 시작 ===");
        log.info("총 유저 수: {}", totalUserCount);
        
        LocalDateTime now = LocalDateTime.now();
        
        // Celebrity 유저와 팔로워 수 정의 (User 60까지 테스트)
        int[][] celebrityFollowers = {
            {10, 1},        // user_10: 1명
            {20, 10},       // user_20: 10명  
            {30, 100},      // user_30: 100명
            {40, 1000},     // user_40: 1,000명
            {50, 10000},    // user_50: 10,000명
            {60, 100000}    // user_60: 100,000명 🚀
        };
        
        for (int[] celebrity : celebrityFollowers) {
            int celebrityNumber = celebrity[0];
            int followerCount = celebrity[1];
            
            UUID celebrityId = generateUserId(celebrityNumber);
            log.info("Celebrity user_{} ({}): {}명의 팔로워 생성 중...", 
                celebrityNumber, celebrityId, followerCount);
            
            createFollowersForCelebrity(celebrityId, celebrityNumber, followerCount, totalUserCount, now);
            
            log.info("Celebrity user_{} 팔로워 생성 완료", celebrityNumber);
        }
        
        log.info("=== 카산드라 팔로우 관계 생성 완료 ===");
    }
    
    /**
     * 특정 Celebrity에 대한 팔로워 생성
     */
    private void createFollowersForCelebrity(UUID celebrityId, int celebrityNumber, 
                                           int followerCount, int totalUserCount, LocalDateTime now) {
        
        List<FollowersByUser> followers = new ArrayList<>();
        int startFollower = getFollowerStartNumber(celebrityNumber);
        int batchSize = 1000; // 배치 크기
        
        for (int i = 0; i < followerCount; i++) {
            int followerNumber = startFollower + i;
            
            // 유효한 범위 내의 팔로워만 생성
            if (followerNumber > totalUserCount) {
                log.warn("팔로워 번호가 총 유저 수를 초과: {}", followerNumber);
                break;
            }
            
            UUID followerId = generateUserId(followerNumber);
            
            FollowersByUser follower = new FollowersByUser(celebrityId, followerId, now);
                    
            followers.add(follower);
            
            // 배치 단위로 저장
            if (followers.size() >= batchSize) {
                followRepository.saveAll(followers);
                followers.clear();
                
                if (i % 10000 == 0) {
                    log.info("진행률: {}/{}명 ({:.1f}%)", 
                        i, followerCount, (double)i/followerCount*100);
                }
            }
        }
        
        // 남은 데이터 저장
        if (!followers.isEmpty()) {
            followRepository.saveAll(followers);
        }
    }
    
    /**
     * 각 Celebrity의 팔로워 시작 번호 계산 (overlapping 방식으로 수정)
     */
    private int getFollowerStartNumber(int celebrityNumber) {
        // 모든 Celebrity가 동일한 풀에서 팔로워를 가져가도록 수정 (overlap 허용)
        switch (celebrityNumber) {
            case 10: return 1;          // user_1
            case 20: return 1;          // user_1 ~ user_10 (중복 허용)
            case 30: return 1;          // user_1 ~ user_100 (중복 허용)
            case 40: return 1;          // user_1 ~ user_1000 (중복 허용)
            case 50: return 1;          // user_1 ~ user_10000 (중복 허용)
            case 60: return 1;          // user_1 ~ user_100000 (중복 허용)
            case 70: return 1;          // user_1 ~ user_1000000 (중복 허용)
            default: return 1;
        }
    }
    
    /**
     * 유저 번호로부터 UUID 생성 (일관된 UUID 생성)
     */
    private UUID generateUserId(int userNumber) {
        // 일관된 UUID 생성을 위해 고정된 패턴 사용
        String uuidString = String.format("550e8400-e29b-41d4-a716-%012d", userNumber);
        return UUID.fromString(uuidString);
    }
    
    /**
     * 모든 팔로우 관계 삭제
     */
    public void clearAllFollowData() {
        log.info("=== 카산드라 팔로우 데이터 정리 시작 ===");
        followRepository.deleteAll();
        log.info("=== 카산드라 팔로우 데이터 정리 완료 ===");
    }
    
    /**
     * 팔로우 관계 통계 조회
     */
    public void logFollowStatistics() {
        log.info("=== 카산드라 팔로우 데이터 통계 ===");
        
        // Celebrity별 팔로워 수 확인
        int[] celebrityNumbers = {10, 20, 30, 40, 50, 60};
        
        for (int celebrityNumber : celebrityNumbers) {
            UUID celebrityId = generateUserId(celebrityNumber);
            List<UUID> followerIds = followRepository.findByKeyFollowedUserId(celebrityId)
                    .stream()
                    .map(follower -> follower.getKey().getFollowerId())
                    .toList();
            int followerCount = followerIds.size();
            log.info("user_{} ({}): {}명의 팔로워", celebrityNumber, celebrityId, followerCount);
        }
        
        log.info("=== 통계 조회 완료 ===");
    }
} 