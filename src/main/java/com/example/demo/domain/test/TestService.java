package com.example.demo.domain.test;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;

    public TestEntity orderItem(Long userId) {
        if (userId == 0L) {
            throw new RuntimeException("예외 발생!");
        }

        TestEntity testEntity = testRepository.findById(userId).orElse(null);
        sleep(1000);
        return testEntity;
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
