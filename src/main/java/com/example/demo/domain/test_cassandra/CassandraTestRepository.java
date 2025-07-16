package com.example.demo.domain.test_cassandra;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CassandraTestRepository extends CassandraRepository<CassandraTestRecord, UUID> {
    
    List<CassandraTestRecord> findByAuthorName(String authorName);
    
    Slice<CassandraTestRecord> findByAuthorName(String authorName, Pageable pageable);
    
    List<CassandraTestRecord> findByCreatedAtAfter(LocalDateTime dateTime);
    
    long countByAuthorName(String authorName);
}
