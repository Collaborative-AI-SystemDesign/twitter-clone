package com.example.demo.domain.test_cassandra;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CassandraTestService {
    private final CassandraTestRepository cassandraTestRepository;

    public CassandraTestRecord createRecord(String authorName, String content) {
        return cassandraTestRepository.save(new CassandraTestRecord(UUID.randomUUID(), authorName, content));
    }
    
    public List<CassandraTestRecord> createBatchRecords(String authorName, int count) {
        List<CassandraTestRecord> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            records.add(new CassandraTestRecord(UUID.randomUUID(), authorName, "Test content " + (i + 1)));
        }
        return cassandraTestRepository.saveAll(records);
    }

    public Optional<CassandraTestRecord> findById(UUID id) {
        return cassandraTestRepository.findById(id);
    }

    public List<CassandraTestRecord> findByAuthorName(String authorName) {
        return cassandraTestRepository.findByAuthorName(authorName);
    }
    
    public Slice<CassandraTestRecord> findByAuthorNamePaged(String authorName, int page, int size) {
        return cassandraTestRepository.findByAuthorName(authorName, PageRequest.of(page, size));
    }
    
    public List<CassandraTestRecord> findRecentRecords(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return cassandraTestRepository.findByCreatedAtAfter(since);
    }
    
    public List<CassandraTestRecord> findAll() {
        return cassandraTestRepository.findAll();
    }
    
    public CassandraTestRecord updateRecord(UUID id, String content) {
        Optional<CassandraTestRecord> optionalRecord = cassandraTestRepository.findById(id);
        if (optionalRecord.isPresent()) {
            CassandraTestRecord record = optionalRecord.get();
            record.updateContent(content);
            return cassandraTestRepository.save(record);
        }
        throw new RuntimeException("Record not found with id: " + id);
    }
    
    public void deleteRecord(UUID id) {
        cassandraTestRepository.deleteById(id);
    }
    
    public void deleteAll() {
        cassandraTestRepository.deleteAll();
    }
    
    public long countByAuthor(String authorName) {
        return cassandraTestRepository.countByAuthorName(authorName);
    }
    
    public long countAll() {
        return cassandraTestRepository.count();
    }
}
