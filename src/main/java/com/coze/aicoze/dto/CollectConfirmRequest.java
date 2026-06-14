package com.coze.aicoze.dto;

import lombok.Data;

import java.util.Map;

/**
 * 信息确认页提交：与分步 {@code /chat/collect} 分离，在用户对画像核对或修改后调用。
 */
@Data
public class CollectConfirmRequest {
    /** 收集阶段返回的 sessionId（必填） */
    private String sessionId;
    /**
     * 确认后的完整字段（key 与收集步骤一致，可与 CollectStore 中已有值合并覆盖）。
     */
    private Map<String, String> answers;
}
