package com.example.ecommerce.order.api.dto;

public class FlashSaleDtos {

    public record JoinResponse(String ticketId, Long enqueueSeq) {}

    public record TicketStatusResponse(
            String ticketId,
            Long productId,
            String status,     // QUEUED / PROCESSING / SUCCESS / SOLD_OUT / EXPIRED
            Integer position,  // 排隊位置（QUEUED 才有）
            Long orderId,       // SUCCESS 才有
            Long enqueueSeq,
            Long successSeq
    ) {}

    /**
     * Internal endpoint 回傳用 DTO
     */
    public record FlashSaleWinnerDto(
            String userId,
            String orderId,
            long enqueueSeq,
            long successSeq
    ) {}
}