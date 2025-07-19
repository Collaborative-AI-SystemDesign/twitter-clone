package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {
        "com.example.demo.domain.user",
        "com.example.demo.domain.test"
})
@EnableCassandraRepositories(basePackages = {
        "com.example.demo.domain.tweet",
        "com.example.demo.domain.follow",
        "com.example.demo.domain.timeline",
        "com.example.demo.domain.celebrity",
        "com.example.demo.domain.test_cassandra",
})
public class RepositoryConfig {
}
