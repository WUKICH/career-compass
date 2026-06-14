package com.coze.aicoze.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 基于 Redis 与请求头 {@code Idempotency-Key} 的提交幂等保护。
 * 需 {@code app.redis.enabled=true} 且 Redis 可用；关闭时不生效。
 * <p>
 * 失败时删除 Redis 键，允许使用同一 Key 重试；成功则键保留至 TTL。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** 是否必须带 Idempotency-Key；{@code false} 时无头则跳过幂等逻辑 */
    boolean required() default true;

    /**
     * 保护时间（秒），{@code <=0} 时使用 {@code app.idempotency.ttl-seconds}
     */
    int ttlSeconds() default -1;
}
