package com.example.ecommerce.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * auth-service：
 * - 負責註冊 / 登入 / 發放 JWT
 * - 提供 /auth/me 讓你測試 JWT 是否有效
 */
@SpringBootApplication
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
