package com.coze.aicoze.service;

import com.coze.aicoze.common.BizErrorCodes;
import com.coze.aicoze.config.PlatformOutboundRateLimitProperties;
import com.coze.aicoze.exception.BusinessException;
import com.coze.aicoze.observability.RateLimitMetrics;
import com.coze.aicoze.service.ratelimit.SlidingWindowRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Coze：双滑动窗口（1s QPS + 60s RPM）；腾讯 LKE：单滑动窗口（60s QPM）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformOutboundRateLimiter {

    private static final long WINDOW_ONE_SECOND_MS = 1_000L;
    private static final long WINDOW_ONE_MINUTE_MS = 60_000L;

    private final PlatformOutboundRateLimitProperties props;
    private final SlidingWindowRateLimiter sliding;
    private final RateLimitMetrics rateLimitMetrics;

    public void acquireCozeOrThrow() {
        if (!props.isEnabled()) {
            return;
        }
        String p = props.getKeyPrefix();
        int rpm = Math.max(1, props.getCoze().getModelRpm());
        int qps = Math.max(1, props.getCoze().getInitConversationQps());
        String rpmKey = p + "coze:rpm";
        String qpsKey = p + "coze:qps";

        Optional<String> rpmMember = sliding.tryReserve(rpmKey, WINDOW_ONE_MINUTE_MS, rpm);
        if (rpmMember.isEmpty()) {
            rateLimitMetrics.incrementOutboundCozeRpmRejected();
            log.warn("Coze outbound sliding RPM exceeded, limit={}", rpm);
            throw new BusinessException(BizErrorCodes.APP_RATE_LIMIT, "请求过于频繁，请稍后再试");
        }
        Optional<String> qpsMember = sliding.tryReserve(qpsKey, WINDOW_ONE_SECOND_MS, qps);
        if (qpsMember.isEmpty()) {
            sliding.release(rpmKey, rpmMember.get());
            rateLimitMetrics.incrementOutboundCozeQpsRejected();
            log.warn("Coze outbound sliding QPS exceeded, limit={}", qps);
            throw new BusinessException(BizErrorCodes.APP_RATE_LIMIT, "请求过于频繁，请稍后再试");
        }
    }

    public void acquireTencentOrThrow() {
        if (!props.isEnabled()) {
            return;
        }
        int qpm = Math.max(1, props.getTencent().getModelQpm());
        String key = props.getKeyPrefix() + "tencent:qpm";
        if (sliding.tryReserve(key, WINDOW_ONE_MINUTE_MS, qpm).isEmpty()) {
            rateLimitMetrics.incrementOutboundTencentQpmRejected();
            log.warn("Tencent LKE outbound sliding QPM exceeded, limit={}", qpm);
            throw new BusinessException(BizErrorCodes.APP_RATE_LIMIT, "请求过于频繁，请稍后再试");
        }
    }
}
