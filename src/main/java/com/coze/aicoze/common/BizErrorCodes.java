package com.coze.aicoze.common;

/**
 * 业务错误码（与 docs/agent-platform-hardening-checklist.md 方案 A 一致）。
 * 平台原始码在适配器内映射到此表；日志可单独记录 platformCode。
 */
public final class BizErrorCodes {

    private BizErrorCodes() {
    }

    /** 参数 / 会话非法 */
    public static final int INVALID_SESSION_OR_PARAMS = 40001;

    /** 重复提交（同 Idempotency-Key 在保护期内） */
    public static final int DUPLICATE_IDEMPOTENT_REQUEST = 40002;

    /** 功能暂未开放（如完全版测评） */
    public static final int FEATURE_TEMPORARILY_UNAVAILABLE = 40003;

    /** 当前流程阶段不允许该操作（HTTP 语义 409） */
    public static final int INVALID_WORKFLOW_PHASE = 40004;

    /** 未登录 / 鉴权失败 */
    public static final int UNAUTHORIZED = 40101;

    /** 应用侧限流 */
    public static final int APP_RATE_LIMIT = 42901;

    /** 上游繁忙 / 平台限流（适配器填充） */
    public static final int UPSTREAM_BUSY_OR_RATE_LIMIT = 50301;

    /** 上游 / 智能体响应超时 */
    public static final int UPSTREAM_TIMEOUT = 50401;

    /** 配额 / 额度用尽（适配器填充） */
    public static final int QUOTA_EXCEEDED = 50901;

    /** 可预期业务失败（解析失败、报告生成异常等） */
    public static final int BUSINESS_EXPECTED_FAILURE = 50001;

    /** 未知系统异常（兜底，替代滥用 code=0） */
    public static final int UNKNOWN_SYSTEM = 59901;
}
