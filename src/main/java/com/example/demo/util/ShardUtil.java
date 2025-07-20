package com.example.demo.util;

import java.util.UUID;

public class ShardUtil {

  public static String selectShardKeyByUserId(UUID userId) {
    int shardCount = 3;
    int hash = userId.hashCode();
    int safeHash = (hash == Integer.MIN_VALUE) ? 0 : Math.abs(hash);
    int shardNum = (safeHash % shardCount) + 1;

    return "shard" + shardNum;
  }
}
