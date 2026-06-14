package com.coze.aicoze.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * 限流 / 幂等冲突观测指标（Micrometer，可由 Prometheus 抓取）。
 * <p>
 * 指标名：{@code ai.ratelimit.rejected}，标签 {@code layer}：
 * {@code entry}、{@code coze_rpm}、{@code coze_qps}、{@code tencent_qpm}、{@code idempotent_duplicate}。
 */
@Component
public class RateLimitMetrics {

    private static final String METER = "ai.ratelimit.rejected";

    private final MeterRegistry registry;

    public RateLimitMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** MVC 入口滑动窗口被拒（42901） */
    public void incrementEntryRejected() {
        inc("entry");
    }

    /** Coze 出站 RPM 桶被拒 */
    public void incrementOutboundCozeRpmRejected() {
        inc("coze_rpm");
    }

    /** Coze 出站 QPS 桶被拒 */
    public void incrementOutboundCozeQpsRejected() {
        inc("coze_qps");
    }

    /** 腾讯 LKE 出站 QPM 桶被拒 */
    public void incrementOutboundTencentQpmRejected() {
        inc("tencent_qpm");
    }

    /** Idempotency-Key 重复（40002） */
    public void incrementIdempotentDuplicate() {
        inc("idempotent_duplicate");
    }

    private void inc(String layer) {
        registry.counter(METER, "layer", layer).increment();
    }
}
