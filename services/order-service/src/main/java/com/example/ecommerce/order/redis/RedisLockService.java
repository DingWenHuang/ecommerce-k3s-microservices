package com.example.ecommerce.order.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * Redis 分布式鎖（示範用但具備基本安全性）
 * - lock: SET key value NX PX ttl
 * - unlock: Lua script（只有 value 相同才刪除）
 */
@Service
public class RedisLockService {

    private final StringRedisTemplate redisTemplate;

    private final DefaultRedisScript<Long> unlockScript = new DefaultRedisScript<>(
            """
            if redis.call("GET", KEYS[1]) == ARGV[1] then
              return redis.call("DEL", KEYS[1])
            else
              return 0
            end
            """,
            Long.class
    );

    public RedisLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 嘗試取得鎖
     * @return lockValue（成功），null（失敗）
     */
    public String tryLock(String key, Duration ttl) {
        String value = UUID.randomUUID().toString();
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(ok) ? value : null;
    }

    /**
     * 安全解鎖：只有 value 相同才刪
     */
    public boolean unlock(String key, String value) {
        Long result = redisTemplate.execute(unlockScript, Collections.singletonList(key), value);
        return result != null && result > 0;
    }
}
