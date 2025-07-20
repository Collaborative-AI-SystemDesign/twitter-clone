package com.example.demo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.jsonwebtoken.lang.Assert;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StringUtils;

@Configuration
@EnableTransactionManagement // @Transactional 활성화를 위해 필요
public class DataSourceConfig {

  // ThreadLocal을 사용하여 현재 스레드에 샤드 키를 저장
  // 이 값에 따라 AbstractRoutingDataSource가 어떤 샤드를 선택할지 결정
  public static ThreadLocal<String> currentShard = new ThreadLocal<>();

  /**
   * 현재 스레드의 샤드 키를 설정합니다.
   * 데이터베이스 작업을 수행하기 전에 반드시 호출되어야 합니다.
   *
   * @param shardKey 설정할 샤드 키 (예: "shard0", "shard1", "shard2")
   */
  public static void setShard(String shardKey) {
    currentShard.set(shardKey);
  }

  /**
   * 현재 스레드에 설정된 샤드 키를 반환합니다.
   * @return 현재 샤드 키
   */
  public static String getShard() {
    return currentShard.get();
  }

  /**
   * 현재 스레드의 샤드 키를 제거합니다.
   * 데이터베이스 작업 완료 후 반드시 호출하여 ThreadLocal을 정리해야 합니다.
   * 그렇지 않으면 스레드 풀 환경에서 잘못된 샤드로 연결될 수 있습니다.
   */
  public static void clearShard() {
    currentShard.remove();
  }

  /**
   * 첫 번째 MySQL 샤드(shard0)에 대한 DataSource 빈을 정의합니다.
   * application.yml의 'spring.datasource.shard0' 접두사로 시작하는 속성들이
   * 이 DataSource 빈에 자동으로 바인딩됩니다.
   */
  @Bean
  @ConfigurationProperties(prefix = "spring.datasource.shard0")
  public DataSource shard0DataSource() {
    // DataSourceBuilder를 사용하여 HikariDataSource 인스턴스를 생성하고 반환
    // @ConfigurationProperties가 이 DataSource 빈에 설정을 직접 바인딩
    return DataSourceBuilder.create().type(HikariDataSource.class).build();
  }

  /**
   * 두 번째 MySQL 샤드(shard1)에 대한 DataSource 빈을 정의합니다.
   */
  @Bean
  @ConfigurationProperties(prefix = "spring.datasource.shard1")
  public DataSource shard1DataSource() {
    return DataSourceBuilder.create().type(HikariDataSource.class).build();
  }

  /**
   * 세 번째 MySQL 샤드(shard2)에 대한 DataSource 빈을 정의합니다.
   */
  @Bean
  @ConfigurationProperties(prefix = "spring.datasource.shard2")
  public DataSource shard2DataSource() {
    return DataSourceBuilder.create().type(HikariDataSource.class).build();
  }

  /**
   * 네 번째 MySQL 샤드(shard2)에 대한 DataSource 빈을 정의합니다.
   */
  @Bean
  @ConfigurationProperties(prefix = "spring.datasource.shard3")
  public DataSource shard3DataSource() {
    return DataSourceBuilder.create().type(HikariDataSource.class).build();
  }

  /**
   * 동적 라우팅을 위한 주(Primary) DataSource 빈을 정의합니다.
   * 이 DataSource를 통해 애플리케이션의 모든 DB 작업이 라우팅됩니다.
   */
  @Bean
  @Primary // 이 DataSource를 주 DataSource로 설정
  public DataSource routingDataSource() {
    MyRoutingDataSource routingDataSource = new MyRoutingDataSource();

    // 각 샤드 DataSource를 맵에 등록 (키: 샤드 키 문자열, 값: 실제 DataSource 인스턴스)
    Map<Object, Object> targetDataSources = new HashMap<>();
    targetDataSources.put("shard0", shard0DataSource());
    targetDataSources.put("shard1", shard1DataSource());
    targetDataSources.put("shard2", shard2DataSource());
    targetDataSources.put("shard3", shard0DataSource());

    routingDataSource.setTargetDataSources(targetDataSources);
    // 샤드 키가 결정되지 않았을 때 사용할 기본/폴백 DataSource 설정
    routingDataSource.setDefaultTargetDataSource(shard0DataSource());
    return routingDataSource;
  }

  /**
   * AbstractRoutingDataSource를 상속받아 런타임에 샤드 키를 결정하는 클래스.
   * DataSourceConfig.currentShard (ThreadLocal)에 저장된 값을 참조합니다.
   */
  public static class MyRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
      String shardKey = currentShard.get();
      // StringUtils.hasText()를 사용하여 null 여부뿐만 아니라 빈 문자열인지도 확인
      if (StringUtils.hasText(shardKey)) {
        return shardKey; // ThreadLocal에 설정된 샤드 키 반환
      }
      // 샤드 키가 설정되지 않은 경우 (예: 애플리케이션 초기화, JPA DDL 작업 등)
      // 기본 샤드("shard0")를 사용하도록 폴백 (이전에 발생했던 에러 방지)
      System.out.println("No shard key set in ThreadLocal, defaulting to shard0. This might be due to JPA/Hibernate DDL operations or un-sharded queries.");
      return "shard0";
    }
  }
}