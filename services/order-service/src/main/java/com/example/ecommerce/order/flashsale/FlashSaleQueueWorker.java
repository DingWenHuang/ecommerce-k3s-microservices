package com.example.ecommerce.order.flashsale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class FlashSaleQueueWorker {

    private static final Logger log = LoggerFactory.getLogger(FlashSaleQueueWorker.class);

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

    @Scheduled(fixedDelayString = "${flashsale.worker.poll-interval-ms:30}")
    public void tick() {
        long productId = 1L; // demo：先固定處理一個搶購商品
        processOneProductQueue(productId);
    }

    private void processOneProductQueue(long productId) {
        String lockValue = UUID.randomUUID().toString();
        boolean locked = redisRepository.tryLock(productId, lockValue, Duration.ofSeconds(2));
        if (!locked) return;

        String ticketId = null;
        long dequeueSeq = 0L;

        try {
            ticketId = redisRepository.popQueueHead(productId);
            if (ticketId == null) return;

            log.info("[worker] 取出票券 productId={}, ticketId={}", productId, ticketId);

            // 這張票拿到「出隊順序」：用來做 FIFO 證據
            dequeueSeq = redisRepository.nextDequeueSeq(productId);

            // 如果 ticket 已不存在（離線/TTL到）：
            // - 既然已出隊，就直接丟棄即可（這張不算成功）
            if (!redisRepository.ticketExists(ticketId)) {
                log.warn("[worker] 票券已過期（離線/TTL到），略過 ticketId={}", ticketId);
                return;
            }

            // 先把 ticket TTL 延長，避免處理中途過期導致 hash 欄位丟失
            //（用 resultTtl 或你想要的保留時間，這裡交給 service 決定更好）
            flashSaleService.extendTicketForProcessing(ticketId);

            // 先寫入 successSeq（其實是 dequeue seq）
            // 這樣 winners 以 successSeq 排時，順序會反映 FIFO 出隊順序
            flashSaleService.markDequeued(ticketId, dequeueSeq);

        } finally {
            redisRepository.unlock(productId, lockValue);
        }

        // 鎖外處理：reserve + 建單（慢的事情在鎖外做）
        if (ticketId != null && redisRepository.ticketExists(ticketId)) {
            log.info("[worker] 開始處理票券 productId={}, ticketId={}, dequeueSeq={}", productId, ticketId, dequeueSeq);
            flashSaleService.processTicket(ticketId);
        }
    }
}