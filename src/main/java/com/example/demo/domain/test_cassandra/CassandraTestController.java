package com.example.demo.domain.test_cassandra;

import com.example.demo.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cassandra/test")
public class CassandraTestController {

    private final CassandraTestService cassandraTestService;

    @PostMapping
    public ApiResponse<String> createRecord(@RequestBody CreateRecordRequest request) {
        CassandraTestRecord record = cassandraTestService.createRecord(request.getAuthorName(), request.getContent());
        return ApiResponse.success(record.getId().toString());
    }

    @GetMapping
    public ApiResponse<List<CassandraTestRecord>> findAll() {
        List<CassandraTestRecord> records = cassandraTestService.findAll();
        return ApiResponse.success(records);
    }
    
    @GetMapping("/author/{authorName}")
    public ApiResponse<List<CassandraTestRecord>> findByAuthorName(@PathVariable String authorName) {
        List<CassandraTestRecord> records = cassandraTestService.findByAuthorName(authorName);
        return ApiResponse.success(records);
    }
    
    
}
