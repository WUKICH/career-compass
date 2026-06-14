package com.coze.aicoze.config;

import com.coze.aicoze.common.BizErrorCodes;
import com.coze.aicoze.exception.BusinessException;
import com.coze.aicoze.observability.RateLimitMetrics;
import com.coze.aicoze.service.ratelimit.SlidingWindowRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 应用侧滑动窗口限流（Redis ZSET，默认任意连续 60s 内不超过配置次数），依赖 Redis。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final long SLIDING_WINDOW_MS = 60_000L;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final SlidingWindowRateLimiter slidingWindowRateLimiter;
    private final AppRateLimitProperties rateLimitProperties;
    private final RateLimitMetrics rateLimitMetrics;

    @Value("${app.redis.enabled:false}")
    private boolean redisFeatureEnabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!rateLimitProperties.isEnabled() || !redisFeatureEnabled) {
            return true;
        }
        String path = request.getServletPath();
        if (!matches(path)) {
            return true;
        }

        String scope = scopeKey(request);
        String redisKey = rateLimitProperties.getKeyPrefix() + "sw:" + scope;
        int limit = Math.max(1, rateLimitProperties.getRequestsPerMinute());
        if (!slidingWindowRateLimiter.tryAcquire(redisKey, SLIDING_WINDOW_MS, limit)) {
            rateLimitMetrics.incrementEntryRejected();
            log.warn("应用滑动窗口限流 path={}, scope={}, limit={}", path, scope, limit);
            throw new BusinessException(BizErrorCodes.APP_RATE_LIMIT, "请求过于频繁，请稍后再试");
        }
        return true;
    }

    private boolean matches(String path) {
        List<String> pathPatterns = rateLimitProperties.getPathPatterns();
        if (pathPatterns == null || pathPatterns.isEmpty()) {
            return false;
        }
        for (String pattern : pathPatterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            String p = pattern.trim();
            if (pathMatcher.match(p, path)) {
                return true;
            }
        }
        return false;
    }

    private static String scopeKey(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId != null) {
            return "u:" + userId;
        }
        String ip = request.getRemoteAddr();
        return "ip:" + (ip == null || ip.isBlank() ? "unknown" : ip);
    }
}
