package com.example.demo.domain.user;

import com.example.demo.domain.MySqlBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Table(name = "user")
@Entity
@NoArgsConstructor
public class User extends MySqlBaseEntity {

  @Id
  @Column(name = "user_id", columnDefinition = "BINARY(16)")
  private UUID userId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "password", nullable = false)
  private String password;

  public User(UUID userId, String name, String email, String password) {
    this.userId = userId;
    this.name = name;
    this.email = email;
    this.password = password;
  }
}
