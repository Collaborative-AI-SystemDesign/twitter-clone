import java.util.UUID;

public class test_shard {
    public static void main(String[] args) {
        UUID user1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID user2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        
        System.out.println("User1 ID: " + user1);
        System.out.println("User1 hashCode: " + user1.hashCode());
        
        int shardCount = 3;
        int hash1 = user1.hashCode();
        int safeHash1 = (hash1 == Integer.MIN_VALUE) ? 0 : Math.abs(hash1);
        int shardNum1 = (safeHash1 % shardCount) + 1;
        System.out.println("User1 -> shard" + shardNum1);
        
        System.out.println("\nUser2 ID: " + user2);
        System.out.println("User2 hashCode: " + user2.hashCode());
        
        int hash2 = user2.hashCode();
        int safeHash2 = (hash2 == Integer.MIN_VALUE) ? 0 : Math.abs(hash2);
        int shardNum2 = (safeHash2 % shardCount) + 1;
        System.out.println("User2 -> shard" + shardNum2);
    }
}
