package com.example.ecommerce.order.api;

import com.example.ecommerce.order.api.dto.FlashSaleDtos;
import com.example.ecommerce.order.flashsale.FlashSaleService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 搶購排隊 API
 * - join：加入 queue，取得 ticketId
 * - status：輪詢 + 心跳（刷新 TTL）
 */
@RestController
@RequestMapping("/flashsale")
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    public FlashSaleController(FlashSaleService flashSaleService) {
        this.flashSaleService = flashSaleService;
    }

    @PostMapping("/products/{productId}/join")
    public FlashSaleDtos.JoinResponse join(Authentication authentication, @PathVariable("productId") Long productId) {
        Long userId = Long.valueOf(authentication.getName());
        return flashSaleService.joinQueue(userId, productId);
    }

    @GetMapping("/tickets/{ticketId}")
    public FlashSaleDtos.TicketStatusResponse status(@PathVariable("ticketId") String ticketId) {
        return flashSaleService.getTicketStatus(ticketId);
    }
}