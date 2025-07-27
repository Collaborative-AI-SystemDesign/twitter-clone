package com.example.demo.config;

import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy; // ✅ 임포트 추가
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StringUtils;

@Configuration
@EnableTransactionManagement
public class DataSourceConfig {

  public static ThreadLocal<String> currentShard = new ThreadLocal<>();

  public static void setShard(String shardKey) {
    String before = currentShard.get();
    currentShard.set(shardKey);
  }

  public static String getShard() {
    String shard = currentShard.get();
    return shard;
  }

  public static void clearShard() {
    String before = currentShard.get();
    currentShard.remove();
  }

  @Bean
  @ConfigurationProperties(prefix = "spring.datasource.shard0")
  public DataSource shard0DataSource() {
    return DataSourceBuilder.create().type(HikariDataSource.class).build();
  }

  @Bean
  @ConfigurationProperties(prefix = "spring.datasource.shard1")
  public DataSource shard1DataSource() {
    return DataSourceBuilder.create().type(HikariDataSource.class).build();
  }

  @Bean
  @ConfigurationProperties(prefix = "spring.datasource.shard2")
  public DataSource shard2DataSource() {
    return DataSourceBuilder.create().type(HikariDataSource.class).build();
  }

  @Bean
  @ConfigurationProperties(prefix = "spring.datasource.shard3")
  public DataSource shard3DataSource() {
    return DataSourceBuilder.create().type(HikariDataSource.class).build();
  }

  @Bean
  public DataSource routingDataSource(
      @Qualifier("shard0DataSource") DataSource shard0DataSource,
      @Qualifier("shard1DataSource") DataSource shard1DataSource,
      @Qualifier("shard2DataSource") DataSource shard2DataSource,
      @Qualifier("shard3DataSource") DataSource shard3DataSource
  ) {
    MyRoutingDataSource routingDataSource = new MyRoutingDataSource();

    Map<Object, Object> targetDataSources = new HashMap<>();
    targetDataSources.put("shard0", shard0DataSource);
    targetDataSources.put("shard1", shard1DataSource);
    targetDataSources.put("shard2", shard2DataSource);
    targetDataSources.put("shard3", shard3DataSource);

    routingDataSource.setTargetDataSources(targetDataSources);
    routingDataSource.setDefaultTargetDataSource(shard0DataSource);

    return routingDataSource;
  }

  @Bean
  @Primary // ✅ 이제 LazyConnectionDataSourceProxy가 Primary 입니다.
  public DataSource dataSource(DataSource routingDataSource) {
    return new LazyConnectionDataSourceProxy(routingDataSource);
  }

  // MyRoutingDataSource 클래스는 이전과 동일
  public static class MyRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
      String shardKey = currentShard.get();
      return StringUtils.hasText(shardKey) ? shardKey : "shard0";
    }
  }
}