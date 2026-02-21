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

    // TODO 這個方法會在下一步被修改為搶購邏輯，這邊先不改動
    @PostMapping("/flash")
    public ResponseEntity<?> create(Authentication authentication,
                                    @RequestBody OrderDtos.CreateOrderRequest req) {
        Long userId = Long.valueOf(authentication.getName());
        var result = service.createOrder(userId, req);

        if (result.success()) return ResponseEntity.ok(result.order());

        // 這裡用較合理的 HTTP code
        return switch (result.message()) {
            case "OUT_OF_STOCK_OR_NOT_FOUND" -> ResponseEntity.status(HttpStatus.CONFLICT).body(result);
            case "BUSY_TRY_AGAIN" -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(result);
            default -> ResponseEntity.badRequest().body(result);
        };
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
