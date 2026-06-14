package com.coze.aicoze.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 出站全局限流，对齐 Coze 限流表（发起对话 QPS + 模型 RPM）与腾讯文档模型 QPM。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.platform-rate-limit")
public class PlatformOutboundRateLimitProperties {

    private boolean enabled = false;
    private String keyPrefix = "rl:out:v1:";

    private Coze coze = new Coze();
    private Tencent tencent = new Tencent();

    @Data
    public static class Coze {
        /** 发起对话类 API：任意 1s 滑动窗口内最大请求数 */
        private int initConversationQps = 20;
        /** 模型：任意 60s 滑动窗口内最大请求数（RPM） */
        private int modelRpm = 300;
    }

    @Data
    public static class Tencent {
        /** 主要模型：任意 60s 滑动窗口内最大请求数（QPM） */
        private int modelQpm = 300;
    }
}
