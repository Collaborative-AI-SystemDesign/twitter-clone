package com.example.demo.domain.test_cassandra;

import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.List;
import java.util.UUID;

public interface CassandraTestRepository extends CassandraRepository<CassandraTestRecord, UUID> {
    List<CassandraTestRecord> findByAuthorName(String authorName);
}
