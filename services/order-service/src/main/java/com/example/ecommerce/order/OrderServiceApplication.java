package com.example.ecommerce.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * order-service：
 * - 建立訂單（搶購核心在這裡）
 * - 透過 Redis lock 控制併發臨界區
 * - 透過 Feign 呼叫 product-service 的 internal reserve 做 DB 原子扣庫存
 */
@EnableFeignClients
@SpringBootApplication
@EnableScheduling
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
