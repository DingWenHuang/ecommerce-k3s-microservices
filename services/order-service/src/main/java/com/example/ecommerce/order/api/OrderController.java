package com.example.ecommerce.order.api;

import com.example.ecommerce.order.api.dto.OrderDtos;
import com.example.ecommerce.order.service.OrderService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 訂單 API：
 * - POST /orders：建立訂單
 * - GET /orders：查詢我的訂單
 *
 * 使用者身份從 Gateway header 轉成 Authentication
 * - authentication.getName() 會是 userId（字串）
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping("/normal")
    public ResponseEntity<?> createNormalOrder(Authentication authentication,
                                    @RequestBody OrderDtos.CreateNormalOrderRequest req) {
        Long userId = Long.valueOf(authentication.getName());
        var result = service.createNormalOrder(userId, req);
        if (result.success()) return ResponseEntity.ok(result.order());

        return ResponseEntity.badRequest().body(result);
    }

    @GetMapping
    public ResponseEntity<List<OrderDtos.OrderResponse>> myOrders(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        return ResponseEntity.ok(service.listMyOrders(userId));
    }
}
