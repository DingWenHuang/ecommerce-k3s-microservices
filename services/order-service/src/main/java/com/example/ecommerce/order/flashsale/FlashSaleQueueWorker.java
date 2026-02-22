package com.example.ecommerce.order.flashsale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * 簡化版 worker：
 * - 每次 tick 嘗試處理「所有可能的 FLASH_SALE 商品隊列」
 *
 * demo 的前提：你搶購商品通常不多（1~數個），所以可以用掃描方式。
 * 若商品很多，應改成「有 join 時把 productId 放到 activeProducts set」，worker 只處理 set 中的 id。
 */
@Component
public class FlashSaleQueueWorker {

    private final FlashSaleRedisRepository redisRepository;
    private final FlashSaleService flashSaleService;

    private final long pollIntervalMs;

    public FlashSaleQueueWorker(
            FlashSaleRedisRepository redisRepository,
            FlashSaleService flashSaleService,
            @Value("${flashsale.worker.poll-interval-ms:30}") long pollIntervalMs
    ) {
        this.redisRepository = redisRepository;
        this.flashSaleService = flashSaleService;
        this.pollIntervalMs = pollIntervalMs;
    }

    /**
     * worker tick：
     * - demo 版先處理 productId=1 的搶購商品（目前 Flash Sale Ticket 就是 id=1）
     */
    @Scheduled(fixedDelayString = "${flashsale.worker.poll-interval-ms:30}")
    public void tick() {
        long productId = 1L; // demo：先固定處理一個搶購商品
        processOneProductQueue(productId);
    }

    private void processOneProductQueue(long productId) {
        String lockValue = UUID.randomUUID().toString();
        boolean locked = redisRepository.tryLock(productId, lockValue, Duration.ofSeconds(2));
        if (!locked) return;

        try {
            // 一次只處理一張票（避免長時間佔鎖）
            String headTicketId = redisRepository.peekQueueHead(productId);
            if (headTicketId == null) return;

            // ticket 若已過期（離線離隊）→ 直接丟棄
            if (!redisRepository.ticketExists(headTicketId)) {
                redisRepository.popQueueHead(productId);
                return;
            }

            // 真的處理：扣庫存 + 建單 + 更新狀態
            flashSaleService.processTicket(headTicketId);

            // 成功或售完，都把隊頭移除（下一輪處理下一位）
            redisRepository.popQueueHead(productId);
        } finally {
            redisRepository.unlock(productId, lockValue);
        }
    }
}