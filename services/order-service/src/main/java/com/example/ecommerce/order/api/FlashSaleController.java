package com.example.ecommerce.order.api;

import com.example.ecommerce.order.api.dto.FlashSaleDtos;
import com.example.ecommerce.order.flashsale.FlashSaleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(FlashSaleController.class);

    private final FlashSaleService flashSaleService;

    public FlashSaleController(FlashSaleService flashSaleService) {
        this.flashSaleService = flashSaleService;
    }

    @PostMapping("/products/{productId}/join")
    public FlashSaleDtos.JoinResponse join(Authentication authentication, @PathVariable("productId") Long productId) {
        Long userId = Long.valueOf(authentication.getName());
        log.info("[flashsale.join] 加入搶購 userId={}, productId={}", userId, productId);
        FlashSaleDtos.JoinResponse resp = flashSaleService.joinQueue(userId, productId);
        log.info("[flashsale.join] 取得票券 userId={}, productId={}, ticketId={}", userId, productId, resp.ticketId());
        return resp;
    }

    @GetMapping("/tickets/{ticketId}")
    public FlashSaleDtos.TicketStatusResponse status(@PathVariable("ticketId") String ticketId) {
        log.info("[flashsale.status] 查詢票券狀態 ticketId={}", ticketId);
        FlashSaleDtos.TicketStatusResponse resp = flashSaleService.getTicketStatus(ticketId);
        log.info("[flashsale.status] 票券狀態 ticketId={}, status={}", ticketId, resp.status());
        return resp;
    }
}