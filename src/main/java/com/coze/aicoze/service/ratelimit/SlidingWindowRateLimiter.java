package com.coze.aicoze.service.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis ZSET + Lua 滑动窗口限流：任意连续 windowMs 内最多 maxRequests 次。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SlidingWindowRateLimiter {

    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>();
    private static final String LUA = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            local member = ARGV[4]
            local cutoff = now - window
            if cutoff < 0 then cutoff = 0 end
            redis.call('ZREMRANGEBYSCORE', key, '-inf', cutoff)
            local c = redis.call('ZCARD', key)
            if c >= limit then
              return 0
            end
            redis.call('ZADD', key, now, member)
            local ttlSec = math.ceil(window / 1000) + 2
            if ttlSec < 2 then ttlSec = 2 end
            redis.call('EXPIRE', key, ttlSec)
            return 1
            """;

    static {
        RESERVE_SCRIPT.setScriptText(LUA);
        RESERVE_SCRIPT.setResultType(Long.class);
    }

    private final ObjectProvider<StringRedisTemplate> redisProvider;

    @Value("${app.redis.enabled:false}")
    private boolean redisFeatureEnabled;

    /**
     * 尝试占位；成功返回 memberId，失败返回 empty（未写入 Redis）。
     */
    public Optional<String> tryReserve(String redisKey, long windowMs, int maxRequests) {
        if (!redisFeatureEnabled) {
            return Optional.of("_noop");
        }
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return Optional.of("_noop");
        }
        if (maxRequests <= 0) {
            maxRequests = 1;
        }
        String member = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        Long ok = redis.execute(RESERVE_SCRIPT, Collections.singletonList(redisKey),
                String.valueOf(now), String.valueOf(windowMs), String.valueOf(maxRequests), member);
        if (ok != null && ok == 1L) {
            return Optional.of(member);
        }
        return Optional.empty();
    }

    /**
     * 释放占位（用于多桶顺序占位时后者失败需回滚）。
     */
    public void release(String redisKey, String memberId) {
        if (memberId == null || "_noop".equals(memberId)) {
            return;
        }
        if (!redisFeatureEnabled) {
            return;
        }
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.opsForZSet().remove(redisKey, memberId);
        } catch (Exception e) {
            log.debug("sliding window release skip key={}", redisKey, e);
        }
    }

    /**
     * 单桶：成功 true，超限 false。
     */
    public boolean tryAcquire(String redisKey, long windowMs, int maxRequests) {
        return tryReserve(redisKey, windowMs, maxRequests).isPresent();
    }
}
