package com.example.ecommerce.order.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 下單 DTO：
 * - unitPrice 使用 BigDecimal（重要）
 * - demo 先支援單品下單（搶購場景）
 */
public class OrderDtos {

    public record CreateOrderRequest(
            Long productId,
            Integer quantity,
            BigDecimal unitPrice
    ) {}

    public record OrderItemResponse(
            Long productId,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal lineAmount
    ) {}

    public record OrderResponse(
            Long orderId,
            BigDecimal totalAmount,
            String status,
            List<OrderItemResponse> items
    ) {}

    public record CreateOrderResult(boolean success, String message, OrderResponse order) {}
}
