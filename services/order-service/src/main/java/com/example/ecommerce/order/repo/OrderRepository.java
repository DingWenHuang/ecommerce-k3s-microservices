package com.example.ecommerce.order.repo;

import com.example.ecommerce.order.domain.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 訂單查詢：
 * - findByUserId：查詢該使用者的訂單
 */
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
