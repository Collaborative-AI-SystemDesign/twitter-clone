package com.example.demo.domain.test_cassandra;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.cassandra.DataCassandraTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataCassandraTest
@ActiveProfiles("test")
@Import(CassandraTestService.class)
class CassandraTestServiceTest {

    @Autowired
    private CassandraTestService cassandraTestService;

    @BeforeEach
    void setUp() {
        // 테스트 전에 모든 데이터 정리
        cassandraTestService.deleteAll();
    }

    @Test
    void testCreateRecord() {
        // Given
        String authorName = "testAuthor";
        String content = "Test content";

        // When
        CassandraTestRecord record = cassandraTestService.createRecord(authorName, content);

        // Then
        assertNotNull(record);
        assertNotNull(record.getId());
        assertEquals(authorName, record.getAuthorName());
        assertEquals(content, record.getContent());
        assertNotNull(record.getCreatedAt());
        assertNotNull(record.getUpdatedAt());
    }

    @Test
    void testFindById() {
        // Given
        CassandraTestRecord created = cassandraTestService.createRecord("author1", "content1");

        // When
        Optional<CassandraTestRecord> found = cassandraTestService.findById(created.getId());

        // Then
        assertTrue(found.isPresent());
        assertEquals(created.getId(), found.get().getId());
        assertEquals(created.getAuthorName(), found.get().getAuthorName());
    }

    @Test
    void testFindByAuthorName() {
        // Given
        String authorName = "testAuthor";
        cassandraTestService.createRecord(authorName, "content1");
        cassandraTestService.createRecord(authorName, "content2");
        cassandraTestService.createRecord("otherAuthor", "content3");

        // When
        List<CassandraTestRecord> records = cassandraTestService.findByAuthorName(authorName);

        // Then
        assertEquals(2, records.size());
        assertTrue(records.stream().allMatch(r -> r.getAuthorName().equals(authorName)));
    }

    @Test
    void testCreateBatchRecords() {
        // Given
        String authorName = "batchAuthor";
        int count = 5;

        // When
        List<CassandraTestRecord> records = cassandraTestService.createBatchRecords(authorName, count);

        // Then
        assertEquals(count, records.size());
        assertTrue(records.stream().allMatch(r -> r.getAuthorName().equals(authorName)));
        
        // Verify in database
        List<CassandraTestRecord> dbRecords = cassandraTestService.findByAuthorName(authorName);
        assertEquals(count, dbRecords.size());
    }

    @Test
    void testUpdateRecord() {
        // Given
        CassandraTestRecord created = cassandraTestService.createRecord("author1", "original content");
        String newContent = "updated content";

        // When
        CassandraTestRecord updated = cassandraTestService.updateRecord(created.getId(), newContent);

        // Then
        assertEquals(created.getId(), updated.getId());
        assertEquals(newContent, updated.getContent());
        assertTrue(updated.getUpdatedAt().isAfter(created.getUpdatedAt()) || 
                  updated.getUpdatedAt().equals(created.getUpdatedAt()));
    }

    @Test
    void testDeleteRecord() {
        // Given
        CassandraTestRecord created = cassandraTestService.createRecord("author1", "content1");

        // When
        cassandraTestService.deleteRecord(created.getId());

        // Then
        Optional<CassandraTestRecord> found = cassandraTestService.findById(created.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void testCountOperations() {
        // Given
        String author1 = "author1";
        String author2 = "author2";
        
        cassandraTestService.createRecord(author1, "content1");
        cassandraTestService.createRecord(author1, "content2");
        cassandraTestService.createRecord(author2, "content3");

        // When & Then
        assertEquals(2, cassandraTestService.countByAuthor(author1));
        assertEquals(1, cassandraTestService.countByAuthor(author2));
        assertEquals(3, cassandraTestService.countAll());
    }

    @Test
    void testUpdateNonExistentRecord() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            cassandraTestService.updateRecord(nonExistentId, "new content");
        });
    }
} 