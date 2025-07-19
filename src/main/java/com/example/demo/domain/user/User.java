package com.example.demo.domain.user;

import com.example.demo.domain.CassandraBaseEntity;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("users")
@Getter
@Setter
public class User extends CassandraBaseEntity {

  @PrimaryKey("user_id")
  private UUID userId;

  @Column("user_name")
  private String userName;

  @Column("user_email")
  private String userEmail;

  @Column("user_password")
  private String userPassword;

  public User() {
  }

  public User(UUID userId, String userName, String userEmail, String userPassword, LocalDateTime createdAt) {
    this.userId = userId;
    this.userName = userName;
    this.userEmail = userEmail;
    this.userPassword = userPassword;
    this.setCreatedAt(createdAt);
  }

  @Override
  public String toString() {
    return "User{" +
        "userId=" + userId +
        ", userName='" + userName + '\'' +
        ", userEmail='" + userEmail + '\'' +
        ", userPassword='" + userPassword + '\'' +
        ", createdAt=" + createdAt +
        '}';
  }
}