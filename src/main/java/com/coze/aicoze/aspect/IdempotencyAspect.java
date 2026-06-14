package com.coze.aicoze.aspect;

import cn.hutool.crypto.digest.DigestUtil;
import com.coze.aicoze.annotation.Idempotent;
import com.coze.aicoze.common.BizErrorCodes;
import com.coze.aicoze.exception.BusinessException;
import com.coze.aicoze.observability.RateLimitMetrics;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Aspect
@Component
@Slf4j
public class IdempotencyAspect {

    private static final int MAX_KEY_HEADER_LEN = 128;

    @Value("${app.redis.enabled:false}")
    private boolean redisFeatureEnabled;

    @Value("${app.idempotency.key-prefix:idem:v1:}")
    private String keyPrefix;

    @Value("${app.idempotency.ttl-seconds:86400}")
    private int defaultTtlSeconds;

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final RateLimitMetrics rateLimitMetrics;

    public IdempotencyAspect(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                             RateLimitMetrics rateLimitMetrics) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.rateLimitMetrics = rateLimitMetrics;
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        if (!redisFeatureEnabled) {
            return pjp.proceed();
        }
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            log.warn("app.redis.enabled=true 但未找到 StringRedisTemplate，跳过幂等");
            return pjp.proceed();
        }

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return pjp.proceed();
        }
        HttpServletRequest request = attrs.getRequest();
        String header = request.getHeader("Idempotency-Key");
        if (!idempotent.required() && (header == null || header.isBlank())) {
            return pjp.proceed();
        }
        if (idempotent.required() && (header == null || header.isBlank())) {
            throw new BusinessException(BizErrorCodes.INVALID_SESSION_OR_PARAMS, "请提供 Idempotency-Key 请求头");
        }
        String rawKey = header.strip();
        if (rawKey.length() > MAX_KEY_HEADER_LEN) {
            throw new BusinessException(BizErrorCodes.INVALID_SESSION_OR_PARAMS, "Idempotency-Key 过长");
        }

        int ttl = idempotent.ttlSeconds() > 0 ? idempotent.ttlSeconds() : defaultTtlSeconds;
        String redisKey = keyPrefix + DigestUtil.sha256Hex(scopePart(request) + "|" + request.getServletPath() + "|" + rawKey);

        Boolean acquired = redis.opsForValue().setIfAbsent(redisKey, "1", Duration.ofSeconds(ttl));
        if (Boolean.FALSE.equals(acquired)) {
            rateLimitMetrics.incrementIdempotentDuplicate();
            throw new BusinessException(BizErrorCodes.DUPLICATE_IDEMPOTENT_REQUEST, "请勿重复提交，或稍后再试");
        }
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            redis.delete(redisKey);
            throw t;
        }
    }

    private static String scopePart(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId != null) {
            return "u:" + userId;
        }
        String ip = request.getRemoteAddr();
        if (ip == null || ip.isBlank()) {
            return "ip:unknown";
        }
        return "ip:" + ip;
    }
}
