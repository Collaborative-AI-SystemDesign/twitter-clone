package com.example.demo.domain.user;

import com.example.demo.util.UUID.UUIDUtil;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.data.cassandra.CassandraConnectionFailureException;
import org.springframework.stereotype.Component;

@Profile("test")
@Component
@RequiredArgsConstructor
public class UserTestRunner implements CommandLineRunner {

  private final UserRepository userRepository;

  @Override
  public void run(String... args) throws Exception {
    System.out.println("--- 카산드라 사용자 엔티티 테스트 시작 ---");

    int maxRetries = 5; // 최대 재시도 횟수
    long delayMillis = 5000; // 5초 지연

    for (int i = 0; i < maxRetries; i++) {
      try {
        // 1. 새 사용자 생성 및 저장 시도
        UUID newUserId = UUIDUtil.generate();
        User newUser = new User(
            newUserId,
            "testuser",
            "test@example.com",
            "hashed_password_123",
            LocalDateTime.now()
        );

        System.out.println("저장할 사용자: " + newUser);
        userRepository.save(newUser); // 여기서 연결 시도가 발생
        System.out.println("사용자 저장 완료.");

        // 2. 저장된 사용자 조회 시도
        Optional<User> foundUserOptional = userRepository.findById(newUserId);
        if (foundUserOptional.isPresent()) {
          User foundUser = foundUserOptional.get();
          System.out.println("조회된 사용자: " + foundUser);
          if (foundUser.getUserName().equals("testuser") && foundUser.getUserEmail().equals("test@example.com")) {
            System.out.println("테이블 매핑 및 데이터 저장/조회 성공!");
          } else {
            System.err.println("오류: 저장된 데이터와 조회된 데이터가 일치하지 않습니다.");
          }
        } else {
          System.err.println("오류: 사용자를 찾을 수 없습니다! 매핑에 문제 있을 수 있음.");
        }

        // 성공적으로 실행되었으면 반복문 종료
        break;

      } catch (CassandraConnectionFailureException e) {
        // 카산드라 연결 실패 예외 발생 시
        System.err.println("카산드라 연결 실패 (재시도 " + (i + 1) + "/" + maxRetries + "): " + e.getMessage());
        if (i < maxRetries - 1) {
          System.out.println(delayMillis / 1000 + "초 후 재시도합니다...");
          Thread.sleep(delayMillis); // 5초 대기
        } else {
          System.err.println("최대 재시도 횟수 초과. 카산드라 연결에 실패했습니다.");
          e.printStackTrace(); // 마지막 시도에서 실패했으면 스택 트레이스 출력
        }
      } catch (DataAccessException e) {
        // Spring Data에서 발생하는 다른 데이터 접근 예외
        System.err.println("데이터 접근 중 오류 발생: " + e.getMessage());
        e.printStackTrace();
        break; // 다른 유형의 에러는 재시도 없이 종료
      } catch (Exception e) {
        // 기타 예상치 못한 예외
        System.err.println("예상치 못한 오류 발생: " + e.getMessage());
        e.printStackTrace();
        break; // 다른 유형의 에러는 재시도 없이 종료
      }
    }

    System.out.println("--- 카산드라 사용자 엔티티 테스트 종료 ---");
  }
}