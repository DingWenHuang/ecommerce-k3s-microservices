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
            @Value("${flashsale.ticket-ttl-seconds:60}") long ticketTtlSeconds,
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
            String existingTicketId = existing.get();

            // 仍刷新 active TTL（表示仍在線/仍想排隊）
            redisRepository.setActiveTicket(productId, userId, existing.get(), ticketTtl);

            // 補回 enqueueSeq（從 ticket hash 讀）
            Long enqueueSeq = null;
            if (redisRepository.ticketExists(existingTicketId)) {
                Map<Object, Object> t = redisRepository.getTicket(existingTicketId);
                Object v = t.get("enqueueSeq");
                if (v != null) enqueueSeq = Long.parseLong(String.valueOf(v));
            }

            return new FlashSaleDtos.JoinResponse(existingTicketId, enqueueSeq);
        }

        // 3) 建立新 ticket（先建 hash，worker 依此確認 ticket 存在）
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

        // 5) 原子入隊：Lua 腳本一次完成 INCR(enqueueSeq) + RPUSH(queue)
        //    保證 seq 遞增順序 == Redis List 插入順序，FIFO 不再有競態
        long enqueueSeq = redisRepository.atomicEnqueueAndGetSeq(productId, ticketId);

        // 6) 將確定的 enqueueSeq 寫回 ticket hash（供 evidence 查詢）
        redisRepository.updateTicket(ticketId, Map.of("enqueueSeq", String.valueOf(enqueueSeq)));

        return new FlashSaleDtos.JoinResponse(ticketId, enqueueSeq);
    }

    public FlashSaleDtos.TicketStatusResponse getTicketStatus(String ticketId) {
        // ticket 不存在 → EXPIRED（離線/TTL 到期）
        if (!redisRepository.ticketExists(ticketId)) {
            return new FlashSaleDtos.TicketStatusResponse(ticketId, null, FlashSaleTicketStatus.EXPIRED.name(), null, null, null, null);
        }

        Map<Object, Object> map = redisRepository.getTicket(ticketId);

        long productId = Long.parseLong(String.valueOf(map.get("productId")));
        long userId = Long.parseLong(String.valueOf(map.get("userId")));
        String status = String.valueOf(map.get("status"));
        Long enqueueSeq = map.get("enqueueSeq") == null ? null : Long.parseLong(String.valueOf(map.get("enqueueSeq")));
        Long successSeq = map.get("successSeq") == null ? null : Long.parseLong(String.valueOf(map.get("successSeq")));

        // 重要：輪詢視同心跳，刷新 TTL（讓「離線離隊」成立）
        redisRepository.refreshTicketTtl(ticketId, ticketTtl);
        redisRepository.setActiveTicket(productId, userId, ticketId, ticketTtl);

        Long orderId = map.get("orderId") == null ? null : Long.parseLong(String.valueOf(map.get("orderId")));

        Integer position = null;
        if (FlashSaleTicketStatus.QUEUED.name().equals(status)) {
            // 只掃描前 5000 個避免太慢（demo 夠用；10k 也可調大）
            position = redisRepository.findPosition(productId, ticketId, 5000);
        }

        return new FlashSaleDtos.TicketStatusResponse(ticketId, productId, status, position, orderId, enqueueSeq, successSeq);
    }

    /**
     * worker 取出隊頭後呼叫：先延長 ticket 存活，避免處理中途過期
     */
    public void extendTicketForProcessing(String ticketId) {
        // 這裡用 resultTtl 當處理保護時間
        redisRepository.refreshTicketTtl(ticketId, resultTtl);
    }

    /**
     * worker 出隊時呼叫：把 FIFO 出隊順序寫到 successSeq
     * 這樣 evidence 用 successSeq 排序時，順序會貼近 FIFO。
     */
    public void markDequeued(String ticketId, long dequeueSeq) {
        // 不改狀態，避免影響 processTicket 的 QUEUED 判斷
        // 只寫 successSeq 作為「處理順序證據」
        redisRepository.updateTicket(ticketId, Map.of(
                "successSeq", String.valueOf(dequeueSeq)
        ));
    }

    /**
     * 由 worker 呼叫：處理隊頭 ticket
     * - 成功扣庫存 → 建立搶購訂單（單品項、單件）
     * - 庫存不足 → SOLD_OUT
     */
    @Transactional
    public void processTicket(String ticketId) {
        // 票不存在就算了（可能剛好過期/被清）
        if (!redisRepository.ticketExists(ticketId)) return;

        Map<Object, Object> map = redisRepository.getTicket(ticketId);
        if (map == null || map.isEmpty()) return;

        // 這些欄位若缺失，代表票曾過期重建或資料不完整 → 直接標 ERROR 並結束
        Object pidObj = map.get("productId");
        Object uidObj = map.get("userId");
        if (pidObj == null || uidObj == null) {
            redisRepository.updateTicket(ticketId, Map.of("status", FlashSaleTicketStatus.ERROR.name()));
            redisRepository.setResultTtl(ticketId, resultTtl);
            return;
        }

        long productId = Long.parseLong(String.valueOf(pidObj));
        long userId = Long.parseLong(String.valueOf(uidObj));

        String status = String.valueOf(map.get("status"));
        if (!FlashSaleTicketStatus.QUEUED.name().equals(status)) {
            return;
        }

        // 標記處理中（可讓前端看到 PROCESSING）
        redisRepository.updateTicket(ticketId, Map.of("status", FlashSaleTicketStatus.PROCESSING.name()));

        try {
            // 1) 扣庫存（FLASH_SALE 專用 endpoint，一次只能扣 1）
            ProductClient.ReserveResponse reserveResponse = productClient.reserveFlashSale(productId, new ProductClient.ReserveRequest(1));

            if (reserveResponse.success()) {
                // 2) 建立搶購訂單（單品項、單件）
                long orderId = orderService.createFlashSaleOrder(userId, productId);

                // successSeq 已經在出隊時寫入 dequeueSeq 了
                // 這裡不要再 nextSuccessSeq，避免被 DB 延遲打亂 FIFO 證據
                redisRepository.updateTicket(ticketId, Map.of(
                        "status", FlashSaleTicketStatus.SUCCESS.name(),
                        "orderId", String.valueOf(orderId)
                ));

                redisRepository.setResultTtl(ticketId, resultTtl);
            } else {
                redisRepository.updateTicket(ticketId, Map.of("status", FlashSaleTicketStatus.SOLD_OUT.name()
                ));
                redisRepository.setResultTtl(ticketId, resultTtl);
            }
        } catch (Exception e) {
            // 庫存不足或其他錯誤 → SOLD_OUT（demo 先統一）
            redisRepository.updateTicket(ticketId, Map.of("status", FlashSaleTicketStatus.ERROR.name()));
            redisRepository.setResultTtl(ticketId, resultTtl);
        } finally {
            // 無論成功或售完，都讓使用者可以重新 join（不再 active）
            redisRepository.deleteActiveTicket(productId, userId);
        }
    }
}