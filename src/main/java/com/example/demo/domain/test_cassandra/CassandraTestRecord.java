package com.example.demo.domain.test_cassandra;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.Indexed;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("cassandra_test_table")
@Getter
@Setter
public class CassandraTestRecord {

    @PrimaryKey
    private UUID id;
    
    @Indexed
    private String authorName;
    
    private String content;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    public CassandraTestRecord() {}

    public CassandraTestRecord(UUID id, String authorName, String content) {
        this.id = id;
        this.authorName = authorName;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateContent(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }
}
