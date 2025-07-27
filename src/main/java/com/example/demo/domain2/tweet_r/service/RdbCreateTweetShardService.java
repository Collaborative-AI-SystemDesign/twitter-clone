package com.example.demo.domain2.tweet_r.service;

import com.example.demo.config.DataSourceConfig;
import com.example.demo.domain2.tweet_r.entity.Tweet;
import com.example.demo.domain2.tweet_r.repository.RdbTweetRepository;
import com.example.demo.domain2.tweet_r.request.CreateTweetRequest;
import com.example.demo.domain2.tweet_r.response.TweetResponse;
import com.example.demo.domain2.follow_r.repository.RdbFollowRepository;
import com.example.demo.util.ShardUtil;
import com.example.demo.util.UUID.UUIDUtil;
import jakarta.transaction.Transactional;
import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class RdbCreateTweetShardService {

  private final RdbFollowRepository followRepository;
  private final RdbTweetRepository rdbTweetRepository;
  private final PlatformTransactionManager transactionManager;
  private final JdbcTemplate jdbcTemplate;

  public TweetResponse createTweetWithCorrectSharding(UUID userId, CreateTweetRequest request) {
    System.out.println("📌 JdbcTemplate.getDataSource(): " + jdbcTemplate.getDataSource().getClass());

    // 1. 트랜잭션 밖에서 팔로워 목록 미리 조회
    List<UUID> followerIds = getFollowersBeforeTransaction(userId);

    // 2. 트윗 저장은 사용자 샤드에서 실행
    TweetResponse response = saveTweetInUserShard(userId, request);

    // 3. Fan-out은 각 샤드별로 독립 실행
    if (!followerIds.isEmpty()) {
      executeFanoutToShardsCorrectly(followerIds, response.getTweetId(), userId, request.getContent());
    }

    return response;
  }

  /**
   * ✅ 수정된 버전: 트랜잭션 시작 전에 샤드 설정
   */
  private void executeFanoutToShardsCorrectly(List<UUID> followerIds, UUID tweetId,
      UUID authorId, String tweetText) {
    // 팔로워를 샤드별로 그룹핑
    Map<String, List<UUID>> followersByShards = followerIds.stream()
        .collect(Collectors.groupingBy(followerId ->
            ShardUtil.selectTweetDataShardKeyByUserId(followerId)
        ));

    log.info("샤드별 팔로워 분포: {}",
        followersByShards.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().size()
            ))
    );

    // 각 샤드별로 순차 처리 (디버깅을 위해 일단 순차로)
    for (Map.Entry<String, List<UUID>> entry : followersByShards.entrySet()) {
      String shardKey = entry.getKey();
      List<UUID> shardFollowers = entry.getValue();

      try {
        executeFanoutInCorrectShard(shardKey, shardFollowers, tweetId, authorId, tweetText);
        log.info("샤드 Fan-out 성공 - shard: {}, 처리건수: {}", shardKey, shardFollowers.size());
      } catch (Exception e) {
        log.error("샤드별 Fan-out 실패 - shard: {}, 팔로워수: {}, error: {}",
            shardKey, shardFollowers.size(), e.getMessage(), e);
      }
    }
  }

  /**
   * ✅ 올바른 방법: 트랜잭션 시작 전에 샤드 설정
   */
  private void executeFanoutInCorrectShard(String shardKey, List<UUID> followers,
      UUID tweetId, UUID authorId, String tweetText) {

    log.info("샤드 Fan-out 시작 - shard: {}, 팔로워수: {}", shardKey, followers.size());

    // 트랜잭션이 시작되기 전에 샤드 키를 설정합니다.
    DataSourceConfig.setShard(shardKey);

    try {
      TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

      transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

      transactionTemplate.execute(status -> {
        // 이제 이 코드 블록은 방금 설정한 샤드에서 시작된 새 트랜잭션 안에서 실행됩니다.
        bulkInsertTimeline(followers, tweetId, authorId, tweetText);
        verifyInsertedShard(shardKey, tweetId); // 검증 로직은 그대로 유지 가능
        return null;
      });

    } finally {
      DataSourceConfig.clearShard();
    }
  }

  /**
   * 실제 저장된 샤드 확인 (디버깅용)
   */
  private void verifyInsertedShard(String expectedShard, UUID tweetId) {
    try {
      String currentShard = DataSourceConfig.getShard();

      // 저장된 데이터 확인
      String countSql = "SELECT COUNT(*) FROM user_timelines WHERE tweet_id = ?";
      int count = jdbcTemplate.queryForObject(countSql, Integer.class, tweetId.toString());

      log.info("샤드 검증 - 예상샤드: {}, 현재샤드: {}, 저장된 레코드 수: {}",
          expectedShard, currentShard, count);

    } catch (Exception e) {
      log.warn("샤드 검증 실패: {}", e.getMessage());
    }
  }

  /**
   * 벌크 인서트로 타임라인 저장 (개선된 버전)
   */
  /**
   * 벌크 인서트 (더 상세한 로깅)
   */
  private void bulkInsertTimeline(List<UUID> followers, UUID tweetId, UUID authorId, String tweetText) {
    if (followers.isEmpty()) return;

    String currentShard = DataSourceConfig.getShard();

    String sql = "INSERT INTO user_timelines (follower_id, tweet_id, author_id, tweet_text, created_at) VALUES (?, ?, ?, ?, NOW())";

    try {
      jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
          ps.setString(1, followers.get(i).toString());
          ps.setString(2, tweetId.toString());
          ps.setString(3, authorId.toString());
          ps.setString(4, tweetText);
        }

        @Override
        public int getBatchSize() {
          return followers.size();
        }
      });


    } catch (Exception e) {
      throw e;
    }
  }

  // 나머지 메서드들은 동일...
  private List<UUID> getFollowersBeforeTransaction(UUID authorId) {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    return transactionTemplate.execute(status -> {
      String userDataShardKey = "shard0"; // follow 테이블이 있는 샤드

      DataSourceConfig.setShard(userDataShardKey);
      try {
        List<UUID> followers = followRepository.findFollowerIds(authorId);
        log.info("팔로워 조회 완료 - authorId: {}, 팔로워 수: {}", authorId, followers.size());
        return followers;
      } finally {
        DataSourceConfig.clearShard();
      }
    });
  }

//  @Transactional Todo: 잠시 주석처리
  public TweetResponse saveTweetInUserShard(UUID userId, CreateTweetRequest request) {
    // 사용자 기반 샤드 설정
    String tweetDataShardKey = ShardUtil.selectTweetDataShardKeyByUserId(userId);
    DataSourceConfig.setShard(tweetDataShardKey);

    try {
      TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
      UUID tweetId = UUIDUtil.generate();
      LocalDateTime now = LocalDateTime.now();

      // 트윗 저장 (현재 샤드에서)
      saveTweetWithoutSharding(userId, tweetId, request.getContent(), now);

      log.info("트윗 저장 완료 - userId: {}, tweetId: {}, shard: {}",
          userId, tweetId, tweetDataShardKey);

      Tweet tweet = Tweet.builder()
          .tweetId(tweetId)
          .userId(userId)
          .tweetText(request.getContent())
          .createdAt(now)
          .build();

      return TweetResponse.of(tweet);

    } finally {
      DataSourceConfig.clearShard();
    }
  }


  private void saveTweetWithoutSharding(UUID userId, UUID tweetId, String content, LocalDateTime createdAt) {
    Tweet tweet = Tweet.builder()
        .tweetId(tweetId)
        .userId(userId)
        .tweetText(content)
        .createdAt(createdAt)
        .build();

    rdbTweetRepository.save(tweet);

//    log.debug("원본 트윗 저장 완료 - userId: {}, tweetId: {}", userId, tweetId);
  }
}