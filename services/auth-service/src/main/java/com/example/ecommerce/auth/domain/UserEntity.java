package com.example.ecommerce.auth.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * 使用者資料表（最小可用版本）
 * - passwordHash：BCrypt 後的 hash，絕不存明文
 * - role：先用單一角色（USER/ADMIN），後面可擴展成多角色
 */
@Entity
@Table(name = "app_user", indexes = {
        @Index(name = "idx_app_user_username", columnList = "username", unique = true)
})
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String username;

    @Column(nullable = false, length = 120)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String role; // USER / ADMIN

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public UserEntity() {}

    public UserEntity(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setRole(String role) { this.role = role; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
