package com.example.ecommerce.order.flashsale;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Repository
public class FlashSaleRedisRepository {

    private final StringRedisTemplate redis;

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
    public void enqueue(long productId, String ticketId) {
        redis.opsForList().rightPush(queueKey(productId), ticketId);
    }

    public String peekQueueHead(long productId) {
        return redis.opsForList().index(queueKey(productId), 0);
    }

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
        // 簡化版：demo 不做 Lua compare-and-del，worker TTL 很短可接受
        redis.delete(lockKey(productId));
    }
}