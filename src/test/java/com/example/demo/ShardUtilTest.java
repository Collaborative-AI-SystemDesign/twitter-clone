package com.example.demo;

import com.example.demo.util.ShardUtil;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShardUtilTest {

    @Test
    public void testHashDistribution() {
        System.out.println("=== UUID 해시값 분배 테스트 ===");
        
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("shard1", 0);
        distribution.put("shard2", 0);
        distribution.put("shard3", 0);
        
        // 테스트한 UUID들의 해시값 확인
        String[] testUuids = {
            "550e8400-e29b-41d4-a716-446655440000",
            "11111111-1111-1111-1111-111111111111",
            "22222222-2222-2222-2222-222222222222",
            "33333333-3333-3333-3333-333333333333",
            "44444444-4444-4444-4444-444444444444",
            "55555555-5555-5555-5555-555555555555",
            "66666666-6666-6666-6666-666666666666",
            "77777777-7777-7777-7777-777777777777",
            "88888888-8888-8888-8888-888888888888",
            "99999999-9999-9999-9999-999999999999",
            "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
            "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
            "cccccccc-cccc-cccc-cccc-cccccccccccc",
            "dddddddd-dddd-dddd-dddd-dddddddddddd",
            "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee",
            "ffffffff-ffff-ffff-ffff-ffffffffffff"
        };
        
        for (String uuidStr : testUuids) {
            UUID userId = UUID.fromString(uuidStr);
            String shardKey = ShardUtil.selectTweetDataShardKeyByUserId(userId);
            
            // 안전한 방식으로 카운트 증가
            distribution.put(shardKey, distribution.getOrDefault(shardKey, 0) + 1);
            
            // 새로운 해시 계산 방식 확인
            long mostSigBits = userId.getMostSignificantBits();
            long leastSigBits = userId.getLeastSignificantBits();
            long combinedHash = mostSigBits ^ leastSigBits;
            int safeHash = (int) Math.abs(combinedHash);
            int shardNum = (safeHash % 3) + 1;
            
            System.out.printf("UUID: %s, MostSig: %d, LeastSig: %d, CombinedHash: %d, SafeHash: %d, ShardNum: %d, ShardKey: %s%n", 
                uuidStr, mostSigBits, leastSigBits, combinedHash, safeHash, shardNum, shardKey);
        }
        
        System.out.println("\n=== 분배 결과 ===");
        for (String shardKey : distribution.keySet()) {
            System.out.printf("%s: %d개%n", shardKey, distribution.get(shardKey));
        }
        
        // 랜덤 UUID로 테스트
        System.out.println("\n=== 랜덤 UUID 100개 테스트 ===");
        Map<String, Integer> randomDistribution = new HashMap<>();
        randomDistribution.put("shard1", 0);
        randomDistribution.put("shard2", 0);
        randomDistribution.put("shard3", 0);
        
        for (int i = 0; i < 100; i++) {
            UUID randomUserId = UUID.randomUUID();
            String shardKey = ShardUtil.selectTweetDataShardKeyByUserId(randomUserId);
            randomDistribution.put(shardKey, randomDistribution.getOrDefault(shardKey, 0) + 1);
        }
        
        for (String shardKey : randomDistribution.keySet()) {
            System.out.printf("%s: %d개 (%.1f%%)%n", 
                shardKey, randomDistribution.get(shardKey), 
                (randomDistribution.get(shardKey) * 100.0) / 100);
        }
    }
} 