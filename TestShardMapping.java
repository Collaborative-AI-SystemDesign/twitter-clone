import java.util.UUID;

public class TestShardMapping {
    
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
    
    public static void main(String[] args) {
        String user1 = "550e8400-e29b-41d4-a716-446655440001";
        String user2 = "550e8400-e29b-41d4-a716-446655440002";
        String user3 = "550e8400-e29b-41d4-a716-446655440003";
        String user4 = "550e8400-e29b-41d4-a716-446655440004";
        
        System.out.println("user1 (" + user1 + ") -> " + selectTweetDataShardKeyByUserId(UUID.fromString(user1)));
        System.out.println("user2 (" + user2 + ") -> " + selectTweetDataShardKeyByUserId(UUID.fromString(user2)));
        System.out.println("user3 (" + user3 + ") -> " + selectTweetDataShardKeyByUserId(UUID.fromString(user3)));
        System.out.println("user4 (" + user4 + ") -> " + selectTweetDataShardKeyByUserId(UUID.fromString(user4)));
    }
} 