package com.example.demo.domain.test_cassandra;

import com.example.demo.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    
    @PostMapping("/batch")
    public ApiResponse<List<CassandraTestRecord>> createBatchRecords(
            @RequestParam String authorName,
            @RequestParam(defaultValue = "10") int count) {
        List<CassandraTestRecord> records = cassandraTestService.createBatchRecords(authorName, count);
        return ApiResponse.success(records);
    }

    @GetMapping("/{id}")
    public ApiResponse<CassandraTestRecord> findById(@PathVariable UUID id) {
        Optional<CassandraTestRecord> record = cassandraTestService.findById(id);
        return record.map(ApiResponse::success)
                .orElse(ApiResponse.fail("Record not found"));
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
    
    @GetMapping("/author/{authorName}/paged")
    public ApiResponse<Slice<CassandraTestRecord>> findByAuthorNamePaged(
            @PathVariable String authorName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Slice<CassandraTestRecord> records = cassandraTestService.findByAuthorNamePaged(authorName, page, size);
        return ApiResponse.success(records);
    }
    
    @GetMapping("/recent")
    public ApiResponse<List<CassandraTestRecord>> findRecentRecords(
            @RequestParam(defaultValue = "24") int hours) {
        List<CassandraTestRecord> records = cassandraTestService.findRecentRecords(hours);
        return ApiResponse.success(records);
    }
    
    @PutMapping("/{id}")
    public ApiResponse<CassandraTestRecord> updateRecord(
            @PathVariable UUID id, 
            @RequestBody UpdateRecordRequest request) {
        try {
            CassandraTestRecord record = cassandraTestService.updateRecord(id, request.getContent());
            return ApiResponse.success(record);
        } catch (RuntimeException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteRecord(@PathVariable UUID id) {
        cassandraTestService.deleteRecord(id);
        return ApiResponse.success("Record deleted successfully");
    }
    
    @DeleteMapping("/all")
    public ApiResponse<String> deleteAll() {
        cassandraTestService.deleteAll();
        return ApiResponse.success("All records deleted successfully");
    }
    
    @GetMapping("/count")
    public ApiResponse<Long> countAll() {
        long count = cassandraTestService.countAll();
        return ApiResponse.success(count);
    }
    
    @GetMapping("/count/author/{authorName}")
    public ApiResponse<Long> countByAuthor(@PathVariable String authorName) {
        long count = cassandraTestService.countByAuthor(authorName);
        return ApiResponse.success(count);
    }
}
