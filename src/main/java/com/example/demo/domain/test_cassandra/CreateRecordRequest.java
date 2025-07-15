package com.example.demo.domain.test_cassandra;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRecordRequest {
    private String authorName;
    private String content;
}