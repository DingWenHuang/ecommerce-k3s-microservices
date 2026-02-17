package com.example.ecommerce.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * product-service：
 * - 商品列表
 * - 庫存管理（補貨/扣庫存）
 * - 提供搶購核心「原子扣庫存」API（之後 Step 9/壓測會用）
 */
@SpringBootApplication
public class ProductServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
