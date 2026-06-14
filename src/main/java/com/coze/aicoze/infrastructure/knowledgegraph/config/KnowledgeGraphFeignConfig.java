package com.coze.aicoze.infrastructure.knowledgegraph.config;

import com.coze.aicoze.infrastructure.knowledgegraph.exception.KnowledgeGraphException;
import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class KnowledgeGraphFeignConfig {

    /**
     * 开启 Feign 日志（建议在开发环境使用 FULL，生产环境可用 BASIC 或 HEADERS）
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * 知识图谱服务可能涉及 Neo4j 写入与缓存清理，端到端耗时较长。
     * - connectTimeout: 2s 够用（网络不通尽快失败）
     * - readTimeout: 60s 给写入/查询留余量，避免误报 Connect/Read timed out
     */
    @Bean
    public Request.Options feignRequestOptions() {
        return new Request.Options(
                2, TimeUnit.SECONDS,
                60, TimeUnit.SECONDS,
                true);
    }

    /**
     * 轻量重试：应对偶发网络抖动/短暂不可用。
     * - 最大尝试次数：3（初次 + 2 次重试）
     * - 退避：200ms -> 400ms（上限 2s）
     */
    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(200, 2_000, 3);
    }

    /**
     * 自定义错误解码器，处理 HTTP 500 等错误响应
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            // 尝试从响应体中读取错误信息（ErrorResponse 格式）
            String errorMsg = String.format("知识图谱服务调用失败: %s %s", response.status(), response.reason());
            try {
                if (response.body() != null) {
                    // 使用 Feign 的 Jackson 解码器将响应体转换为 ErrorResponse
                    feign.Response.Body body = response.body();
                    // 这里简化处理，实际可能需要借助 Decoder 或手动解析
                    // 更严谨的方式：注入 Decoder 或使用 Jackson 的 ObjectMapper
                    errorMsg = "知识图谱服务返回错误: " + body.toString(); // 实际需解析 JSON
                }
            } catch (Exception e) {
                // ignore
            }
            throw new KnowledgeGraphException(errorMsg);
        };
    }

    // 如果需要添加请求头（如 API Key），可以配置 RequestInterceptor
    // @Bean
    // public RequestInterceptor apiKeyInterceptor(@Value("${kg.service.api-key}") String apiKey) {
    //     return template -> template.header("X-API-KEY", apiKey);
    // }
}