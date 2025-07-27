package com.example.demo.domain.test;

import com.example.demo.config.DataSourceConfig;
import com.example.demo.util.ShardUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.*;

@RestController
@RequestMapping("/api/sharding-test")
public class ShardingTestController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/shard-key/{userId}")
    public Map<String, Object> getShardKey(@PathVariable String userId) {
        UUID userUuid = UUID.fromString(userId);
        
        String userShardKey = ShardUtil.selectUserDataShardKey();
        String tweetShardKey = ShardUtil.selectTweetDataShardKeyByUserId(userUuid);
        
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("userShardKey", userShardKey);
        result.put("tweetShardKey", tweetShardKey);
        
        return result;
    }

    @GetMapping("/test-connection/{shardKey}")
    public Map<String, Object> testConnection(@PathVariable String shardKey) {
        Map<String, Object> result = new HashMap<>();
        
        DataSourceConfig.setShard(shardKey);
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            
            // 데이터베이스 정보 조회
            String database = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
            
            result.put("shardKey", shardKey);
            result.put("database", database);
            result.put("version", version);
            result.put("status", "success");
            
        } catch (Exception e) {
            result.put("shardKey", shardKey);
            result.put("status", "error");
            result.put("error", e.getMessage());
        } finally {
            DataSourceConfig.clearShard();
        }
        
        return result;
    }

    @GetMapping("/test-all-shards")
    public Map<String, Object> testAllShards() {
        Map<String, Object> result = new HashMap<>();
        List<String> shardKeys = Arrays.asList("shard0", "shard1", "shard2", "shard3");
        
        for (String shardKey : shardKeys) {
            result.put(shardKey, testConnection(shardKey));
        }
        
        return result;
    }

    @PostMapping("/test-distribution")
    public Map<String, Object> testDistribution(@RequestParam(defaultValue = "100") int count) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Integer> distribution = new HashMap<>();
        
        distribution.put("shard1", 0);
        distribution.put("shard2", 0);
        distribution.put("shard3", 0);
        
        List<Map<String, String>> details = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            UUID userId = UUID.randomUUID();
            String shardKey = ShardUtil.selectTweetDataShardKeyByUserId(userId);
            
            distribution.put(shardKey, distribution.get(shardKey) + 1);
            
            Map<String, String> detail = new HashMap<>();
            detail.put("userId", userId.toString());
            detail.put("shardKey", shardKey);
            details.add(detail);
        }
        
        result.put("totalCount", count);
        result.put("distribution", distribution);
        result.put("details", details);
        
        // 분산률 계산
        Map<String, Double> percentages = new HashMap<>();
        for (String shardKey : distribution.keySet()) {
            double percentage = (distribution.get(shardKey) * 100.0) / count;
            percentages.put(shardKey, percentage);
        }
        result.put("percentages", percentages);
        
        return result;
    }

    @PostMapping("/test-transaction/{shardKey}")
    public Map<String, Object> testTransaction(@PathVariable String shardKey) {
        Map<String, Object> result = new HashMap<>();
        
        DataSourceConfig.setShard(shardKey);
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            
            // 테스트 테이블 생성
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS shard_test (id VARCHAR(255) PRIMARY KEY, shard_key VARCHAR(10), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            
            // 데이터 삽입
            String testId = UUID.randomUUID().toString();
            jdbcTemplate.update("INSERT INTO shard_test (id, shard_key) VALUES (?, ?)", testId, shardKey);
            
            // 데이터 조회
            String retrievedShardKey = jdbcTemplate.queryForObject(
                "SELECT shard_key FROM shard_test WHERE id = ?", 
                String.class, 
                testId
            );
            
            result.put("shardKey", shardKey);
            result.put("testId", testId);
            result.put("retrievedShardKey", retrievedShardKey);
            result.put("status", "success");
            result.put("message", "Transaction test completed successfully");
            
        } catch (Exception e) {
            result.put("shardKey", shardKey);
            result.put("status", "error");
            result.put("error", e.getMessage());
        } finally {
            DataSourceConfig.clearShard();
        }
        
        return result;
    }

    @PostMapping("/test-tweet-creation")
    public Map<String, Object> testTweetCreation(@RequestParam(defaultValue = "10") int count) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Integer> distribution = new HashMap<>();
        Map<String, List<String>> tweetIds = new HashMap<>();
        
        distribution.put("shard1", 0);
        distribution.put("shard2", 0);
        distribution.put("shard3", 0);
        
        tweetIds.put("shard1", new ArrayList<>());
        tweetIds.put("shard2", new ArrayList<>());
        tweetIds.put("shard3", new ArrayList<>());
        
        List<Map<String, Object>> details = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            UUID userId = UUID.randomUUID();
            String expectedShardKey = ShardUtil.selectTweetDataShardKeyByUserId(userId);
            
            // 실제 트윗 생성 API 호출
            try {
                String tweetContent = "테스트 트윗 #" + (i + 1) + " - 사용자: " + userId;
                
                // HTTP 클라이언트로 실제 API 호출
                String response = createTweetViaApi(userId, tweetContent);
                
                distribution.put(expectedShardKey, distribution.get(expectedShardKey) + 1);
                tweetIds.get(expectedShardKey).add("tweet-" + (i + 1));
                
                Map<String, Object> detail = new HashMap<>();
                detail.put("userId", userId.toString());
                detail.put("expectedShardKey", expectedShardKey);
                detail.put("tweetContent", tweetContent);
                detail.put("response", response);
                details.add(detail);
                
            } catch (Exception e) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("userId", userId.toString());
                detail.put("expectedShardKey", expectedShardKey);
                detail.put("error", e.getMessage());
                details.add(detail);
            }
        }
        
        result.put("totalCount", count);
        result.put("distribution", distribution);
        result.put("tweetIds", tweetIds);
        result.put("details", details);
        
        // 분산률 계산
        Map<String, Double> percentages = new HashMap<>();
        for (String shardKey : distribution.keySet()) {
            double percentage = (distribution.get(shardKey) * 100.0) / count;
            percentages.put(shardKey, percentage);
        }
        result.put("percentages", percentages);
        
        return result;
    }
    
    private String createTweetViaApi(UUID userId, String content) {
        // 간단한 HTTP 클라이언트 구현
        try {
            String jsonBody = "{\"content\":\"" + content + "\"}";
            
            ProcessBuilder pb = new ProcessBuilder(
                "curl", "-X", "POST", 
                "http://localhost:8080/tweets-rdb",
                "-H", "Content-Type: application/json",
                "-H", "Tweet-User-Id: " + userId.toString(),
                "-d", jsonBody
            );
            
            Process process = pb.start();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            
            process.waitFor();
            return response.toString();
            
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/create-tables")
    public Map<String, Object> createTables() {
        Map<String, Object> result = new HashMap<>();
        List<String> shardKeys = Arrays.asList("shard0", "shard1", "shard2", "shard3");
        
        for (String shardKey : shardKeys) {
            Map<String, Object> shardResult = new HashMap<>();
            
            DataSourceConfig.setShard(shardKey);
            try {
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                
                // tweets 테이블 생성
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS tweets (
                        tweet_id BINARY(16) PRIMARY KEY,
                        user_id BINARY(16) NOT NULL,
                        tweet_text VARCHAR(280) NOT NULL,
                        created_at DATETIME NOT NULL,
                        modified_at DATETIME,
                        INDEX idx_tweet_user_created (user_id, created_at DESC),
                        INDEX idx_tweet_created (created_at DESC)
                    )
                """);
                
                // tweets_by_user 테이블 생성
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS tweets_by_user (
                        user_id BINARY(16) NOT NULL,
                        tweet_id BINARY(16) NOT NULL,
                        tweet_created_at DATETIME NOT NULL,
                        tweet_text VARCHAR(280) NOT NULL,
                        PRIMARY KEY (user_id, tweet_id, tweet_created_at),
                        INDEX idx_tweet_by_user_created (user_id, tweet_created_at DESC)
                    )
                """);
                
                // user_timeline 테이블 생성
                jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS user_timeline (
                        follower_id BINARY(16) NOT NULL,
                        tweet_id BINARY(16) NOT NULL,
                        author_id BINARY(16) NOT NULL,
                        tweet_text VARCHAR(280) NOT NULL,
                        created_at DATETIME NOT NULL,
                        PRIMARY KEY (follower_id, tweet_id, created_at),
                        INDEX idx_timeline_created (follower_id, created_at DESC)
                    )
                """);
                
                // follows 테이블 생성 (shard0에만)
                if ("shard0".equals(shardKey)) {
                    jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS follows (
                            follower_id BINARY(16) NOT NULL,
                            following_id BINARY(16) NOT NULL,
                            created_at DATETIME NOT NULL,
                            PRIMARY KEY (follower_id, following_id),
                            INDEX idx_following_created (following_id, created_at DESC)
                        )
                    """);
                }
                
                shardResult.put("status", "success");
                shardResult.put("message", "Tables created successfully");
                
            } catch (Exception e) {
                shardResult.put("status", "error");
                shardResult.put("error", e.getMessage());
            } finally {
                DataSourceConfig.clearShard();
            }
            
            result.put(shardKey, shardResult);
        }
        
        return result;
    }
} 