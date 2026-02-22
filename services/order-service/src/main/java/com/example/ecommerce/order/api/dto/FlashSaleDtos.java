package com.example.ecommerce.order.api.dto;

public class FlashSaleDtos {

    public record JoinResponse(String ticketId) {}

    public record TicketStatusResponse(
            String ticketId,
            Long productId,
            String status,     // QUEUED / PROCESSING / SUCCESS / SOLD_OUT / EXPIRED
            Integer position,  // 排隊位置（QUEUED 才有）
            Long orderId       // SUCCESS 才有
    ) {}
}