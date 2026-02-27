package com.example.ecommerce.order.flashsale;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class FlashSaleRedisRepository {

    private final StringRedisTemplate redis;

    // compare-and-del Lua：只有 value 相同才刪除
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('get', KEYS[1]) == ARGV[1] then
              return redis.call('del', KEYS[1])
            else
              return 0
            end
            """,
            Long.class
    );

    /**
     * 原子入隊腳本：同一個 Lua 事務內完成 INCR + RPUSH。
     * 保證 enqueueSeq 的遞增順序嚴格等同於 Redis List 的插入順序，避免競態導致 FIFO 失效。
     * KEYS[1] = enqueue-seq key, KEYS[2] = queue key
     * ARGV[1] = ticketId
     */
    private static final DefaultRedisScript<Long> ENQUEUE_SCRIPT = new DefaultRedisScript<>(
            """
            local seq = redis.call('INCR', KEYS[1])
            redis.call('RPUSH', KEYS[2], ARGV[1])
            return seq
            """,
            Long.class
    );

    public FlashSaleRedisRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String queueKey(long productId) {
        return "flash:queue:" + productId;
    }

    public String ticketKey(String ticketId) {
        return "flash:ticket:" + ticketId;
    }

    public String activeKey(long productId, long userId) {
        return "flash:active:" + productId + ":" + userId;
    }

    public String lockKey(long productId) {
        return "flash:lock:" + productId;
    }

    /**
     * 原子操作：同時分配 enqueueSeq 並將 ticketId 推入隊列尾端。
     * 使用 Lua 腳本保證兩個操作不可分割，解決競態條件下 enqueueSeq 與實際入隊順序不一致的問題。
     */
    public long atomicEnqueueAndGetSeq(long productId, String ticketId) {
        String seqKey = "flash:enqueue-seq:" + productId;
        Long seq = redis.execute(ENQUEUE_SCRIPT, List.of(seqKey, queueKey(productId)), ticketId);
        return seq == null ? 0L : seq;
    }

    public long nextDequeueSeq(long productId) {
        String key = "flash:dequeue-seq:" + productId;
        Long v = redis.opsForValue().increment(key);
        return v == null ? 0L : v;
    }

    // ===== Active（避免同一 user 重複 join）=====
    public Optional<String> getActiveTicket(long productId, long userId) {
        String value = redis.opsForValue().get(activeKey(productId, userId));
        return Optional.ofNullable(value);
    }

    public void setActiveTicket(long productId, long userId, String ticketId, Duration ttl) {
        redis.opsForValue().set(activeKey(productId, userId), ticketId, ttl);
    }

    public void deleteActiveTicket(long productId, long userId) {
        redis.delete(activeKey(productId, userId));
    }

    // ===== Ticket =====
    public void createTicket(String ticketId, Map<String, String> fields, Duration ttl) {
        redis.opsForHash().putAll(ticketKey(ticketId), fields);
        redis.expire(ticketKey(ticketId), ttl);
    }

    public boolean ticketExists(String ticketId) {
        Boolean exists = redis.hasKey(ticketKey(ticketId));
        return Boolean.TRUE.equals(exists);
    }

    public Map<Object, Object> getTicket(String ticketId) {
        return redis.opsForHash().entries(ticketKey(ticketId));
    }

    public void updateTicket(String ticketId, Map<String, String> fields) {
        redis.opsForHash().putAll(ticketKey(ticketId), fields);
    }

    public void refreshTicketTtl(String ticketId, Duration ttl) {
        redis.expire(ticketKey(ticketId), ttl);
    }

    public void setResultTtl(String ticketId, Duration ttl) {
        redis.expire(ticketKey(ticketId), ttl);
    }

    // ===== Queue =====

    public String popQueueHead(long productId) {
        return redis.opsForList().leftPop(queueKey(productId));
    }

    public Integer findPosition(long productId, String ticketId, int scanLimit) {
        // 為避免超大 list 全掃，限制 scanLimit
        Long size = redis.opsForList().size(queueKey(productId));
        if (size == null || size == 0) return null;

        long limit = Math.min(size, scanLimit);
        for (int i = 0; i < limit; i++) {
            String value = redis.opsForList().index(queueKey(productId), i);
            if (ticketId.equals(value)) return i + 1; // 人類從 1 開始
        }
        return null;
    }

    // ===== Lock =====
    public boolean tryLock(long productId, String lockValue, Duration ttl) {
        Boolean ok = redis.opsForValue().setIfAbsent(lockKey(productId), lockValue, ttl);
        return Boolean.TRUE.equals(ok);
    }

    public void unlock(long productId, String lockValue) {
        redis.execute(UNLOCK_SCRIPT, List.of(lockKey(productId)), lockValue);
    }
}