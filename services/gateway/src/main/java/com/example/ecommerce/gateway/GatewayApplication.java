package com.example.ecommerce.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Gateway 入口：
 * - 作為所有 API 的統一入口（路由、權限驗證、限流等）
 * - 後續會在這裡加入 JWT 驗證 Filter
 */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
