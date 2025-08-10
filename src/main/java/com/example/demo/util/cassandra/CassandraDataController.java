package com.example.demo.util.cassandra;

import com.example.demo.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 카산드라 팔로우 관계 테스트 데이터 생성 REST API
 * 
 * 사용 예시:
 * POST /cassandra/data/generate-follows?userCount=1000001
 * GET /cassandra/data/statistics
 * DELETE /cassandra/data/clear
 */
@Slf4j
@RestController
@RequestMapping("/cassandra/data")
@RequiredArgsConstructor
public class CassandraDataController {

    private final CassandraDataGenerator dataGenerator;

    /**
     * 대용량 팔로우 관계 생성
     * 
     * @param userCount 총 유저 수 (기본값: 1000001)
     */
    @PostMapping("/generate-follows")
    public ApiResponse<String> generateFollowRelations(
            @RequestParam(defaultValue = "1000001") int userCount) {
        
        try {
            log.info("팔로우 관계 생성 요청: 총 유저 수 {}", userCount);
            
            long startTime = System.currentTimeMillis();
            dataGenerator.generateFollowRelations(userCount);
            long endTime = System.currentTimeMillis();
            
            String message = String.format("팔로우 관계 생성 완료 (소요시간: %d초)", (endTime - startTime) / 1000);
            log.info(message);
            
            return ApiResponse.success("팔로우 관계 생성 완료", message);
            
        } catch (Exception e) {
            log.error("팔로우 관계 생성 실패", e);
            return ApiResponse.fail("팔로우 관계 생성 실패: " + e.getMessage());
        }
    }

    /**
     * 팔로우 관계 통계 조회
     */
    @GetMapping("/statistics")
    public ApiResponse<String> getStatistics() {
        try {
            log.info("카산드라 팔로우 데이터 통계 요청");
            dataGenerator.logFollowStatistics();
            return ApiResponse.success("통계 조회 완료", "로그를 확인해주세요");
            
        } catch (Exception e) {
            log.error("통계 조회 실패", e);
            return ApiResponse.fail("통계 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 모든 팔로우 데이터 삭제
     */
    @DeleteMapping("/clear")
    public ApiResponse<String> clearAllData() {
        try {
            log.info("카산드라 팔로우 데이터 삭제 요청");
            
            long startTime = System.currentTimeMillis();
            dataGenerator.clearAllFollowData();
            long endTime = System.currentTimeMillis();
            
            String message = String.format("데이터 삭제 완료 (소요시간: %d초)", (endTime - startTime) / 1000);
            log.info(message);
            
            return ApiResponse.success("데이터 삭제 완료", message);
            
        } catch (Exception e) {
            log.error("데이터 삭제 실패", e);
            return ApiResponse.fail("데이터 삭제 실패: " + e.getMessage());
        }
    }

    /**
     * 테스트용 소규모 데이터 생성 (올바른 팔로워 수로 생성)
     */
    @PostMapping("/generate-test")
    public ApiResponse<String> generateTestData() {
        try {
            log.info("테스트용 팔로우 관계 생성 요청 (올바른 팔로워 수로)");
            
            // 모든 Celebrity 유저의 팔로워를 지원하도록 충분한 유저 수 설정
            long startTime = System.currentTimeMillis();
            dataGenerator.generateFollowRelations(200000);  // 20만명으로 설정 (user_60의 10만 팔로워 + 여유분)
            long endTime = System.currentTimeMillis();
            
            String message = String.format("테스트 데이터 생성 완료 (소요시간: %d초)", (endTime - startTime) / 1000);
            log.info(message);
            
            return ApiResponse.success("테스트 데이터 생성 완료", message);
            
        } catch (Exception e) {
            log.error("테스트 데이터 생성 실패", e);
            return ApiResponse.fail("테스트 데이터 생성 실패: " + e.getMessage());
        }
    }
} 