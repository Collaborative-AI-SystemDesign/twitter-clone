package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.example.demo.domain.test")
@EnableCassandraRepositories(basePackages = "com.example.demo.domain.test_cassandra")
public class RepositoryConfig {
}
