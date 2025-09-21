import java.util.UUID;

public class TestSharding {
    public static void main(String[] args) {
        System.out.println("=== 샤딩 테스트 ===");
        
        // 기존 사용자들
        testUser("550e8400-e29b-41d4-a716-446655440100", "User C");
        testUser("550e8400-e29b-41d4-a716-446655440200", "User D");
        
        // 새로운 테스트 사용자들 (확실히 다른 샤드로 갈 수 있는)
        testUser("11111111-1111-1111-1111-111111111111", "User E");
        testUser("22222222-2222-2222-2222-222222222222", "User F");
        testUser("33333333-3333-3333-3333-333333333333", "User G");
        testUser("44444444-4444-4444-4444-444444444444", "User H");
    }
    
    public static void testUser(String uuidStr, String name) {
        UUID userId = UUID.fromString(uuidStr);
        System.out.println("\n" + name + " (" + uuidStr + ")");
        System.out.println("hashCode: " + userId.hashCode());
        
        // 트윗 데이터용 샤딩 (shard1, shard2, shard3)
        int shardCount = 3;
        int hash = userId.hashCode();
        int safeHash = (hash == Integer.MIN_VALUE) ? 0 : Math.abs(hash);
        int shardNum = (safeHash % shardCount) + 1;
        
        System.out.println("safeHash: " + safeHash);
        System.out.println("→ shard" + shardNum + " (트윗 데이터용)");
    }
}
