package com.example.demo.util;

import java.util.UUID;

public class ShardUtil {

  /**
   * 사용자 데이터용 샤드 키 (항상 shard0)
   * User, Follows 테이블용
   */
  public static String selectUserDataShardKey() {
    return "shard0";
  }

  /**
   * 트윗 데이터용 샤드 키 (사용자 ID 기반으로 shard1, shard2, shard3 중 선택)
   * TweetsByUser, UserTimeline 테이블용
   */
  public static String selectTweetDataShardKeyByUserId(UUID userId) {
    int shardCount = 3;
    byte[] uuidBytes = new byte[16];
    long mostSigBits = userId.getMostSignificantBits();
    long leastSigBits = userId.getLeastSignificantBits();

    for (int i = 0; i < 8; i++) {
      uuidBytes[i] = (byte) (mostSigBits >>> (8 * (7 - i)));
    }

    for (int i = 8; i < 16; i++) {
      uuidBytes[i] = (byte) (leastSigBits >>> (8 * (15 - i)));
    }

    int hash = 0;
    for (int i = 0; i < uuidBytes.length; i++) {
      hash = hash * 31 + uuidBytes[i];
    }

    int safeHash = Math.abs(hash);
    int shardNum = (safeHash % shardCount) + 1; // 1, 2, 3

    return "shard" + shardNum;
  }

  /**
   * 기존 호환성을 위한 메서드 (트윗 데이터용으로 리다이렉트)
   */
  public static String selectShardKeyByUserId(UUID userId) {
    return selectTweetDataShardKeyByUserId(userId);
  }
}
