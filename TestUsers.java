import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public class TestUsers {
    public static void main(String[] args) {
        System.out.println("=== 10명의 테스트 사용자 샤드 매핑 ===");
        
        // 10명의 테스트 사용자 ID 생성
        String[] userIds = {
            "10000000-1000-1000-1000-100000000001",
            "20000000-2000-2000-2000-200000000002", 
            "30000000-3000-3000-3000-300000000003",
            "40000000-4000-4000-4000-400000000004",
            "50000000-5000-5000-5000-500000000005",
            "60000000-6000-6000-6000-600000000006",
            "70000000-7000-7000-7000-700000000007",
            "80000000-8000-8000-8000-800000000008",
            "90000000-9000-9000-9000-900000000009",
            "a0000000-a000-a000-a000-a00000000010"
        };
        
        int[] shardCounts = new int[4]; // shard1, shard2, shard3 카운트
        
        for (int i = 0; i < userIds.length; i++) {
            UUID userId = UUID.fromString(userIds[i]);
            
            // 트윗 데이터용 샤딩 (shard1, shard2, shard3)
            int shardCount = 3;
            int hash = userId.hashCode();
            int safeHash = (hash == Integer.MIN_VALUE) ? 0 : Math.abs(hash);
            int shardNum = (safeHash % shardCount) + 1;
            
            System.out.println("User" + (i+1) + " (" + userIds[i] + ") -> shard" + shardNum);
            shardCounts[shardNum]++;
        }
        
        System.out.println("\n=== 샤드별 분포 ===");
        for (int i = 1; i <= 3; i++) {
            System.out.println("shard" + i + ": " + shardCounts[i] + "명");
        }
    }
}
