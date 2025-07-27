package com.example.demo.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class JdbcTemplateConfig {

  @Bean
  public JdbcTemplate jdbcTemplate(@Qualifier("routingDataSource") DataSource routingDataSource) {
    System.out.println("✅ JdbcTemplate이 routingDataSource 사용 중");
    return new JdbcTemplate(routingDataSource);
  }
}
