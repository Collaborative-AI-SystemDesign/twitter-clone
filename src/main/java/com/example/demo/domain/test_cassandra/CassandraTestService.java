package com.example.demo.domain.test_cassandra;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CassandraTestService {
    private final CassandraTestRepository cassandraTestRepository;

    public CassandraTestRecord createRecord(String authorName, String content) {
        return cassandraTestRepository.save(new CassandraTestRecord(UUID.randomUUID(), authorName, content));
    }

    public List<CassandraTestRecord> findByAuthorName(String authorName) {
        return cassandraTestRepository.findByAuthorName(authorName);
    }
    
    public List<CassandraTestRecord> findAll() {
        return cassandraTestRepository.findAll();
    }
}
