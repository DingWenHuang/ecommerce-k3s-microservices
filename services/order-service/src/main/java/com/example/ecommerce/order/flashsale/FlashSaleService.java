package com.example.ecommerce.order.flashsale;

import com.example.ecommerce.order.api.dto.FlashSaleDtos;
import com.example.ecommerce.order.client.ProductClient;
import com.example.ecommerce.order.service.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class FlashSaleService {

    private final FlashSaleRedisRepository redisRepository;
    private final ProductClient productClient;
    private final OrderService orderService;

    private final Duration ticketTtl;
    private final Duration resultTtl;

    public FlashSaleService(
            FlashSaleRedisRepository redisRepository,
            ProductClient productClient,
            OrderService orderService,
            @Value("${flashsale.ticket-ttl-seconds:15}") long ticketTtlSeconds,
            @Value("${flashsale.result-ttl-seconds:600}") long resultTtlSeconds
    ) {
        this.redisRepository = redisRepository;
        this.productClient = productClient;
        this.orderService = orderService;
        this.ticketTtl = Duration.ofSeconds(ticketTtlSeconds);
        this.resultTtl = Duration.ofSeconds(resultTtlSeconds);
    }

    public FlashSaleDtos.JoinResponse joinQueue(long userId, long productId) {
        // 1) 驗證商品是 FLASH_SALE（避免 NORMAL 誤入）
        ProductClient.ProductInfo product = productClient.getProductInfo(productId);
        if (!"FLASH_SALE".equals(product.productType())) {
            throw new IllegalStateException("Product is not FLASH_SALE");
        }

        // 2) 若使用者已在隊列中（active），回傳既有 ticketId（避免重複排隊）
        var existing = redisRepository.getActiveTicket(productId, userId);
        if (existing.isPresent()) {
            // 仍刷新 active TTL（表示仍在線/仍想排隊）
            redisRepository.setActiveTicket(productId, userId, existing.get(), ticketTtl);
            return new FlashSaleDtos.JoinResponse(existing.get());
        }

        // 3) 建立新 ticket
        String ticketId = UUID.randomUUID().toString();

        redisRepository.createTicket(ticketId, Map.of(
                "ticketId", ticketId,
                "userId", String.valueOf(userId),
                "productId", String.valueOf(productId),
                "status", FlashSaleTicketStatus.QUEUED.name(),
                "createdAt", Instant.now().toString()
        ), ticketTtl);

        // 4) 標記 active（避免同一 user 重複 join），TTL = ticketTtl
        redisRepository.setActiveTicket(productId, userId, ticketId, ticketTtl);

        // 5) 入隊 FIFO
        redisRepository.enqueue(productId, ticketId);

        return new FlashSaleDtos.JoinResponse(ticketId);
    }

    public FlashSaleDtos.TicketStatusResponse getTicketStatus(String ticketId) {
        // ticket 不存在 → EXPIRED（離線/TTL 到期）
        if (!redisRepository.ticketExists(ticketId)) {
            return new FlashSaleDtos.TicketStatusResponse(ticketId, null, FlashSaleTicketStatus.EXPIRED.name(), null, null);
        }

        Map<Object, Object> map = redisRepository.getTicket(ticketId);

        long productId = Long.parseLong(String.valueOf(map.get("productId")));
        long userId = Long.parseLong(String.valueOf(map.get("userId")));
        String status = String.valueOf(map.get("status"));

        // 重要：輪詢視同心跳，刷新 TTL（讓「離線離隊」成立）
        redisRepository.refreshTicketTtl(ticketId, ticketTtl);
        redisRepository.setActiveTicket(productId, userId, ticketId, ticketTtl);

        Long orderId = map.get("orderId") == null ? null : Long.parseLong(String.valueOf(map.get("orderId")));

        Integer position = null;
        if (FlashSaleTicketStatus.QUEUED.name().equals(status)) {
            // 只掃描前 5000 個避免太慢（demo 夠用；10k 也可調大）
            position = redisRepository.findPosition(productId, ticketId, 5000);
        }

        return new FlashSaleDtos.TicketStatusResponse(ticketId, productId, status, position, orderId);
    }

    /**
     * 由 worker 呼叫：處理隊頭 ticket
     * - 成功扣庫存 → 建立搶購訂單（單品項、單件）
     * - 庫存不足 → SOLD_OUT
     */
    @Transactional
    public void processTicket(String ticketId) {
        Map<Object, Object> map = redisRepository.getTicket(ticketId);
        long productId = Long.parseLong(String.valueOf(map.get("productId")));
        long userId = Long.parseLong(String.valueOf(map.get("userId")));

        // 如果 ticket 已經不是 QUEUED（例如重複處理），直接跳過
        String status = String.valueOf(map.get("status"));
        if (!FlashSaleTicketStatus.QUEUED.name().equals(status)) {
            return;
        }

        // 標記處理中（可讓前端看到 PROCESSING）
        redisRepository.updateTicket(ticketId, Map.of("status", FlashSaleTicketStatus.PROCESSING.name()));

        try {
            // 1) 扣庫存（FLASH_SALE 專用 endpoint，一次只能扣 1）
            productClient.reserveFlashSale(productId, new ProductClient.ReserveRequest(1));

            // 2) 建立搶購訂單（單品項、單件）
            long orderId = orderService.createFlashSaleOrder(userId, productId);

            redisRepository.updateTicket(ticketId, Map.of(
                    "status", FlashSaleTicketStatus.SUCCESS.name(),
                    "orderId", String.valueOf(orderId)
            ));

            // 成功結果保留一段時間，讓前端可看到 orderId
            redisRepository.setResultTtl(ticketId, resultTtl);
        } catch (Exception e) {
            // 庫存不足或其他錯誤 → SOLD_OUT（demo 先統一）
            redisRepository.updateTicket(ticketId, Map.of("status", FlashSaleTicketStatus.SOLD_OUT.name()));
            redisRepository.setResultTtl(ticketId, resultTtl);
        } finally {
            // 無論成功或售完，都讓使用者可以重新 join（不再 active）
            redisRepository.deleteActiveTicket(productId, userId);
        }
    }
}