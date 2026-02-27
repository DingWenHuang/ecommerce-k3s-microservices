package com.example.ecommerce.order.flashsale;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * 證據輸出工具（不影響業務路徑）
 * 從 Redis 掃描所有 flash:ticket:*，篩出 SUCCESS，並依 successSeq 排序輸出。
 */
@Service
public class FlashSaleEvidenceService {

    private final StringRedisTemplate redis;

    public FlashSaleEvidenceService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * @param sinceSeconds 只統計最近 sinceSeconds 秒內 createdAt 的票；<=0 代表不限制
     */
    public WinnersResult getWinners(long productId, int limit, long sinceSeconds) {
        Instant cutoff = (sinceSeconds > 0) ? Instant.now().minusSeconds(sinceSeconds) : null;

        List<TicketRecord> successTickets = scanSuccessTickets(productId, cutoff);

        // 依 successSeq 小到大（代表成功順序）
        successTickets.sort(Comparator.comparingLong(t -> t.successSeq == null ? Long.MAX_VALUE : t.successSeq));

        int total = successTickets.size();
        List<TicketRecord> firstN = successTickets.stream().limit(limit).toList();

        // FIFO 證據：把前 N 筆成功者的 enqueueSeq 拉出來看是否遞增
        List<Long> enqueueSeqList = firstN.stream()
                .map(t -> t.enqueueSeq)
                .filter(Objects::nonNull)
                .toList();

        boolean nonDecreasing = isNonDecreasing(enqueueSeqList);

        return new WinnersResult(productId, total, limit, sinceSeconds, nonDecreasing, firstN);
    }

    private List<TicketRecord> scanSuccessTickets(long productId, Instant cutoff) {
        String pattern = "flash:ticket:*";

        List<TicketRecord> results = new ArrayList<>();

        // SCAN 不會阻塞 Redis（相對 KEYS 安全），適合 demo
        try (Cursor<byte[]> cursor = Objects.requireNonNull(redis.getConnectionFactory())
                .getConnection()
                .scan(ScanOptions.scanOptions().match(pattern).count(1000).build())) {

            while (cursor.hasNext()) {
                String key = new String(cursor.next());

                Map<Object, Object> hash = redis.opsForHash().entries(key);
                if (hash == null || hash.isEmpty()) continue;

                String pid = stringValue(hash.get("productId"));
                if (pid == null || !pid.equals(String.valueOf(productId))) continue;

                // createdAt 過濾（避免混到前幾輪測試）
                if (cutoff != null) {
                    Instant createdAt = parseInstant(hash.get("createdAt"));
                    // 沒有 createdAt 或解析失敗：保守起見不納入（避免混雜）
                    if (createdAt == null || createdAt.isBefore(cutoff)) continue;
                }


                String status = stringValue(hash.get("status"));
                if (!FlashSaleTicketStatus.SUCCESS.name().equals(status)) continue;

                String ticketId = stringValue(hash.get("ticketId"));
                Long enqueueSeq = longValue(hash.get("enqueueSeq"));
                Long successSeq = longValue(hash.get("successSeq"));
                Long userId = longValue(hash.get("userId"));
                Long orderId = longValue(hash.get("orderId"));

                results.add(new TicketRecord(ticketId, userId, productId, enqueueSeq, successSeq, orderId));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to scan Redis for winners evidence", e);
        }

        return results;
    }

    private Instant parseInstant(Object v) {
        if (v == null) return null;
        try {
            return Instant.parse(String.valueOf(v));
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isNonDecreasing(List<Long> values) {
        if (values.isEmpty()) return true;
        long prev = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            long cur = values.get(i);
            if (cur < prev) return false;
            prev = cur;
        }
        return true;
    }

    private String stringValue(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private Long longValue(Object v) {
        if (v == null) return null;
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public record WinnersResult(
            long productId,
            int successCount,
            int limit,
            long sinceSeconds,
            boolean enqueueSeqNonDecreasingInFirstN,
            List<TicketRecord> winners
    ) {}

    public record TicketRecord(
            String ticketId,
            Long userId,
            Long productId,
            Long enqueueSeq,
            Long successSeq,
            Long orderId
    ) {}
}