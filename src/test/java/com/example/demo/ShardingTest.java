package com.example.demo;

import com.example.demo.config.DataSourceConfig;
import com.example.demo.util.ShardUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ShardingTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void testShardKeySelection() {
        // 사용자 데이터는 항상 shard0에 저장되어야 함
        String userShardKey = ShardUtil.selectUserDataShardKey();
        assertEquals("shard0", userShardKey);

        // 트윗 데이터는 사용자 ID 기반으로 분산되어야 함
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();

        String tweetShardKey1 = ShardUtil.selectTweetDataShardKeyByUserId(userId1);
        String tweetShardKey2 = ShardUtil.selectTweetDataShardKeyByUserId(userId2);
        String tweetShardKey3 = ShardUtil.selectTweetDataShardKeyByUserId(userId3);

        // shard1, shard2, shard3 중 하나여야 함
        assertTrue(tweetShardKey1.matches("shard[1-3]"));
        assertTrue(tweetShardKey2.matches("shard[1-3]"));
        assertTrue(tweetShardKey3.matches("shard[1-3]"));

        System.out.println("User ID 1: " + userId1 + " -> Shard: " + tweetShardKey1);
        System.out.println("User ID 2: " + userId2 + " -> Shard: " + tweetShardKey2);
        System.out.println("User ID 3: " + userId3 + " -> Shard: " + tweetShardKey3);
    }

    @Test
    public void testDataSourceRouting() {
        // shard0 테스트
        DataSourceConfig.setShard("shard0");
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            String result = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            System.out.println("Shard0 Database: " + result);
            assertNotNull(result);
        } finally {
            DataSourceConfig.clearShard();
        }

        // shard1 테스트
        DataSourceConfig.setShard("shard1");
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            String result = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            System.out.println("Shard1 Database: " + result);
            assertNotNull(result);
        } finally {
            DataSourceConfig.clearShard();
        }

        // shard2 테스트
        DataSourceConfig.setShard("shard2");
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            String result = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            System.out.println("Shard2 Database: " + result);
            assertNotNull(result);
        } finally {
            DataSourceConfig.clearShard();
        }

        // shard3 테스트
        DataSourceConfig.setShard("shard3");
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            String result = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            System.out.println("Shard3 Database: " + result);
            assertNotNull(result);
        } finally {
            DataSourceConfig.clearShard();
        }
    }

    @Test
    public void testShardDistribution() {
        // 여러 사용자 ID로 샤드 분산 테스트
        int shard1Count = 0, shard2Count = 0, shard3Count = 0;
        int totalTests = 1000;

        for (int i = 0; i < totalTests; i++) {
            UUID userId = UUID.randomUUID();
            String shardKey = ShardUtil.selectTweetDataShardKeyByUserId(userId);
            
            switch (shardKey) {
                case "shard1":
                    shard1Count++;
                    break;
                case "shard2":
                    shard2Count++;
                    break;
                case "shard3":
                    shard3Count++;
                    break;
                default:
                    fail("Unexpected shard key: " + shardKey);
            }
        }

        System.out.println("Shard Distribution Test Results:");
        System.out.println("Shard1: " + shard1Count + " (" + (shard1Count * 100.0 / totalTests) + "%)");
        System.out.println("Shard2: " + shard2Count + " (" + (shard2Count * 100.0 / totalTests) + "%)");
        System.out.println("Shard3: " + shard3Count + " (" + (shard3Count * 100.0 / totalTests) + "%)");

        // 분산이 대략적으로 균등한지 확인 (각 샤드가 20% 이상의 데이터를 받아야 함)
        assertTrue(shard1Count > totalTests * 0.2, "Shard1 distribution too low");
        assertTrue(shard2Count > totalTests * 0.2, "Shard2 distribution too low");
        assertTrue(shard3Count > totalTests * 0.2, "Shard3 distribution too low");
    }

    @Test
    @Transactional
    public void testTransactionalSharding() {
        // 트랜잭션 내에서 샤딩이 제대로 작동하는지 테스트
        UUID userId = UUID.randomUUID();
        String shardKey = ShardUtil.selectTweetDataShardKeyByUserId(userId);
        
        DataSourceConfig.setShard(shardKey);
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            
            // 테스트 테이블 생성 (실제 환경에서는 이미 존재할 것)
            try {
                jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS shard_test (id VARCHAR(255) PRIMARY KEY, shard_key VARCHAR(10))");
            } catch (Exception e) {
                // 테이블이 이미 존재하거나 권한 문제일 수 있음
                System.out.println("Could not create test table: " + e.getMessage());
            }

            // 데이터 삽입 테스트
            String testId = UUID.randomUUID().toString();
            jdbcTemplate.update("INSERT INTO shard_test (id, shard_key) VALUES (?, ?)", testId, shardKey);
            
            // 데이터 조회 테스트
            String result = jdbcTemplate.queryForObject(
                "SELECT shard_key FROM shard_test WHERE id = ?", 
                String.class, 
                testId
            );
            
            assertEquals(shardKey, result);
            System.out.println("Transactional sharding test passed for shard: " + shardKey);
            
        } finally {
            DataSourceConfig.clearShard();
        }
    }
} 