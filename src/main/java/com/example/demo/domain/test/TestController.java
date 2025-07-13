package com.example.demo.domain.test;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;

    @GetMapping("/v1/test/{userId}")
    public TestEntity request(@PathVariable Long userId) {
        return testService.orderItem(userId);
    }
}
