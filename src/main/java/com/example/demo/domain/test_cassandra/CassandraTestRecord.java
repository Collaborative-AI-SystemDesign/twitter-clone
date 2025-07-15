package com.example.demo.domain.test_cassandra;

import com.example.demo.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.Indexed;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

@Table("Cassandra_test_table")
@Getter
@Setter
public class CassandraTestRecord extends BaseEntity {

    @PrimaryKey
    private UUID id;
    
    @Indexed
    private String authorName;
    
    private String content;

    public CassandraTestRecord() {}

    public CassandraTestRecord(UUID id, String authorName, String content) {
        this.id = id;
        this.authorName = authorName;
        this.content = content;
    }
}
